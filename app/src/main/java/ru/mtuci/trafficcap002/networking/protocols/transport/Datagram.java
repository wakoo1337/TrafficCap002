package ru.mtuci.trafficcap002.networking.protocols.transport;

import ru.mtuci.trafficcap002.networking.protocols.ip.IPPacket;

import java.nio.ByteBuffer;

public interface Datagram {
    ByteBuffer getPayload();

    int getSourcePort();

    int getDestinationPort();

    IPPacket getParent();
}
