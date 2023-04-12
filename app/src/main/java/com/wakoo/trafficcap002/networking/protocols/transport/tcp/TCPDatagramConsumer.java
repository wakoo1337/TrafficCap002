package com.wakoo.trafficcap002.networking.protocols.transport.tcp;

import static com.wakoo.trafficcap002.networking.protocols.transport.tcp.TCPPacket.POS_RST;

import android.util.Log;

import com.wakoo.trafficcap002.networking.protocols.ip.IPPacketBuilder;
import com.wakoo.trafficcap002.networking.protocols.ip.ipv4.IPv4PacketBuilder;
import com.wakoo.trafficcap002.networking.protocols.transport.BadDatagramException;
import com.wakoo.trafficcap002.networking.protocols.transport.DatagramConsumer;
import com.wakoo.trafficcap002.networking.protocols.ip.IPPacket;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class TCPDatagramConsumer implements DatagramConsumer {
    private final Map<TCPEndpoints, TCPConnection> connections;

    public TCPDatagramConsumer() {
        connections = new HashMap<>();
    }

    @Override
    public void accept(IPPacket parent) {
        try {
            final TCPPacket packet;
            packet = TCPPacket.of(parent);
            final TCPEndpoints endpoints;
            endpoints = new TCPEndpoints(new InetSocketAddress(packet.getSourceAddress(), packet.getSourcePort()), new InetSocketAddress(packet.getDestinationAddress(), packet.getDestinationPort()));
            final TCPConnection connection;
            connection = connections.get(endpoints);
            if (connection != null) {

            } else {
                // Соединения нет
                if (!packet.getFlags()[POS_RST]) {
                    // Тут нужно сгенерировать RESET
                    final TCPPacketBuilder tcp_builder;
                    final boolean[] rst_flag = new boolean[]{false, false, false, true, false, false};
                    tcp_builder = new TCPPacketBuilder(packet.getDestinationPort(), packet.getSourcePort(), ByteBuffer.allocate(0), 0, packet.getSeq(), rst_flag, 0, 0, false, 1, false, 0);
                    final IPPacketBuilder ip_builder;
                    ip_builder = ((parent.getDestinationAddress()) instanceof Inet6Address) ? null : new IPv4PacketBuilder((Inet4Address) packet.getDestinationAddress(), (Inet4Address) packet.getSourceAddress(), tcp_builder, PROTOCOL_TCP);
                    final byte[][] packets;
                    packets = ip_builder.createPackets();
                }
            }
        } catch (BadDatagramException badexcp) {
            Log.e("Разбор пакета TCP", "Плохой пакет TCP", badexcp);
        }
    }
}
