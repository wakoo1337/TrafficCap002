package ru.mtuci.trafficcap002.networking.protocols.transport;

import java.nio.ByteBuffer;

import ru.mtuci.trafficcap002.networking.protocols.ip.IPPacket;

public interface Datagram {
    ByteBuffer getPayload();

    int getSourcePort();

    int getDestinationPort();

    IPPacket getParent();
}
