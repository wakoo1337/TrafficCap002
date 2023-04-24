package com.wakoo.trafficcap002.networking.protocols.transport.udp;

import static com.wakoo.trafficcap002.networking.protocols.transport.DatagramConsumer.PROTOCOL_UDP;
import static java.nio.channels.SelectionKey.OP_READ;

import com.wakoo.trafficcap002.CaptureService;
import com.wakoo.trafficcap002.networking.PcapWriter;
import com.wakoo.trafficcap002.networking.protocols.ip.IPPacketBuilder;
import com.wakoo.trafficcap002.networking.protocols.ip.ipv4.IPv4PacketBuilder;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Selector;

public class UDPExternalPort {
    private final DatagramChannel channel;
    private final int port;
    private final FileOutputStream out;
    private final PcapWriter writer;

    public UDPExternalPort(int port, Selector selector, FileOutputStream out, PcapWriter writer) throws IOException {
        channel = DatagramChannel.open();
        channel.configureBlocking(false);
        channel.setOption(StandardSocketOptions.SO_REUSEADDR, Boolean.TRUE);
        channel.bind(null);
        channel.register(selector, OP_READ, this);
        this.port = port;
        this.out = out;
        this.writer = writer;
    }

    public int getPort() {
        return port;
    }

    public DatagramChannel getChannel() {
        return channel;
    }

    public void relayDatagram() throws IOException {
        final ByteBuffer buffer = ByteBuffer.allocate(65536);
        final SocketAddress from;
        from = channel.receive(buffer);
        buffer.flip();
        if (!(from instanceof InetSocketAddress))
            throw new IOException("UDP-датаграмма не из Интернета");
        final InetSocketAddress inet_from;
        inet_from = (InetSocketAddress) from;
        final UDPPacketBuilder udp_builder;
        udp_builder = new UDPPacketBuilder(inet_from.getPort(), port, buffer);
        final IPPacketBuilder ip_builder;
        if (inet_from.getAddress() instanceof Inet4Address) {
            ip_builder = new IPv4PacketBuilder(inet_from.getAddress(), CaptureService.getLocalInet4(), udp_builder, 100, PROTOCOL_UDP);
            final byte[][] packets;
            packets = ip_builder.createPackets();
            for (final byte[] packet : packets) {
                out.write(packet);
                writer.writePacket(packet, packet.length);
            }
        } else if (inet_from.getAddress() instanceof Inet6Address) {

        }
    }
}
