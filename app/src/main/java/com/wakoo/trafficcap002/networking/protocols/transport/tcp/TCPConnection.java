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
import java.util.concurrent.ThreadLocalRandom;

public class TCPConnection implements ConnectionState {
    private static final TCPOption zero_option_true = new TCPOption(0, true);
    private static final TCPOption zero_option_false = new TCPOption(0, false);

    private final TCPEndpoints endpoints;
    private final TCPOption mss, tx_scale, rx_scale;
    private final SelectionKey key;
    private final FileOutputStream out;
    private final PcapWriter writer;
    private final List<ByteBuffer> site_queue; // Очередь отправки на удалённый сайт
    private final List<TCPSegmentData> app_queue;
    private final int[] our_seq; // SEQ следующего отправленного нами пакета
    private int wanted_seq; // SEQ пакета, получение которого ожидается. В наших пакетах будет как ACK.
    private int last_acknowledged; // ACK последнего полученного пакета. Используется для оценки возможности отправки сегментов.
    private int last_window; // Масштабированное последнее значение окна. Вместе с предыдущим параметром используется для оценки возможности отправки сегментов.
    private ConnectionState state;

    public TCPConnection(TCPPacket packet, TCPEndpoints endpoints, Selector selector, FileOutputStream out, PcapWriter writer) throws IOException {
        this.endpoints = endpoints;
        final SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(false);
        channel.setOption(StandardSocketOptions.SO_REUSEADDR, Boolean.TRUE);
        channel.setOption(StandardSocketOptions.SO_KEEPALIVE, Boolean.TRUE);
        channel.socket().setOOBInline(true);
        channel.connect(endpoints.getSite());
        this.key = channel.register(selector, OP_CONNECT, this);
        this.out = out;
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
        this.last_acknowledged = packet.getAck();
        this.last_window = packet.getWindow(rx_scale.getValue());
        state = new StateSynRecieved();
    }

    @Override
    public boolean consumePacket(TCPPacket packet) throws IOException {
        return state.consumePacket(packet);
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

    private void removeConfirmedSegments() {
        int todel = -1;
        final Iterator<TCPSegmentData> seg_iterator;
        seg_iterator = app_queue.iterator();
        int i = 0;
        while (seg_iterator.hasNext()) {
            final TCPSegmentData seg;
            seg = seg_iterator.next();
            if ((seg.getSequenceNumber() + seg.getSegmentLength()) == last_acknowledged) {
                todel = i;
                break;
            }
            i++;
        }
        app_queue.subList(0, todel + 1).clear();
    }

    private void setInterestOptions() {
        if (key.isValid())
            key.interestOps(((last_window - getApplicationSendQueueSize()) > 0 ? OP_READ : 0) | ((getSiteSendQueueSize() > 0) ? OP_WRITE : 0));
    }

    public void closeByApplication() throws IOException {
        key.channel().close();
    }

    private final class StateSynRecieved extends Periodic implements ConnectionState {
        // Принят пакет SYN. Нужно соединиться с удалённым сайтом и уведомить об этом приложение.

        @Override
        public boolean consumePacket(TCPPacket tcp_packet) throws IOException {
            return false;
        }

        @Override
        public void processSelectionKey() throws IOException {
            if (key.isConnectable()) {
                final SocketChannel channel = (SocketChannel) key.channel();
                try {
                    channel.finishConnect();
                    state = new SuccessfullyConnected();
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

    private final class SuccessfullyConnected extends Periodic implements ConnectionState {
        // Соединено с удалённым сайтом. Нужно уведомить об этом приложение и получить ответ.

        @Override
        public boolean consumePacket(TCPPacket tcp_packet) throws IOException {
            if (tcp_packet.getFlags()[POS_ACK] && (tcp_packet.getAck() == (our_seq[0] + 1))) {
                our_seq[0]++;
                final TCPPacketBuilder tcp_builder;
                tcp_builder = new TCPPacketBuilder(endpoints.getSite().getPort(),
                        endpoints.getApplication().getPort(),
                        ByteBuffer.allocate(0),
                        our_seq[0], wanted_seq,
                        new boolean[]{false, true, false, false, false, false},
                        getOurRecieveWindow(),
                        0, mss, tx_scale);
                final IPPacketBuilder ip_builder;
                ip_builder = (endpoints.getSite().getAddress() instanceof Inet6Address) ? null : new IPv4PacketBuilder(endpoints.getSite().getAddress(), endpoints.getApplication().getAddress(), tcp_builder, 100, PROTOCOL_TCP); // TODO сделать IPv6
                byte[][] packets = ip_builder.createPackets();
                for (byte[] packet : packets) {
                    out.write(packet);
                    writer.writePacket(packet, packet.length);
                }
                state = new StateEstablisted();
                last_window = tcp_packet.getWindow(rx_scale.getValue());
                key.interestOps((last_window > 0) ? OP_READ : 0);
            }
            return false;
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
        public boolean consumePacket(TCPPacket tcp_packet) throws IOException {
            if ((tcp_packet.getSeq() == wanted_seq) && tcp_packet.getFlags()[POS_ACK]) {
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
                    state = new StateCloseWait();
                    key.channel().close();
                }
                wanted_seq += urgent_data.limit() + payload.limit();
                last_acknowledged = tcp_packet.getAck();
                last_window = tcp_packet.getWindow(rx_scale.getValue());
                removeConfirmedSegments();
                final TCPPacketBuilder tcp_builder;
                tcp_builder = new TCPPacketBuilder(endpoints.getSite().getPort(),
                        endpoints.getApplication().getPort(),
                        ByteBuffer.allocate(0),
                        our_seq[0], wanted_seq,
                        new boolean[]{false, true, false, false, false, false},
                        getOurRecieveWindow(), 0,
                        zero_option_false, zero_option_false);
                final IPPacketBuilder ip_builder;
                ip_builder = (endpoints.getSite().getAddress() instanceof Inet6Address) ? null : new IPv4PacketBuilder(endpoints.getSite().getAddress(), endpoints.getApplication().getAddress(), tcp_builder, 100, PROTOCOL_TCP);
                final byte[][] packets;
                packets = ip_builder.createPackets();
                for (final byte[] packet : packets) {
                    out.write(packet);
                    writer.writePacket(packet, packet.length);
                }
                setInterestOptions();
            }
            return false;
        }

        @Override
        public void processSelectionKey() throws IOException {
            if (key.isReadable()) {
                final ByteBuffer data;
                final int readed;
                data = ByteBuffer.allocate(65536);
                readed = ((SocketChannel) key.channel()).read(data);
                data.flip();
                if (readed == -1) {
                    // Сайт разорвал соединение
                    key.channel().close();
                } else {
                    final List<TCPSegmentData> segs;
                    segs = TCPSegmentData.makeSegments(data, our_seq, mss.getValue());
                    app_queue.addAll(segs);
                }
            }
            if (key.isWritable()) {
                final Iterator<ByteBuffer> iterator;
                iterator = site_queue.iterator();
                while (iterator.hasNext()) {
                    final ByteBuffer current;
                    current = iterator.next();
                    final int written;
                    written = ((SocketChannel) key.channel()).write(current);
                    if (!current.hasRemaining())
                        iterator.remove();
                    if (written == -1) {
                        // Сайт разорвал соединение
                        key.channel().close();
                    }
                }
            }
            setInterestOptions();
        }

        @Override
        protected void periodicAction() throws IOException {
            final Iterator<TCPSegmentData> seg_iterator;
            seg_iterator = app_queue.iterator();
            while (seg_iterator.hasNext()) {
                final TCPSegmentData seg;
                seg = seg_iterator.next();
                if (seg.checkTimeoutExpiredThenUpdate()) {
                    final TCPPacketBuilder tcp_builder;
                    tcp_builder = new TCPPacketBuilder(endpoints.getSite().getPort(),
                            endpoints.getApplication().getPort(),
                            seg.getSegmentData(),
                            seg.getSequenceNumber(), wanted_seq,
                            new boolean[]{false, true, true, false, false, false},
                            getOurRecieveWindow(), 0,
                            zero_option_false, zero_option_false);
                    final IPPacketBuilder ip_builder;
                    ip_builder = (endpoints.getSite().getAddress() instanceof Inet6Address) ? null : new IPv4PacketBuilder(endpoints.getSite().getAddress(), endpoints.getApplication().getAddress(), tcp_builder, 100, PROTOCOL_TCP);
                    final byte[][] packets;
                    packets = ip_builder.createPackets();
                    for (final byte[] packet : packets) {
                        out.write(packet);
                        writer.writePacket(packet, packet.length);
                    }
                }
            }
        }
    }

    private final class StateCloseWait extends Periodic implements ConnectionState {
        // Получен FIN-пакет от приложения. Нужно прекратить приём данных от приложения, закончить отправку на приложение и отослать FIN самим.

        @Override
        public boolean consumePacket(TCPPacket packet) throws IOException {
            return false;
        }

        @Override
        public void processSelectionKey() throws IOException {

        }

        @Override
        protected void periodicAction() throws IOException {

        }
    }
}
