package ru.mtuci.trafficcap002.networking.protocols.transport.tcp;

import java.io.IOException;

import ru.mtuci.trafficcap002.networking.HttpWriter;

public interface ConnectionState {
    void consumePacket(TCPPacket tcp_packet, HttpWriter http_writer) throws IOException;

    void doPeriodic() throws IOException;

    void processSelectionKey(HttpWriter writer) throws IOException;
}