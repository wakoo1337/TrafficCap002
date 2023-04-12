package com.wakoo.trafficcap002.networking.protocols.ip.ipv4;

import static com.wakoo.trafficcap002.networking.threads.DescriptorListener.INTERFACE_MTU;

import com.wakoo.trafficcap002.networking.ChecksumComputer;
import com.wakoo.trafficcap002.networking.protocols.ip.IPFragmentOffset;
import com.wakoo.trafficcap002.networking.protocols.ip.IPPacketBuilder;
import com.wakoo.trafficcap002.networking.protocols.transport.DatagramBuilder;

import java.net.Inet4Address;
import java.nio.ByteBuffer;

public class IPv4PacketBuilder implements IPPacketBuilder {
    private final Inet4Address src, dest;
    private final DatagramBuilder builder;
    private final int proto;

    public IPv4PacketBuilder(Inet4Address src, Inet4Address dest, DatagramBuilder builder, int proto) {
        this.src = src;
        this.dest = dest;
        this.builder = builder;
        this.proto = proto;
    }

    private static short identification = 1;
    private static final int HEADER_SIZE = 20;

    @Override
    public byte[][] createPackets() {
        final int fragment_max = INTERFACE_MTU - HEADER_SIZE;
        final int total = builder.getDatagramSize() + HEADER_SIZE;
        final int fragments_count = (total / fragment_max) + (((total % fragment_max) != 0) ? 1 : 0);
        final byte[][] bytes = new byte[fragments_count][];
        final IPFragmentOffset[] offsets = new IPFragmentOffset[fragments_count];
        int remaining = total;
        for (int i = 0; i < fragments_count; i++) {
            final int current_length = (i < (fragments_count - 1)) ? (Integer.min(fragment_max, remaining) & -8) : remaining;
            bytes[i] = new byte[current_length];
            offsets[i] = new IPFragmentOffset(current_length, total - remaining, 20);
            final ByteBuffer header = (ByteBuffer) ((ByteBuffer) ByteBuffer.wrap(bytes[i])).limit(20);
            header.putShort((short) (69 * 256));
            header.putShort((short) total);
            header.putShort(identification);
            final short flags_offset = (short) (16384 + ((i < (fragments_count - 1)) ? 8192 : 0) + ((total-remaining) >>> 3));
            header.putShort(flags_offset);
            header.put((byte) proto);
            header.putShort((short) 0);
            header.put(src.getAddress());
            header.put(dest.getAddress());
            final ChecksumComputer header_cc = new ChecksumComputer();
            header.position(0);
            header_cc.moreData(header);
            header.putShort(10, (short) header_cc.get());
            remaining -= current_length;
        }
        final ByteBuffer pseudo_header = ByteBuffer.allocate(12);
        pseudo_header.put(src.getAddress());
        pseudo_header.put(dest.getAddress());
        pseudo_header.put((byte) 0);
        pseudo_header.put((byte) proto);
        pseudo_header.putShort((short) total);
        final ChecksumComputer pseudo_cc = new ChecksumComputer();
        pseudo_cc.moreData(pseudo_header);
        builder.fillPacketWithData(bytes, offsets, pseudo_cc);
        identification++;
        return bytes;
    }
}
