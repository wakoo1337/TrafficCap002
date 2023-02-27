package com.wakoo.trafficcap002.networking.protocols;

import java.net.InetAddress;
import java.nio.ByteBuffer;

public interface Packet {
    int PROTOCOL_IPv4 = 4;
    int PROTOCOL_IPv6 = 6;

    InetAddress getSourceAddress();
    InetAddress getDestinationAddress();
    int getProtocol();
    ByteBuffer getDatagram();
}