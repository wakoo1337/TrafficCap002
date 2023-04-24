package com.wakoo.trafficcap002.networking.protocols.transport.tcp;

import static com.wakoo.trafficcap002.networking.protocols.transport.DatagramConsumer.PROTOCOL_TCP;
import static com.wakoo.trafficcap002.networking.protocols.transport.tcp.TCPPacket.POS_ACK;
import static com.wakoo.trafficcap002.networking.protocols.transport.tcp.TCPPacket.POS_FIN;
import static java.nio.channels.SelectionKey.OP_CONNECT;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;

import com.wakoo.trafficcap002.networking.PcapWriter;
import com.wakoo.trafficcap002.networking.protocols.ip.IPPacketBuilder;
import com.wakoo.trafficcap002.networking.protocols.ip.ipv4.IPv4PacketBuilder;
import com.wakoo.trafficcap002.networking.protocols.transport.Periodic;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Inet6Address;
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

public class TCPConnection implements ConnectionState {
    private static final TCPOption zero_option_true = new TCPOption(0, true);
    private static final TCPOption zero_option_false = new TCPOption(0, false);

    private final TCPEndpoints endpoints;
    private final TCPOption mss, tx_scale, rx_scale;
    private final SelectionKey key;
    private final FileOutputStream out;
    private final PcapWriter writer;
    private final Map<TCPEndpoints, TCPConnection> connections;
    private final List<ByteBuffer> site_queue; // Очередь отправки на удалённый сайт
    private final List<TCPSegmentData> app_queue; // Очередь отправки на приложение
    private final int[] our_seq; // SEQ следующего отправленного нами пакета
    private int wanted_seq; // SEQ пакета, получение которого ожидается. В наших пакетах будет как ACK.
    private int last_acknowledged; // ACK последнего полученного пакета. Используется для оценки возможности отправки сегментов.
    private int last_window; // Масштабированное последнее значение окна. Вместе с предыдущим параметром используется для оценки возможности отправки сегментов.
    private ConnectionState state;

    public TCPConnection(Map<TCPEndpoints, TCPConnection> connections, TCPPacket packet, TCPEndpoints endpoints, Selector selector, FileOutputStream out, PcapWriter writer) throws IOException {
        this.endpoints = endpoints;
        final SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(false);
        channel.setOption(StandardSocketOptions.SO_REUSEADDR, Boolean.TRUE);
        channel.setOption(StandardSocketOptions.SO_KEEPALIVE, Boolean.TRUE);
        channel.socket().setOOBInline(true);
        channel.connect(endpoints.getSite());
        this.key = channel.register(selector, OP_CONNECT, this);
        this.out = out;
        this.connections = connections;
        this.writer = writer;
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
        connections.remove(endpoints).closeByApplication();
    }

    @Override
    public void consumePacket(TCPPacket packet) throws IOException {
        state.consumePacket(packet);
    }

    @Override
    public void doPeriodic() throws IOException {
        state.doPeriodic();
    }

    @Override
    public void processSelectionKey() throws IOException {
        state.processSelectionKey();
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

    private int getSiteSendQueueSize() {
        int size = 0;
        for (ByteBuffer bb : site_queue) {
            size += bb.remaining();
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
                break;
            }
            i++;
        }
    }

    private void sendRemainingToApp() throws IOException {
        final Iterator<TCPSegmentData> seg_iterator;
        seg_iterator = app_queue.iterator();
        while (seg_iterator.hasNext()) {
            final TCPSegmentData seg;
            seg = seg_iterator.next();
            if (seg.checkTimeoutExpiredThenUpdate() && ((seg.getSequenceNumber() + seg.getSegmentLength()) < (last_acknowledged + last_window))) {
                final TCPPacketBuilder tcp_builder;
                tcp_builder = new TCPPacketBuilder(endpoints.getSite().getPort(),
                        endpoints.getApplication().getPort(),
                        seg.getSegmentData(),
                        seg.getSequenceNumber(), wanted_seq,
                        new boolean[]{false, true, seg.getFlagPush(), false, false, false},
                        getOurRecieveWindow(), 0,
                        zero_option_false, zero_option_false);
                final IPPacketBuilder ip_builder;
                ip_builder = (endpoints.getSite().getAddress() instanceof Inet6Address) ? null : new IPv4PacketBuilder(endpoints.getSite().getAddress(), endpoints.getApplication().getAddress(), tcp_builder, 100, PROTOCOL_TCP); // TODO IPv6
                final byte[][] packets;
                packets = ip_builder.createPackets();
                for (final byte[] packet : packets) {
                    out.write(packet);
                    writer.writePacket(packet, packet.length);
                }
            }
        }
    }

    private void setInterestOptions() {
        if (key.isValid())
            key.interestOps(((last_window - getApplicationSendQueueSize()) > 0 ? OP_READ : 0) | ((getSiteSendQueueSize() > 0) ? OP_WRITE : 0));
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
        ip_builder = (endpoints.getSite().getAddress() instanceof Inet6Address) ? null : new IPv4PacketBuilder(endpoints.getSite().getAddress(), endpoints.getApplication().getAddress(), tcp_builder, 100, PROTOCOL_TCP); // TODO IPv6
        final byte[][] packets;
        packets = ip_builder.createPackets();
        for (final byte[] packet : packets) {
            out.write(packet);
            writer.writePacket(packet, packet.length);
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
        ip_builder = (endpoints.getSite().getAddress() instanceof Inet6Address) ? null : new IPv4PacketBuilder(endpoints.getSite().getAddress(), endpoints.getApplication().getAddress(), tcp_builder, 100, PROTOCOL_TCP); // TODO IPv6
        final byte[][] packets;
        packets = ip_builder.createPackets();
        for (final byte[] packet : packets) {
            out.write(packet);
            writer.writePacket(packet, packet.length);
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
        ip_builder = (endpoints.getSite().getAddress() instanceof Inet6Address) ? null : new IPv4PacketBuilder(endpoints.getSite().getAddress(), endpoints.getApplication().getAddress(), tcp_builder, 100, PROTOCOL_TCP); // TODO IPv6
        final byte[][] packets;
        packets = ip_builder.createPackets();
        for (final byte[] packet : packets) {
            out.write(packet);
            writer.writePacket(packet, packet.length);
        }
    }

    private final class StateSynRecieved extends Periodic implements ConnectionState {
        // Принят пакет SYN. Нужно соединиться с удалённым сайтом и уведомить об этом приложение.

        @Override
        public void consumePacket(TCPPacket tcp_packet) throws IOException {
        }

        @Override
        public void processSelectionKey() throws IOException {
            if (key.isConnectable()) {
                final SocketChannel channel = (SocketChannel) key.channel();
                try {
                    channel.finishConnect();
                    state = new StateSuccessfullyConnected();
                    state.doPeriodic();
                } catch (
                        IOException ioexcp) {
                    // TODO ответить через ICMP, что невозможно подключиться
                }
            }
        }

        @Override
        protected void periodicAction() throws IOException {

        }
    }

    private final class StateSuccessfullyConnected extends Periodic implements ConnectionState {
        // Соединено с удалённым сайтом. Нужно уведомить об этом приложение и получить ответ.

        @Override
        public void consumePacket(TCPPacket tcp_packet) throws IOException {
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
                ip_builder = (endpoints.getSite().getAddress() instanceof Inet6Address) ? null : new IPv4PacketBuilder(endpoints.getSite().getAddress(), endpoints.getApplication().getAddress(), tcp_builder, 100, PROTOCOL_TCP); // TODO сделать IPv6
                byte[][] packets = ip_builder.createPackets();
                for (byte[] packet : packets) {
                    out.write(packet);
                    writer.writePacket(packet, packet.length);
                }
                state = new StateEstablisted();
                last_window = tcp_packet.getWindow(rx_scale.getValue());
                last_acknowledged = tcp_packet.getAck();
                key.interestOps((last_window > 0) ? OP_READ : 0);
            }
        }

        @Override
        public void processSelectionKey() throws IOException {

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
            final IPPacketBuilder ip_builder = (endpoints.getSite().getAddress() instanceof Inet6Address) ? null : new IPv4PacketBuilder(endpoints.getSite().getAddress(), endpoints.getApplication().getAddress(), tcp_builder, 100, PROTOCOL_TCP); // TODO сделать IPv6
            byte[][] packets = ip_builder.createPackets();
            for (byte[] packet : packets) {
                out.write(packet);
                writer.writePacket(packet, packet.length);
            }
        }
    }

    private final class StateEstablisted extends Periodic implements ConnectionState {
        // Соединение установлено с обеими сторонами. Нужно перебрасывать пакеты.

        @Override
        public void consumePacket(TCPPacket tcp_packet) throws IOException {
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
                    if (payload.hasRemaining())
                        site_queue.add(payload);
                    if (tcp_packet.getFlags()[POS_FIN]) {
                        wanted_seq++; // FIN, как и SYN, увеличивает номер последовательности на единицу
                        state = new StateFlushRemainingToBoth();
                    }
                    wanted_seq += urgent_data.limit() + payload.limit();
                }
                if ((!app_queue.isEmpty()) && (last_window != 0))
                    sendRemainingToApp();
                else if ((last_window == 0) || (old_ack != wanted_seq))
                    acknowledge();
            }
            setInterestOptions();
        }

        @Override
        public void processSelectionKey() throws IOException {
            if (key.isValid()) {
                if (key.isReadable()) {
                    final ByteBuffer data;
                    final int readed;
                    data = ByteBuffer.allocate(65536);
                    try {
                        readed = ((SocketChannel) key.channel()).read(data);
                        data.flip();
                        if (readed == -1) {
                            // Сайт разорвал соединение
                            key.channel().close();
                            state = new StateFlushRemainingToApp();
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
                key.channel().close();
                suicide();
            }
        }

        @Override
        protected void periodicAction() throws IOException {
            sendRemainingToApp();
        }
    }

    private final class StateFlushRemainingToApp extends Periodic implements ConnectionState {
        // Сайт разорвал соединение. Сбрасываем остаток на приложение, игнорируя поступающие с него данные, затем отправляем на приложение FIN.

        @Override
        public void consumePacket(TCPPacket tcp_packet) throws IOException {
            if (tcp_packet.getFlags()[POS_ACK]) {
                removeConfirmedSegments(tcp_packet.getAck(), tcp_packet.getWindow(rx_scale.getValue()));
                mockDataReception(tcp_packet);
                if (tcp_packet.getFlags()[POS_FIN]) {
                    wanted_seq++;
                    acknowledge();
                }
                if (app_queue.isEmpty()) {
                    state = new StateOurFinSent();
                    sendFin();
                } else if ((last_window > 0))
                    sendRemainingToApp();
                else {
                    acknowledge();
                }
            }
        }

        @Override
        public void processSelectionKey() throws IOException {
        }

        @Override
        protected void periodicAction() throws IOException {
            sendRemainingToApp();
        }
    }

    private final class StateOurFinSent extends Periodic implements ConnectionState {
        // Данные на приложение сброшены, отправлен наш FIN. Имитируем приём данных и ждём его подтверждения.

        @Override
        public void consumePacket(TCPPacket tcp_packet) throws IOException {
            if (tcp_packet.getFlags()[POS_ACK]) {
                mockDataReception(tcp_packet);
                if (tcp_packet.getFlags()[POS_FIN] && (tcp_packet.getAck() == our_seq[0])) {
                    wanted_seq++;
                    acknowledge();
                    state = new StateSimultaneousClosing();
                } else if (tcp_packet.getAck() == (our_seq[0] + 1)) {
                    our_seq[0]++;
                    state = new StateOurFinAcknowledged();
                } else
                    acknowledge();
            }
        }

        @Override
        public void processSelectionKey() throws IOException {
        }

        @Override
        protected void periodicAction() throws IOException {
            sendFin();
        }
    }

    private final class StateOurFinAcknowledged extends Periodic implements ConnectionState {
        // Наш FIN подтверждён. Имитируем приём данных, ждём FIN от противоположной стороны. Подтверждаем его

        @Override
        public void consumePacket(TCPPacket tcp_packet) throws IOException {
            if (tcp_packet.getFlags()[POS_ACK]) {
                mockDataReception(tcp_packet);
                if (tcp_packet.getFlags()[POS_FIN] && (tcp_packet.getAck() == our_seq[0])) {
                    wanted_seq++;
                    acknowledge();
                    suicide();
                } else
                    acknowledge();
            }
        }

        @Override
        public void processSelectionKey() throws IOException {
        }

        @Override
        protected void periodicAction() throws IOException {
        }
    }

    private final class StateFlushRemainingToBoth extends Periodic implements ConnectionState {
        // Получен FIN от приложения. Сбрасываем очереди и на сайт, и на приложение.

        @Override
        public void consumePacket(TCPPacket tcp_packet) throws IOException {
            if (tcp_packet.getFlags()[POS_ACK]) {
                removeConfirmedSegments(tcp_packet.getAck(), tcp_packet.getWindow(rx_scale.getValue()));
                if ((!app_queue.isEmpty()) && (last_window != 0))
                    sendRemainingToApp();
                else
                    acknowledge();
                if (site_queue.isEmpty() && app_queue.isEmpty()) {
                    sendFin();
                    state = new StateAckWait();
                }
            }
        }

        @Override
        public void processSelectionKey() throws IOException {
            if (key.isValid()) {
                if (key.isReadable()) {
                    final ByteBuffer data;
                    final int readed;
                    data = ByteBuffer.allocate(65536);
                    try {
                        readed = ((SocketChannel) key.channel()).read(data);
                        data.flip();
                        if (readed == -1) {
                            // Сайт разорвал соединение
                            key.channel().close();
                            site_queue.clear();
                            return;
                        } else {
                            final List<TCPSegmentData> segs;
                            segs = TCPSegmentData.makeSegments(data, our_seq, mss.getValue());
                            app_queue.addAll(segs);
                        }
                    } catch (
                            IOException ioexcp) {
                        resetConnection();
                        suicide();
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
                    } catch (
                            IOException ioexcp) {
                        resetConnection();
                        suicide();
                    }
                }
                if (site_queue.isEmpty()) {
                    key.channel().close();
                } else {
                    key.interestOps(OP_WRITE | ((last_window - getApplicationSendQueueSize()) > 0 ? OP_READ : 0));
                }
            } else {
                key.channel().close();
                site_queue.clear();
            }
        }

        @Override
        protected void periodicAction() throws IOException {
            if (site_queue.isEmpty() && app_queue.isEmpty()) {
                sendFin();
                state = new StateAckWait();
            } else if (!app_queue.isEmpty())
                sendRemainingToApp();
        }
    }

    private final class StateAckWait extends Periodic implements ConnectionState {
        // Данные сброшены. Отправляем свой FIN, ждём на него ответа.

        @Override
        public void consumePacket(TCPPacket tcp_packet) throws IOException {
            if (tcp_packet.getFlags()[POS_ACK] && (tcp_packet.getAck() == (our_seq[0] + 1))) {
                suicide();
            }
        }

        @Override
        public void processSelectionKey() throws IOException {

        }

        @Override
        protected void periodicAction() throws IOException {
            sendFin();
        }
    }

    private final class StateSimultaneousClosing extends Periodic implements ConnectionState {
        // Подтверждаем чужой ACK и ждём ответа на свой

        @Override
        public void consumePacket(TCPPacket tcp_packet) throws IOException {
            if (tcp_packet.getFlags()[POS_ACK] && (tcp_packet.getAck() == (our_seq[0] + 1)))
                suicide();
        }

        @Override
        public void processSelectionKey() throws IOException {

        }

        @Override
        protected void periodicAction() throws IOException {
            acknowledge();
        }
    }
}
