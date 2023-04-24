package com.wakoo.trafficcap002.networking.protocols.transport.udp;

import com.wakoo.trafficcap002.networking.ChecksumComputer;
import com.wakoo.trafficcap002.networking.protocols.transport.DatagramBuilder;

import java.nio.ByteBuffer;

public class UDPPacketBuilder implements DatagramBuilder {
    private final int src_port, dst_port;
    private final ByteBuffer data;

    public UDPPacketBuilder(int src_port, int dst_port, ByteBuffer data) {
        this.src_port = src_port;
        this.dst_port = dst_port;
        this.data = data;
    }

    @Override
    public int getDatagramSize() {
        return 8 + data.limit();
    }

    @Override
    public void fillPacketWithData(byte[][] bytes, int[] offsets, ChecksumComputer cc) {
        assert bytes.length == offsets.length;
        final ByteBuffer udp_header = ByteBuffer.allocate(8);
        udp_header.putShort((short) src_port);
        udp_header.putShort((short) dst_port);
        udp_header.putShort((short) (data.limit() + 8));
        udp_header.putShort((short) 0);
        udp_header.position(0);
        data.position(0);
        cc.moreData(udp_header).moreData(data);
        udp_header.putShort(6, (short) cc.get());
        udp_header.position(0);
        data.position(0);
        for (int i = 0; i < bytes.length; i++) {
            int offset = offsets[i];
            // TODO вынести
            if (udp_header.position() < udp_header.limit()) {
                final int copied = Integer.min(udp_header.remaining(), bytes[i].length - offset);
                udp_header.get(bytes[i], offset, copied);
                offset += copied;
            }
            if (udp_header.position() == udp_header.limit()) {
                final int copied = Integer.min(data.remaining(), bytes[i].length - offset);
                data.get(bytes[i], offset, copied);
                offset += copied;
            }
        }
    }
}
