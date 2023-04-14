package com.wakoo.trafficcap002.networking.protocols.transport.tcp;

import java.io.IOException;

public interface ConnectionState {
    void consumePacket(TCPPacket packet) throws IOException;

    void doPeriodic() throws IOException;
}