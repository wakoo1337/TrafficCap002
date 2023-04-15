package com.wakoo.trafficcap002.networking.protocols.transport;

import java.nio.ByteBuffer;

public interface Datagram {
    ByteBuffer getPayload();

    int getSourcePort();

    int getDestinationPort();
}
