package com.wakoo.trafficcap002.networking.protocols.transport.tcp;

import com.wakoo.trafficcap002.networking.HttpWriter;

import java.io.IOException;

public interface ConnectionState {
    void consumePacket(TCPPacket tcp_packet, HttpWriter http_writer) throws IOException;

    void doPeriodic() throws IOException;

    void processSelectionKey(HttpWriter writer) throws IOException;
}