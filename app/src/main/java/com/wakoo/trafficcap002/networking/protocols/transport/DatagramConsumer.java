package com.wakoo.trafficcap002.networking.protocols.transport;

import com.wakoo.trafficcap002.networking.protocols.ip.IPPacket;

import java.util.function.Consumer;

public interface DatagramConsumer extends Consumer<IPPacket> {
    int PROTOCOL_TCP = 6;
    int PROTOCOL_UDP = 17;
    int PROTOCOL_ICMPv4 = 1;
    int PROTOCOL_ICMPv6 = 58;

    void accept(IPPacket parent);
}
