package com.wakoo.trafficcap002.networking.protocols.transport;

import com.wakoo.trafficcap002.networking.ChecksumComputer;
import com.wakoo.trafficcap002.networking.protocols.ip.IPFragmentOffset;

public interface DatagramBuilder {
    int getDatagramSize();
    void fillPacketWithData(byte[][] bytes, IPFragmentOffset[] offsets, ChecksumComputer cc);
}
