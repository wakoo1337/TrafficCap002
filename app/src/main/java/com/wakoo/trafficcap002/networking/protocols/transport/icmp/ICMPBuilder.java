package com.wakoo.trafficcap002.networking.protocols.transport.icmp;

import com.wakoo.trafficcap002.networking.ChecksumComputer;
import com.wakoo.trafficcap002.networking.protocols.ip.IPPacket;
import com.wakoo.trafficcap002.networking.protocols.transport.DatagramBuilder;

import java.nio.ByteBuffer;

public class ICMPBuilder implements DatagramBuilder {
    // Я знаю, что ICMP — протокол сетевой, а не транспортный

    public static final int TYPE_DESTINATION_UNREACHABLE=3;

    public static final int CODE_HOST_UNREACHABLE=1;
    public static final int CODE_PORT_UNREACHABLE=3;

    private final IPPacket packet;
    private final int type, code;

    public ICMPBuilder(IPPacket packet, int type, int code) {
        this.packet = packet;
        this.type = type;
        this.code = code;
    }

    @Override
    public int getDatagramSize() {
        return 8 + packet.getICMPReplyData().length;
    }

    @Override
    public void fillPacketWithData(byte[][] bytes, int[] offsets, ChecksumComputer cc) {
        final ByteBuffer icmp_header = ByteBuffer.allocate(8);
        icmp_header.put((byte) type);
        icmp_header.put((byte) code);
        icmp_header.putShort((short) 0);
        icmp_header.putInt(0);
        final ChecksumComputer empty_computer;
        empty_computer = new ChecksumComputer();
        final ByteBuffer reply_data = ByteBuffer.wrap(packet.getICMPReplyData());
        icmp_header.position(0);
        empty_computer.moreData(icmp_header).moreData(reply_data);
        icmp_header.position(0);
        reply_data.position(0);
        icmp_header.putShort(2, (short) empty_computer.get());
        for (int i = 0; i < bytes.length; i++) {
            int offset = offsets[i];
            // TODO вынести
            if (icmp_header.position() < icmp_header.limit()) {
                final int copied = Integer.min(icmp_header.remaining(), bytes[i].length - offset);
                icmp_header.get(bytes[i], offset, copied);
                offset += copied;
            }
            if (reply_data.position() < reply_data.limit()) {
                final int copied = Integer.min(reply_data.remaining(), bytes[i].length - offset);
                reply_data.get(bytes[i], offset, copied);
                offset += copied;
            }
        }
    }
}
