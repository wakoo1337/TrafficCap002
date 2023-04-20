package com.wakoo.trafficcap002.networking.protocols.transport.tcp;

import java.io.IOException;

public interface ConnectionState {
    void consumePacket(TCPPacket tcp_packet) throws IOException;

    void doPeriodic() throws IOException;

    void processSelectionKey() throws IOException;
}