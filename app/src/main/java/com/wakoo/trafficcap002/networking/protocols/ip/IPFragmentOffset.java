package com.wakoo.trafficcap002.networking.protocols.ip;

public class IPFragmentOffset {
    private final int length;
    private final int datagram_offset;
    private final int packet_offset;

    public IPFragmentOffset(int length, int datagram_offset, int packet_offset) {
        this.length = length;
        this.datagram_offset = datagram_offset;
        this.packet_offset = packet_offset;
    }

    public int getLength() {
        return length;
    }

    public int getDatagramOffset() {
        return datagram_offset;
    }

    public int getPacketOffset() {
        return packet_offset;
    }
}
