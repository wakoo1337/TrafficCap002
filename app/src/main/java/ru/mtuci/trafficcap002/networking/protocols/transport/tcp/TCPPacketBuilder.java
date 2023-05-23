package ru.mtuci.trafficcap002.networking.protocols.transport.tcp;

import java.nio.ByteBuffer;

import ru.mtuci.trafficcap002.networking.ChecksumComputer;
import ru.mtuci.trafficcap002.networking.protocols.transport.DatagramBuilder;

public final class TCPPacketBuilder implements DatagramBuilder {
    private final int src_port, dst_port;
    private final ByteBuffer data;
    private final int seq, ack;
    private final boolean[] flags;
    private final int window, urgent;
    private final TCPOption mss;
    private final TCPOption scale;

    public TCPPacketBuilder(int src_port, int dst_port, ByteBuffer data, int seq, int ack, boolean[] flags, int window, int urgent, TCPOption mss, TCPOption scale) {
        this.src_port = src_port;
        this.dst_port = dst_port;
        this.data = data;
        this.seq = seq;
        this.ack = ack;
        assert flags.length == 6;
        this.flags = flags;
        this.window = window;
        this.urgent = urgent;
        this.mss = mss;
        this.scale = scale;
    }

    @Override
    public int getDatagramSize() {
        return 20 + data.limit() + (mss.getPresence() ? 4 : 0) + (scale.getPresence() ? 4 : 0);
    }

    @Override
    public void fillPacketWithData(byte[][] bytes, int[] offsets, ChecksumComputer cc) {
        assert bytes.length == offsets.length;
        final ByteBuffer tcp_header = ByteBuffer.allocate(20 + (mss.getPresence() ? 4 : 0) + (scale.getPresence() ? 4 : 0));
        tcp_header.putShort((short) src_port);
        tcp_header.putShort((short) dst_port);
        tcp_header.putInt(seq);
        tcp_header.putInt(ack);
        tcp_header.put((byte) ((5 + (mss.getPresence() ? 1 : 0) + (scale.getPresence() ? 1 : 0)) << 4));
        final byte flags_byte =
                (byte) ((flags[0] ? 32 : 0) |
                        (flags[1] ? 16 : 0) |
                        (flags[2] ? 8 : 0) |
                        (flags[3] ? 4 : 0) |
                        (flags[4] ? 2 : 0) |
                        (flags[5] ? 1 : 0));
        tcp_header.put(flags_byte);
        tcp_header.putShort((short) window);
        tcp_header.putShort((short) 0);
        tcp_header.putShort((short) urgent);
        if (mss.getPresence()) {
            tcp_header.putShort((short) 0x0204);
            tcp_header.putShort((short) mss.getValue());
        }
        if (scale.getPresence()) {
            tcp_header.putShort((short) 0x0303);
            tcp_header.putShort((short) (scale.getValue() * 256 + 1));
        }
        tcp_header.position(0);
        cc.moreData(tcp_header).moreData(data);
        data.position(0);
        tcp_header.putShort(16, (short) cc.get());
        tcp_header.position(0);
        for (int i = 0; i < bytes.length; i++) {
            int offset = offsets[i];
            // TODO вынести
            if (tcp_header.position() < tcp_header.limit()) {
                final int copied = Integer.min(tcp_header.remaining(), bytes[i].length - offset);
                tcp_header.get(bytes[i], offset, copied);
                offset += copied;
            }
            if (tcp_header.position() == tcp_header.limit()) {
                final int copied = Integer.min(data.remaining(), bytes[i].length - offset);
                data.get(bytes[i], offset, copied);
                offset += copied;
            }
        }
    }
}
