package com.wakoo.trafficcap002.networking.protocols.transport;

import com.wakoo.trafficcap002.networking.protocols.ip.IPPacket;

import java.net.InetAddress;
import java.nio.ByteBuffer;

public interface DatagramConsumer {
    int PROTOCOL_TCP = 6;
    int PROTOCOL_UDP = 17;

    void accept(IPPacket parent);
}
