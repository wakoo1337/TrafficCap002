package com.wakoo.trafficcap002.networking.protocols.transport;

import com.wakoo.trafficcap002.networking.protocols.ip.IPPacket;

import java.nio.ByteBuffer;

public interface Datagram {
    ByteBuffer getPayload();

    int getSourcePort();

    int getDestinationPort();

    IPPacket getParent();
}
