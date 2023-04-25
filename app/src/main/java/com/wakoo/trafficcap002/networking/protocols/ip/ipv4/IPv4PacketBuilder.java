package com.wakoo.trafficcap002.networking.protocols.ip.ipv4;

import static com.wakoo.trafficcap002.networking.threads.DescriptorListener.INTERFACE_MTU;

import com.wakoo.trafficcap002.networking.ChecksumComputer;
import com.wakoo.trafficcap002.networking.protocols.ip.IPPacketBuilder;
import com.wakoo.trafficcap002.networking.protocols.transport.DatagramBuilder;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class IPv4PacketBuilder implements IPPacketBuilder {
    private static final int HEADER_SIZE = 20;
    private static short identification = 1;
    private final InetAddress src, dst;
    private final DatagramBuilder builder;
    private final int ttl, proto;

    public IPv4PacketBuilder(InetAddress src, InetAddress dst, DatagramBuilder builder, int ttl, int proto) {
        assert (src instanceof Inet4Address) && (dst instanceof Inet4Address);
        this.src = src;
        this.dst = dst;
        this.builder = builder;
        this.ttl = ttl;
        this.proto = proto;
    }

    @Override
    public byte[][] createPackets() {
        final int fragment_max = INTERFACE_MTU - HEADER_SIZE;
        final int datagram_size = builder.getDatagramSize();
        final int fragments_count = (datagram_size / fragment_max) + (((datagram_size % fragment_max) > 0) ? 1 : 0);
        final byte[][] bytes = new byte[fragments_count][];
        final int[] offsets = new int[fragments_count];
        Arrays.fill(offsets, HEADER_SIZE);
        int remaining = datagram_size;
        for (int i = 0; i < fragments_count; i++) {
            final int current_length = (i < (fragments_count - 1)) ? (Integer.min(fragment_max, remaining) & -8) : remaining;
            bytes[i] = new byte[current_length + HEADER_SIZE];
            final ByteBuffer header = (ByteBuffer) ByteBuffer.wrap(bytes[i]).limit(20);
            header.putShort((short) (69 * 256)); // IPv4 и заголовок в 20 байтов, грязный извращенец!
            header.putShort((short) (current_length + HEADER_SIZE));
            header.putShort(identification);
            final short flags_offset = (short) (((i < (fragments_count - 1)) ? 8192 : 0) + ((datagram_size - remaining) >>> 3));
            header.putShort(flags_offset);
            header.put((byte) ttl);
            header.put((byte) proto);
            header.putShort((short) 0);
            header.put(src.getAddress());
            header.put(dst.getAddress());
            final ChecksumComputer header_cc = new ChecksumComputer();
            header.position(0);
            header_cc.moreData(header);
            header.putShort(10, (short) header_cc.get());
            remaining -= current_length;
        }
        final ByteBuffer pseudo_header = ByteBuffer.allocate(12);
        pseudo_header.put(src.getAddress());
        pseudo_header.put(dst.getAddress());
        pseudo_header.put((byte) 0);
        pseudo_header.put((byte) proto);
        pseudo_header.putShort((short) datagram_size);
        pseudo_header.position(0);
        final ChecksumComputer pseudo_cc = new ChecksumComputer();
        pseudo_cc.moreData(pseudo_header);
        builder.fillPacketWithData(bytes, offsets, pseudo_cc);
        identification++;
        return bytes;
    }
}
