package ru.mtuci.trafficcap002.networking.protocols.transport.tcp;

import static java.nio.channels.SelectionKey.OP_CONNECT;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;
import static ru.mtuci.trafficcap002.networking.protocols.transport.DatagramConsumer.PROTOCOL_ICMPv4;
import static ru.mtuci.trafficcap002.networking.protocols.transport.DatagramConsumer.PROTOCOL_ICMPv6;
import static ru.mtuci.trafficcap002.networking.protocols.transport.DatagramConsumer.PROTOCOL_TCP;
import static ru.mtuci.trafficcap002.networking.protocols.transport.icmp.ICMPBuilder.CODE_HOST_UNREACHABLE;
import static ru.mtuci.trafficcap002.networking.protocols.transport.icmp.ICMPBuilder.CODE_PORT_UNREACHABLE;
import static ru.mtuci.trafficcap002.networking.protocols.transport.icmp.ICMPBuilder.TYPE_DESTINATION_UNREACHABLE;
import static ru.mtuci.trafficcap002.networking.protocols.transport.tcp.TCPPacket.POS_ACK;
import static ru.mtuci.trafficcap002.networking.protocols.transport.tcp.TCPPacket.POS_FIN;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Inet6Address;
import java.net.NoRouteToHostException;
import java.net.PortUnreachableException;
import java.net.Socket;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import ru.mtuci.trafficcap002.networking.HttpWriter;
import ru.mtuci.trafficcap002.networking.PcapWriter;
import ru.mtuci.trafficcap002.networking.protocols.ip.IPPacketBuilder;
import ru.mtuci.trafficcap002.networking.protocols.ip.ipv4.IPv4PacketBuilder;
import ru.mtuci.trafficcap002.networking.protocols.ip.ipv6.IPv6Packet;
import ru.mtuci.trafficcap002.networking.protocols.ip.ipv6.IPv6PacketBuilder;
import ru.mtuci.trafficcap002.networking.protocols.transport.DatagramBuilder;
import ru.mtuci.trafficcap002.networking.protocols.transport.Endpoints;
import ru.mtuci.trafficcap002.networking.protocols.transport.Periodic;
import ru.mtuci.trafficcap002.networking.protocols.transport.icmp.ICMPBuilder;

public final class TCPConnection implements ConnectionState {
    private static final TCPOption zero_option_true = new TCPOption(0, true);
    private static final TCPOption zero_option_false = new TCPOption(0, false);

    private final Endpoints endpoints;
    private final TCPPacket syn_packet;
    private final TCPOption mss, tx_scale, rx_scale;
    private final SelectionKey key;
    private final FileOutputStream out;
    private final PcapWriter pcap_writer;
    private final Map<Endpoints, TCPConnection> connections;
    private final List<ByteBuffer> site_queue; // Очередь отправки на удалённый сайт
    private final List<TCPSegmentData> app_queue; // Очередь отправки на приложение
    private final int[] our_seq; // SEQ следующего отправленного нами пакета
    private int wanted_seq; // SEQ пакета, получение которого ожидается. В наших пакетах будет как ACK.
    private int last_acknowledged; // ACK последнего полученного пакета. Используется для оценки возможности отправки сегментов.
    private int last_window; // Масштабированное последнее значение окна. Вместе с предыдущим параметром используется для оценки возможности отправки сегментов.
    private ConnectionState state;

    public TCPConnection(Map<Endpoints, TCPConnection> connections, TCPPacket packet, Endpoints endpoints, Selector selector, FileOutputStream out, PcapWriter pcap_writer) throws IOException {
        this.endpoints = endpoints;
        this.syn_packet = packet;
        final SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(false);
        channel.setOption(StandardSocketOptions.SO_REUSEADDR, Boolean.TRUE);
        channel.setOption(StandardSocketOptions.SO_KEEPALIVE, Boolean.TRUE);
        channel.socket().setOOBInline(true);
        channel.connect(endpoints.getSite());
        this.key = channel.register(selector, OP_CONNECT, this);
        this.out = out;
        this.connections = connections;
        this.pcap_writer = pcap_writer;
        this.mss = packet.getMSS();
        this.tx_scale = packet.getScale().getPresence() ? zero_option_true : zero_option_false;
        this.rx_scale = packet.getScale();
        this.site_queue = new LinkedList<>();
        this.app_queue = new LinkedList<>();
        if (packet.getPayload().hasRemaining())
            site_queue.add(packet.getPayload());
        this.wanted_seq = packet.getSeq() + 1;
        this.our_seq = new int[]{ThreadLocalRandom.current().nextInt() ^ endpoints.hashCode() ^ ((int) System.nanoTime())};
        this.last_window = packet.getWindow(rx_scale.getValue());
        state = new StateSynRecieved();
    }

    private void suicide() throws IOException {
        final TCPConnection connection;
        connection = connections.remove(endpoints);
        if (connection != null)
            connection.closeByApplication();
    }

    @Override
    public void consumePacket(TCPPacket packet, HttpWriter http_writer) throws IOException {
        state.consumePacket(packet, http_writer);
    }

    @Override
    public void doPeriodic() throws IOException {
        state.doPeriodic();
    }

    @Override
    public void processSelectionKey(HttpWriter http_writer) throws IOException {
        state.processSelectionKey(http_writer);
    }

    private int getOurRecieveWindow() {
        int window = 131072;
        for (ByteBuffer bb : site_queue)
            window -= bb.remaining();
        return Integer.max(Integer.min(65535, window), 0);
    }

    private int getApplicationSendQueueSize() {
        int size = 0;
        for (TCPSegmentData seg : app_queue) {
            size += seg.getSegmentLength();
        }
        return size;
    }

    private void removeConfirmedSegments(int new_ack, int new_window) {
        final Iterator<TCPSegmentData> seg_iterator;
        if (new_ack == last_acknowledged)
            last_window = new_window;
        seg_iterator = app_queue.iterator();
        int i = 0;
        while (seg_iterator.hasNext()) {
            final TCPSegmentData seg;
            seg = seg_iterator.next();
            if ((seg.getSequenceNumber() + seg.getSegmentLength()) == new_ack) {
                last_acknowledged = new_ack;
                last_window = new_window;
                app_queue.subList(0, i + 1).clear();
                return;
            }
            i++;
        }
    }

    public void closeByApplication() throws IOException {
        key.channel().close();
    }

    private void sendFin() throws IOException {
        final TCPPacketBuilder tcp_builder;
        tcp_builder = new TCPPacketBuilder(endpoints.getSite().getPort(),
                endpoints.getApplication().getPort(),
                ByteBuffer.allocate(0),
                our_seq[0], wanted_seq,
                new boolean[]{false, true, false, false, false, true},
                0, 0,
                zero_option_false, zero_option_false);
        final IPPacketBuilder ip_builder;
        ip_builder = (endpoints.getSite().getAddress() instanceof Inet6Address)
                ? new IPv6PacketBuilder(endpoints.getSite().getAddress(), endpoints.getApplication().getAddress(), tcp_builder, 100, PROTOCOL_TCP)
                : new IPv4PacketBuilder(endpoints.getSite().getAddress(), endpoints.getApplication().getAddress(), tcp_builder, 100, PROTOCOL_TCP);
        final byte[][] packets;
        packets = ip_builder.createPackets();
        for (final byte[] packet : packets) {
            out.write(packet);
            pcap_writer.writePacket(packet, packet.length);
        }
    }

    private void mockDataReception(TCPPacket tcp_packet) throws IOException {
        if (tcp_packet.getSeq() == wanted_seq) {
            final ByteBuffer urgent_data;
            urgent_data = tcp_packet.getUrgentPayload();
            final ByteBuffer payload = tcp_packet.getPayload();
            wanted_seq += urgent_data.limit() + payload.limit();
        }
    }

    private void acknowledge() throws IOException {
        final TCPPacketBuilder tcp_builder;
        tcp_builder = new TCPPacketBuilder(endpoints.getSite().getPort(),
                endpoints.getApplication().getPort(),
                ByteBuffer.allocate(0),
                our_seq[0], wanted_seq,
                new boolean[]{false, true, false, false, false, false},
                getOurRecieveWindow(), 0,
                zero_option_false, zero_option_false);
        final IPPacketBuilder ip_builder;
        ip_builder = (endpoints.getSite().getAddress() instanceof Inet6Address)
                ? new IPv6PacketBuilder(endpoints.getSite().getAddress(), endpoints.getApplication().getAddress(), tcp_builder, 100, PROTOCOL_TCP)
                : new IPv4PacketBuilder(endpoints.getSite().getAddress(), endpoints.getApplication().getAddress(), tcp_builder, 100, PROTOCOL_TCP);
        final byte[][] packets;
        packets = ip_builder.createPackets();
        for (final byte[] packet : packets) {
            out.write(packet);
            pcap_writer.writePacket(packet, packet.length);
        }
    }

    private void resetConnection() throws IOException {
        final TCPPacketBuilder tcp_builder;
        tcp_builder = new TCPPacketBuilder(endpoints.getSite().getPort(),
                endpoints.getApplication().getPort(),
                ByteBuffer.allocate(0),
                our_seq[0], wanted_seq,
                new boolean[]{false, true, false, true, false, false},
                0, 0,
                zero_option_false, zero_option_false);
        final IPPacketBuilder ip_builder;
        ip_builder = (endpoints.getSite().getAddress() instanceof Inet6Address)
                ? new IPv6PacketBuilder(endpoints.getSite().getAddress(), endpoints.getApplication().getAddress(), tcp_builder, 100, PROTOCOL_TCP)
                : new IPv4PacketBuilder(endpoints.getSite().getAddress(), endpoints.getApplication().getAddress(), tcp_builder, 100, PROTOCOL_TCP);
        final byte[][] packets;
        packets = ip_builder.createPackets();
        for (final byte[] packet : packets) {
            out.write(packet);
            pcap_writer.writePacket(packet, packet.length);
        }
    }

    private void setInterestOptions() {
        if (key.isValid())
            key.interestOps((((last_window - getApplicationSendQueueSize()) > 0) ? OP_READ : 0) | ((!site_queue.isEmpty()) ? OP_WRITE : 0));
    }

    private void sendRemainingToApp() throws IOException {
        final Iterator<TCPSegmentData> seg_iterator;
        seg_iterator = app_queue.iterator();
        while (seg_iterator.hasNext()) {
            final TCPSegmentData seg;
            seg = seg_iterator.next();
            if (((seg.getSequenceNumber() + seg.getSegmentLength() - last_acknowledged) <= last_window) && seg.checkTimeoutExpiredThenUpdate()) {
                final TCPPacketBuilder tcp_builder;
                tcp_builder = new TCPPacketBuilder(endpoints.getSite().getPort(),
                        endpoints.getApplication().getPort(),
                        seg.getSegmentData(),
                        seg.getSequenceNumber(), wanted_seq,
                        new boolean[]{false, true, seg.getFlagPush(), false, false, false},
                        getOurRecieveWindow(), 0,
                        zero_option_false, zero_option_false);
                final IPPacketBuilder ip_builder;
                ip_builder = (endpoints.getSite().getAddress() instanceof Inet6Address)
                        ? new IPv6PacketBuilder(endpoints.getSite().getAddress(), endpoints.getApplication().getAddress(), tcp_builder, 100, PROTOCOL_TCP)
                        : new IPv4PacketBuilder(endpoints.getSite().getAddress(), endpoints.getApplication().getAddress(), tcp_builder, 100, PROTOCOL_TCP);
                final byte[][] packets;
                packets = ip_builder.createPackets();
                for (final byte[] packet : packets) {
                    out.write(packet);
                    pcap_writer.writePacket(packet, packet.length);
                }
            }
        }
    }

    private final class StateSynRecieved extends Periodic implements ConnectionState {
        // Принят пакет SYN. Нужно соединиться с удалённым сайтом и уведомить об этом приложение.

        @Override
        public void consumePacket(TCPPacket tcp_packet, HttpWriter http_writer) throws IOException {
        }

        @Override
        public void processSelectionKey(HttpWriter http_writer) throws IOException {
            if (key.isValid()) {
                if (key.isConnectable()) {
                    final SocketChannel channel = (SocketChannel) key.channel();
                    try {
                        channel.finishConnect();
                        state = new StateSuccessfullyConnected();
                        state.doPeriodic();
                    } catch (
                            ConnectException |
                            NoRouteToHostException e) {
                        final DatagramBuilder icmp_builder;
                        icmp_builder = new ICMPBuilder(syn_packet.getParent(), TYPE_DESTINATION_UNREACHABLE, CODE_HOST_UNREACHABLE);
                        final IPPacketBuilder ip_builder;
                        ip_builder = (syn_packet.getParent() instanceof IPv6Packet)
                                ? new IPv6PacketBuilder(endpoints.getSite().getAddress(), endpoints.getApplication().getAddress(), icmp_builder, 100, PROTOCOL_ICMPv6)
                                : new IPv4PacketBuilder(endpoints.getSite().getAddress(), endpoints.getApplication().getAddress(), icmp_builder, 100, PROTOCOL_ICMPv4);
                        final byte[][] packets;
                        packets = ip_builder.createPackets();
                        for (final byte[] packet : packets) {
                            out.write(packet);
                            pcap_writer.writePacket(packet, packet.length);
                        }
                        suicide();
                    } catch (
                            PortUnreachableException e) {
                        final DatagramBuilder icmp_builder;
                        icmp_builder = new ICMPBuilder(syn_packet.getParent(), TYPE_DESTINATION_UNREACHABLE, CODE_PORT_UNREACHABLE);
                        final IPPacketBuilder ip_builder;
                        ip_builder = (syn_packet.getParent() instanceof IPv6Packet) ?
                                new IPv6PacketBuilder(endpoints.getSite().getAddress(), endpoints.getApplication().getAddress(), icmp_builder, 100, PROTOCOL_ICMPv6) :
                                new IPv4PacketBuilder(endpoints.getSite().getAddress(), endpoints.getApplication().getAddress(), icmp_builder, 100, PROTOCOL_ICMPv4);
                        final byte[][] packets;
                        packets = ip_builder.createPackets();
                        for (final byte[] packet : packets) {
                            out.write(packet);
                            pcap_writer.writePacket(packet, packet.length);
                        }
                        suicide();
                    } catch (
                            IOException ioexcp) {
                        suicide();
                    }
                }
            } else {
                key.channel().close();
                suicide();
            }
        }

        @Override
        protected void periodicAction() throws IOException {

        }
    }

    private final class StateSuccessfullyConnected extends Periodic implements ConnectionState {
        // Соединено с удалённым сайтом. Нужно уведомить об этом приложение и получить ответ.

        @Override
        public void consumePacket(TCPPacket tcp_packet, HttpWriter http_writer) throws IOException {
            if (tcp_packet.getFlags()[POS_ACK] && (tcp_packet.getAck() == (our_seq[0] + 1))) {
                our_seq[0]++;
                final TCPPacketBuilder tcp_builder;
                tcp_builder = new TCPPacketBuilder(endpoints.getSite().getPort(),
                        endpoints.getApplication().getPort(),
                        ByteBuffer.allocate(0),
                        our_seq[0], wanted_seq,
                        new boolean[]{false, true, false, false, false, false},
                        getOurRecieveWindow(),
                        0, zero_option_false, zero_option_false);
                final IPPacketBuilder ip_builder;
                ip_builder = (endpoints.getSite().getAddress() instanceof Inet6Address)
                        ? new IPv6PacketBuilder(endpoints.getSite().getAddress(), endpoints.getApplication().getAddress(), tcp_builder, 100, PROTOCOL_TCP)
                        : new IPv4PacketBuilder(endpoints.getSite().getAddress(), endpoints.getApplication().getAddress(), tcp_builder, 100, PROTOCOL_TCP);
                byte[][] packets = ip_builder.createPackets();
                for (byte[] packet : packets) {
                    out.write(packet);
                    pcap_writer.writePacket(packet, packet.length);
                }
                state = new StateEstablisted();
                last_window = tcp_packet.getWindow(rx_scale.getValue());
                last_acknowledged = tcp_packet.getAck();
                key.interestOps((last_window > 0) ? OP_READ : 0);
            }
        }

        @Override
        public void processSelectionKey(HttpWriter http_writer) throws IOException {

        }

        @Override
        protected void periodicAction() throws IOException {
            final TCPPacketBuilder tcp_builder;
            tcp_builder = new TCPPacketBuilder(endpoints.getSite().getPort(),
                    endpoints.getApplication().getPort(),
                    ByteBuffer.allocate(0),
                    our_seq[0], wanted_seq,
                    new boolean[]{false, true, false, false, true, false}, // SYN + ACK
                    getOurRecieveWindow(),
                    0, mss, tx_scale);
            final IPPacketBuilder ip_builder = (endpoints.getSite().getAddress() instanceof Inet6Address)
                    ? new IPv6PacketBuilder(endpoints.getSite().getAddress(), endpoints.getApplication().getAddress(), tcp_builder, 100, PROTOCOL_TCP)
                    : new IPv4PacketBuilder(endpoints.getSite().getAddress(), endpoints.getApplication().getAddress(), tcp_builder, 100, PROTOCOL_TCP);
            byte[][] packets = ip_builder.createPackets();
            for (byte[] packet : packets) {
                out.write(packet);
                pcap_writer.writePacket(packet, packet.length);
            }
        }
    }

    private final class StateEstablisted extends Periodic implements ConnectionState {
        // Соединение установлено с обеими сторонами. Нужно перебрасывать пакеты.

        @Override
        public void consumePacket(TCPPacket tcp_packet, HttpWriter http_writer) throws IOException {
            if (tcp_packet.getFlags()[POS_ACK]) {
                removeConfirmedSegments(tcp_packet.getAck(), tcp_packet.getWindow(rx_scale.getValue()));
                final int old_ack = wanted_seq;
                if (tcp_packet.getSeq() == wanted_seq) {
                    final ByteBuffer urgent_data;
                    urgent_data = tcp_packet.getUrgentPayload();
                    final Socket sock;
                    sock = ((SocketChannel) key.channel()).socket();
                    while (urgent_data.hasRemaining())
                        sock.sendUrgentData(urgent_data.get());
                    final ByteBuffer payload = tcp_packet.getPayload();
                    if (payload.hasRemaining()) {
                        site_queue.add(payload);
                        if (http_writer != null)
                            http_writer.send(payload.duplicate(),
                                    endpoints.getApplication().getAddress(), endpoints.getSite().getAddress(),
                                    endpoints.getApplication().getPort(), endpoints.getSite().getPort(),
                                    "tcp");
                    }
                    wanted_seq += urgent_data.limit() + payload.limit();
                    if (tcp_packet.getFlags()[POS_FIN]) {
                        wanted_seq++;
                        acknowledge();
                        state = new StateFinRecieved();
                        return;
                    }
                }
                if ((!app_queue.isEmpty()) && (last_window != 0))
                    sendRemainingToApp();
                else if ((last_window == 0) || (old_ack != wanted_seq))
                    acknowledge();
            }
            setInterestOptions();
        }

        @Override
        public void processSelectionKey(HttpWriter http_writer) throws IOException {
            if (key.isValid()) {
                if (key.isReadable()) {
                    final ByteBuffer data;
                    final int readed;
                    data = ByteBuffer.allocate(65536);
                    try {
                        readed = ((SocketChannel) key.channel()).read(data);
                        data.flip();
                        if (readed == -1) {
                            // Сайт больше не будет передавать данные
                            state = new StateSiteEOFRecieved();
                            return;
                        } else {
                            final List<TCPSegmentData> segs;
                            segs = TCPSegmentData.makeSegments(data, our_seq, mss.getValue());
                            app_queue.addAll(segs);
                            sendRemainingToApp();
                            if (http_writer != null)
                                http_writer.send(data.duplicate(),
                                        endpoints.getSite().getAddress(), endpoints.getApplication().getAddress(),
                                        endpoints.getSite().getPort(), endpoints.getApplication().getPort(),
                                        "tcp");
                        }
                    } catch (
                            IOException ioexcp) {
                        resetConnection();
                        suicide();
                        return;
                    }
                }
                if (key.isWritable()) {
                    final Iterator<ByteBuffer> iterator;
                    iterator = site_queue.iterator();
                    try {
                        final int old_window = getOurRecieveWindow();
                        while (iterator.hasNext()) {
                            final ByteBuffer current;
                            current = iterator.next();
                            ((SocketChannel) key.channel()).write(current);
                            if (!current.hasRemaining())
                                iterator.remove();
                        }
                        if (getOurRecieveWindow() != old_window) acknowledge();
                    } catch (
                            IOException ioexcp) {
                        resetConnection();
                        suicide();
                        return;
                    }
                }
                setInterestOptions();
            } else {
                resetConnection();
                suicide();
            }
        }

        @Override
        protected void periodicAction() throws IOException {
            sendRemainingToApp();
        }
    }

    private final class StateFinRecieved extends Periodic implements ConnectionState {
        // Получен FIN от приложения. Подтвердить его, закончить пересылку на сайт, сделать shutdown(), дождаться завершения отправки сайтом и отослать свой FIN.

        private boolean reading_finished = false;

        @Override
        public void consumePacket(TCPPacket tcp_packet, HttpWriter http_writer) throws IOException {
            if (tcp_packet.getFlags()[POS_ACK]) {
                removeConfirmedSegments(tcp_packet.getAck(), tcp_packet.getWindow(rx_scale.getValue()));
                final int old_ack = wanted_seq;
                if (tcp_packet.getSeq() == wanted_seq)
                    mockDataReception(tcp_packet);
                if ((!app_queue.isEmpty()) && (last_window != 0))
                    sendRemainingToApp();
                else if ((last_window == 0) || (old_ack != wanted_seq))
                    acknowledge();
                if (site_queue.isEmpty())
                    ((SocketChannel) key.channel()).shutdownOutput();
                if (reading_finished && app_queue.isEmpty()) {
                    sendFin();
                    state = new StateFinAnswer(false);
                }
            }
        }

        @Override
        public void processSelectionKey(HttpWriter http_writer) throws IOException {
            if (key.isValid()) {
                if (key.isReadable() && (!reading_finished)) {
                    final ByteBuffer data;
                    final int readed;
                    data = ByteBuffer.allocate(65536);
                    try {
                        readed = ((SocketChannel) key.channel()).read(data);
                        data.flip();
                        if (readed == -1) {
                            // Сайт больше не будет передавать данные
                            reading_finished = true;
                            key.channel().close();
                            if (app_queue.isEmpty()) {
                                sendFin();
                                state = new StateFinAnswer(false);
                            }
                            return;
                        } else {
                            final List<TCPSegmentData> segs;
                            segs = TCPSegmentData.makeSegments(data, our_seq, mss.getValue());
                            app_queue.addAll(segs);
                            sendRemainingToApp();
                        }
                    } catch (
                            IOException ioexcp) {
                        resetConnection();
                        suicide();
                        return;
                    }
                }
                if (key.isWritable()) {
                    final Iterator<ByteBuffer> iterator;
                    iterator = site_queue.iterator();
                    try {
                        while (iterator.hasNext()) {
                            final ByteBuffer current;
                            current = iterator.next();
                            ((SocketChannel) key.channel()).write(current);
                            if (!current.hasRemaining())
                                iterator.remove();
                        }
                        if (site_queue.isEmpty()) {
                            ((SocketChannel) key.channel()).shutdownOutput();
                            return;
                        }
                    } catch (
                            IOException ioexcp) {
                        resetConnection();
                        suicide();
                        return;
                    }
                }
                key.interestOps(((((last_window - getApplicationSendQueueSize()) > 0) && (!reading_finished)) ? OP_READ : 0) | ((!site_queue.isEmpty()) ? OP_WRITE : 0));
            } else {
                resetConnection();
                suicide();
            }
        }

        @Override
        protected void periodicAction() throws IOException {
            if (reading_finished && app_queue.isEmpty()) {
                sendFin();
                state = new StateFinAnswer(false);
            } else
                sendRemainingToApp();
        }
    }

    private final class StateSiteEOFRecieved extends Periodic implements ConnectionState {
        // read() вернула -1. Закончить отправку на сайт, разорвать соединение с ним. Передав остаток переданных данных на приложение и получив подтверждение, отправить FIN.

        private boolean fin_got = false;

        @Override
        public void consumePacket(TCPPacket tcp_packet, HttpWriter http_writer) throws IOException {
            if (tcp_packet.getFlags()[POS_ACK]) {
                removeConfirmedSegments(tcp_packet.getAck(), tcp_packet.getWindow(rx_scale.getValue()));
                final int old_ack = wanted_seq;
                if (tcp_packet.getSeq() == wanted_seq) {
                    if (fin_got) {
                        mockDataReception(tcp_packet);
                    } else {
                        final ByteBuffer urgent_data;
                        urgent_data = tcp_packet.getUrgentPayload();
                        final Socket sock;
                        sock = ((SocketChannel) key.channel()).socket();
                        while (urgent_data.hasRemaining())
                            sock.sendUrgentData(urgent_data.get());
                        final ByteBuffer payload = tcp_packet.getPayload();
                        if (payload.hasRemaining())
                            site_queue.add(payload);
                        wanted_seq += urgent_data.limit() + payload.limit();
                    }
                    if (tcp_packet.getFlags()[POS_FIN] && (!fin_got)) {
                        wanted_seq++;
                        acknowledge();
                        fin_got = true;
                    }
                    if (site_queue.isEmpty()) {
                        key.channel().close();
                    }
                    if (app_queue.isEmpty()) {
                        sendFin();
                        state = new StateFinAnswer(!fin_got);
                    }
                }
                if ((!app_queue.isEmpty()) && (last_window != 0))
                    sendRemainingToApp();
                else if ((last_window == 0) || (old_ack != wanted_seq))
                    acknowledge();
            }
            if (key.isValid())
                key.interestOps((!site_queue.isEmpty()) ? OP_WRITE : 0);
        }

        @Override
        public void processSelectionKey(HttpWriter http_writer) throws IOException {
            if (key.isValid()) {
                if (key.isWritable()) {
                    final Iterator<ByteBuffer> iterator;
                    iterator = site_queue.iterator();
                    try {
                        final int old_window = getOurRecieveWindow();
                        while (iterator.hasNext()) {
                            final ByteBuffer current;
                            current = iterator.next();
                            ((SocketChannel) key.channel()).write(current);
                            if (!current.hasRemaining())
                                iterator.remove();
                        }
                        if (getOurRecieveWindow() != old_window) acknowledge();
                        if (site_queue.isEmpty()) {
                            key.channel().close();
                            return;
                        }
                    } catch (
                            IOException ioexcp) {
                        resetConnection();
                        suicide();
                        return;
                    }
                }
                key.interestOps((!site_queue.isEmpty()) ? OP_WRITE : 0);
            } else {
                if (site_queue.isEmpty()) {
                    key.channel().close();
                } else {
                    resetConnection();
                    suicide();
                }
            }
        }

        @Override
        protected void periodicAction() throws IOException {
            if (app_queue.isEmpty())
                sendFin();
            else
                sendRemainingToApp();
        }
    }

    private final class StateFinAnswer extends Periodic implements ConnectionState {
        // Ожидаем ответа на отправленный FIN.

        private final boolean wait_fin;

        public StateFinAnswer(boolean wait) {
            wait_fin = wait;
        }

        @Override
        public void consumePacket(TCPPacket tcp_packet, HttpWriter http_writer) throws IOException {
            if (wait_fin) {
                if (tcp_packet.getFlags()[POS_FIN]) {
                    wanted_seq++;
                    acknowledge();
                    sendFin();
                } else
                    suicide();
            } else {
                if (tcp_packet.getAck() == our_seq[0] + 1)
                    suicide();
            }
        }

        @Override
        public void processSelectionKey(HttpWriter http_writer) throws IOException {

        }

        @Override
        protected void periodicAction() throws IOException {
            sendFin();
        }
    }
}
