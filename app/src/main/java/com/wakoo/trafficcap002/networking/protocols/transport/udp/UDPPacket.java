package com.wakoo.trafficcap002.networking.protocols.transport.udp;

import com.wakoo.trafficcap002.networking.protocols.ip.IPPacket;
import com.wakoo.trafficcap002.networking.protocols.transport.Datagram;

import java.net.InetAddress;
import java.nio.ByteBuffer;

public class UDPPacket implements IPPacket, Datagram {
    @Override
    public InetAddress getSourceAddress() {
        return null;
    }

    @Override
    public InetAddress getDestinationAddress() {
        return null;
    }

    @Override
    public int getProtocol() {
        return 0;
    }

    @Override
    public ByteBuffer getDatagram() {
        return null;
    }

    @Override
    public ByteBuffer getChecksumPseudoHeader() {
        return null;
    }

    @Override
    public ByteBuffer getPayload() {
        return null;
    }

    @Override
    public int getSourcePort() {
        return 0;
    }

    @Override
    public int getDestinationPort() {
        return 0;
    }
}
