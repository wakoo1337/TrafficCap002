package com.wakoo.trafficcap002.networking.protocols.ip.ipv6;

import static com.wakoo.trafficcap002.networking.threads.DescriptorListener.INTERFACE_MTU;

import com.wakoo.trafficcap002.networking.ChecksumComputer;
import com.wakoo.trafficcap002.networking.protocols.ip.IPPacketBuilder;
import com.wakoo.trafficcap002.networking.protocols.transport.DatagramBuilder;

import java.net.InetAddress;
import java.nio.ByteBuffer;

public class IPv6PacketBuilder implements IPPacketBuilder {
    private static final int identification = 1;

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
        if (datagram_size > max_nofrag) {
            // TODO сделать фрагментированные пакеты
            return new byte[0][];
        } else {
            final byte[][] bytes = new byte[1][];
            bytes[0] = new byte[datagram_size + 40];
            final ByteBuffer header = (ByteBuffer) ByteBuffer.wrap(bytes[0]).limit(40);
            header.put((byte) 96);
            header.put((byte) 0);
            header.putShort((short) 0);
            header.putShort((short) datagram_size);
            header.put((byte) proto);
            header.put((byte) ttl);
            header.put(src.getAddress());
            header.put(dst.getAddress());
            builder.fillPacketWithData(bytes, new int[]{40}, cc);
            return bytes;
        }
    }
}
