package com.wakoo.trafficcap002.networking.protocols.transport.tcp;

import static com.wakoo.trafficcap002.networking.protocols.transport.DatagramConsumer.PROTOCOL_TCP;
import static com.wakoo.trafficcap002.networking.protocols.transport.tcp.TCPPacket.POS_ACK;
import static java.nio.channels.SelectionKey.OP_CONNECT;

import com.wakoo.trafficcap002.networking.PcapWriter;
import com.wakoo.trafficcap002.networking.protocols.ip.IPPacketBuilder;
import com.wakoo.trafficcap002.networking.protocols.ip.ipv4.IPv4PacketBuilder;
import com.wakoo.trafficcap002.networking.protocols.transport.Periodic;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Inet6Address;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class TCPConnection implements ConnectionState {
    private final TCPEndpoints endpoints;
    private final TCPOption mss, scale;
    private final SelectionKey key;
    private final FileOutputStream out;
    private final PcapWriter writer;
    private final List<ByteBuffer> site_queue; // Очередь отправки на удалённый сайт
    private final int wanted_seq; // SEQ пакета, получение которого ожидается. В наших пакетах будет как ACK.
    private int our_seq; // SEQ следующего отправленного нами пакета
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
        this.scale = new TCPOption(0, packet.getScale().getPresence());
        site_queue = new LinkedList<>();
        site_queue.add(packet.getPayload());
        wanted_seq = packet.getSeq() + 1;
        our_seq = ThreadLocalRandom.current().nextInt() ^ endpoints.hashCode() ^ ((int) System.nanoTime());
        state = new StateSynRecieved();
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
        public void consumePacket(TCPPacket tcp_packet) throws IOException {
            if (tcp_packet.getFlags()[POS_ACK] && (tcp_packet.getAck() == (our_seq + 1))) {
                our_seq++;
                final TCPPacketBuilder tcp_builder;
                tcp_builder = new TCPPacketBuilder(endpoints.getSite().getPort(),
                        endpoints.getApplication().getPort(),
                        ByteBuffer.allocate(0),
                        our_seq, wanted_seq,
                        new boolean[]{false, true, false, false, false, false},
                        getOurRecieveWindow(),
                        0, mss, scale);
                final IPPacketBuilder ip_builder;
                ip_builder = (endpoints.getSite().getAddress() instanceof Inet6Address) ? null : new IPv4PacketBuilder(endpoints.getSite().getAddress(), endpoints.getApplication().getAddress(), tcp_builder, 100, PROTOCOL_TCP); // TODO сделать IPv6
                byte[][] packets = ip_builder.createPackets();
                for (byte[] packet : packets) {
                    out.write(packet);
                    writer.writePacket(packet, packet.length);
                }
                state = new StateEstablisted();
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
                    our_seq, wanted_seq,
                    new boolean[]{false, true, false, false, true, false}, // SYN + ACK
                    getOurRecieveWindow(),
                    0, mss, scale);
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
        public void consumePacket(TCPPacket packet) throws IOException {

        }

        @Override
        public void processSelectionKey() throws IOException {

        }

        @Override
        protected void periodicAction() throws IOException {

        }
    }
}
