package ru.mtuci.trafficcap002.networking.protocols.transport;

import ru.mtuci.trafficcap002.networking.ChecksumComputer;

public interface DatagramBuilder {
    int getDatagramSize();

    void fillPacketWithData(byte[][] bytes, int[] offsets, ChecksumComputer cc);
}
