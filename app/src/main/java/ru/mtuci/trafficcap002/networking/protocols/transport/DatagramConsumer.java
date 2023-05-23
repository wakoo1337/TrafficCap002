package ru.mtuci.trafficcap002.networking.protocols.transport;

import java.util.function.Consumer;

import ru.mtuci.trafficcap002.networking.protocols.ip.IPPacket;

public interface DatagramConsumer extends Consumer<IPPacket> {
    int PROTOCOL_TCP = 6;
    int PROTOCOL_UDP = 17;
    int PROTOCOL_ICMPv4 = 1;
    int PROTOCOL_ICMPv6 = 58;

    void accept(IPPacket parent);
}
