package com.wakoo.trafficcap002.networking.protocols.transport.tcp;

import java.io.IOException;

public interface ConnectionState {
    boolean consumePacket(TCPPacket packet) throws IOException; // Удалять ли соединение из списка

    void doPeriodic() throws IOException;

    void processSelectionKey() throws IOException;
}