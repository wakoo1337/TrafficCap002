package com.wakoo.trafficcap002.networking.protocols.ip;

import com.wakoo.trafficcap002.networking.protocols.Packet;

import java.net.InetAddress;
import java.nio.ByteBuffer;

public class IPv6Packet implements Packet {
    public IPv6Packet(ByteBuffer packet) {

    }

    public static IPv6Packet of(ByteBuffer packet) throws BadIPPacketException {
        return null;
    }

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
}
