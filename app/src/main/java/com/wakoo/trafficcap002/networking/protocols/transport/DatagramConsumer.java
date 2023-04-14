package com.wakoo.trafficcap002.networking.protocols.transport;

import com.wakoo.trafficcap002.networking.protocols.ip.IPPacket;

public interface DatagramConsumer {
    int PROTOCOL_TCP = 6;
    int PROTOCOL_UDP = 17;
    int PROTOCOL_ICMP = 1;

    void accept(IPPacket parent);
}
