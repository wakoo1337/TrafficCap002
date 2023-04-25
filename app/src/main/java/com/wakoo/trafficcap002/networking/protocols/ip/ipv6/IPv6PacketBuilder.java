package com.wakoo.trafficcap002.networking.protocols.ip.ipv6;

import static com.wakoo.trafficcap002.networking.protocols.ip.ipv6.IPv6Packet.HEADER_FRAGMENT;
import static com.wakoo.trafficcap002.networking.threads.DescriptorListener.INTERFACE_MTU;

import com.wakoo.trafficcap002.networking.ChecksumComputer;
import com.wakoo.trafficcap002.networking.protocols.ip.IPPacketBuilder;
import com.wakoo.trafficcap002.networking.protocols.transport.DatagramBuilder;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class IPv6PacketBuilder implements IPPacketBuilder {
    private static int identification = 1;

    private final InetAddress src, dst;
    private final DatagramBuilder builder;
    private final int ttl, proto;

    public IPv6PacketBuilder(InetAddress src, InetAddress dst, DatagramBuilder builder, int ttl, int proto) {
        this.src = src;
        this.dst = dst;
        this.builder = builder;
        this.ttl = ttl;
        this.proto = proto;
    }

    @Override
    public byte[][] createPackets() {
        final int max_nofrag = INTERFACE_MTU - 40;
        final int datagram_size = builder.getDatagramSize();
        final ByteBuffer pseudo = ByteBuffer.allocate(40);
        pseudo.put(src.getAddress());
        pseudo.put(dst.getAddress());
        pseudo.putInt(datagram_size);
        pseudo.putInt(proto);
        pseudo.position(0);
        final ChecksumComputer cc;
        cc = new ChecksumComputer();
        cc.moreData(pseudo);
        final byte[][] bytes;
        if (datagram_size > max_nofrag) {
            final int per_fragment = max_nofrag-8;
            final int fragments_count = datagram_size / per_fragment + (((datagram_size % per_fragment) != 0) ? 1 : 0);
            int remaining = datagram_size;
            final int[] offsets = new int[fragments_count];
            Arrays.fill(offsets, 48);
            bytes = new byte[fragments_count][];
            for (int i = 0; i < fragments_count; i++) {
                final int current_length = (i < (fragments_count - 1)) ? (Integer.min(per_fragment, remaining) & -8) : remaining;
                bytes[i] = new byte[48 + current_length];
                final ByteBuffer header = (ByteBuffer) ByteBuffer.wrap(bytes[i]).limit(48);
                header.putShort((short) (96*256));
                header.putShort((short) 0);
                header.putShort((short) datagram_size);
                header.put((byte) HEADER_FRAGMENT);
                header.put((byte) ttl);
                header.put(src.getAddress());
                header.put(dst.getAddress());
                header.putShort((short) (proto*256));
                final short frag_offset_M = (short) (((datagram_size - remaining) & -8) + (i == (fragments_count-1) ? 1 : 0));
                header.putShort(frag_offset_M);
                header.putInt(identification);
                remaining -= current_length;
            }
            builder.fillPacketWithData(bytes, offsets, cc);
            identification++;
            return bytes;
        } else {
            bytes = new byte[1][];
            bytes[0] = new byte[datagram_size + 40];
            final ByteBuffer header = (ByteBuffer) ByteBuffer.wrap(bytes[0]).limit(40);
            header.putShort((byte) (96*256));
            header.putShort((short) 0);
            header.putShort((short) datagram_size);
            header.put((byte) proto);
            header.put((byte) ttl);
            header.put(src.getAddress());
            header.put(dst.getAddress());
            builder.fillPacketWithData(bytes, new int[]{40}, cc);
        }
        return bytes;
    }
}
