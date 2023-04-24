package com.wakoo.trafficcap002.networking.protocols.transport.tcp;

import static com.wakoo.trafficcap002.networking.protocols.transport.tcp.TCPPacket.POS_ACK;
import static com.wakoo.trafficcap002.networking.protocols.transport.tcp.TCPPacket.POS_RST;
import static com.wakoo.trafficcap002.networking.protocols.transport.tcp.TCPPacket.POS_SYN;

import android.util.Log;

import com.wakoo.trafficcap002.networking.PcapWriter;
import com.wakoo.trafficcap002.networking.protocols.ip.IPPacket;
import com.wakoo.trafficcap002.networking.protocols.ip.IPPacketBuilder;
import com.wakoo.trafficcap002.networking.protocols.ip.ipv4.IPv4PacketBuilder;
import com.wakoo.trafficcap002.networking.protocols.transport.BadDatagramException;
import com.wakoo.trafficcap002.networking.protocols.transport.DatagramConsumer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.util.HashMap;
import java.util.Map;

public class TCPDatagramConsumer implements DatagramConsumer {
    private final Map<TCPEndpoints, TCPConnection> connections;
    private final Selector selector;
    private final FileOutputStream out;
    private final PcapWriter writer;

    public TCPDatagramConsumer(Selector selector, FileOutputStream out, PcapWriter writer) {
        connections = new HashMap<>();
        this.selector = selector;
        this.out = out;
        this.writer = writer;
    }

    @Override
    public void accept(IPPacket parent) {
        try {
            final TCPPacket tcp_packet;
            tcp_packet = TCPPacket.of(parent);
            final TCPEndpoints endpoints;
            endpoints = new TCPEndpoints(new InetSocketAddress(tcp_packet.getParent().getSourceAddress(), tcp_packet.getSourcePort()), new InetSocketAddress(tcp_packet.getParent().getDestinationAddress(), tcp_packet.getDestinationPort()));
            final TCPConnection connection;
            connection = connections.get(endpoints);
            if (connection != null) {
                if (tcp_packet.getFlags()[POS_RST])
                    connections.remove(endpoints).closeByApplication();
                else
                    connection.consumePacket(tcp_packet);
            } else {
                // Соединения нет
                if (!(tcp_packet.getFlags()[POS_RST] || tcp_packet.getFlags()[POS_SYN])) {
                    final TCPPacketBuilder tcp_builder;
                    final boolean[] rst_flag = new boolean[]{false, false, false, true, false, false};
                    tcp_builder = new TCPPacketBuilder(tcp_packet.getDestinationPort(), tcp_packet.getSourcePort(), ByteBuffer.allocate(0), 0, tcp_packet.getSeq(), rst_flag, 0, 0, new TCPOption(536, false), new TCPOption(0, false));
                    final IPPacketBuilder ip_builder;
                    ip_builder = ((parent.getDestinationAddress()) instanceof Inet6Address) ? null : new IPv4PacketBuilder(tcp_packet.getParent().getDestinationAddress(), tcp_packet.getParent().getSourceAddress(), tcp_builder, 100, PROTOCOL_TCP);
                    final byte[][] packets;
                    packets = ip_builder.createPackets();
                    for (byte[] packet : packets) {
                        out.write(packet);
                        writer.writePacket(packet, packet.length);
                    }
                } else if (tcp_packet.getFlags()[POS_SYN] && (!tcp_packet.getFlags()[POS_ACK]) && (!tcp_packet.getFlags()[POS_RST])) {
                    // Принимаем новое соединение
                    final TCPConnection new_connection;
                    new_connection = new TCPConnection(connections, tcp_packet, endpoints, this.selector, this.out, this.writer);
                    connections.put(endpoints, new_connection);
                }
            }
        } catch (
                BadDatagramException badexcp) {
            Log.e("Разбор пакета TCP", "Плохой пакет TCP", badexcp);
        } catch (
                IOException ioexcp) {
            Log.e("Работа с TCP", "Ошибка ввода-вывода", ioexcp);
        }
    }

    public void doPeriodic() throws IOException {
        for (final TCPConnection connection : connections.values()) {
            connection.doPeriodic();
        }
    }
}
