package com.wakoo.trafficcap002.networking.protocols.ip.ipv4;

import android.util.Log;

import com.wakoo.trafficcap002.networking.ChecksumComputer;
import com.wakoo.trafficcap002.networking.protocols.ip.BadIPPacketException;
import com.wakoo.trafficcap002.networking.protocols.ip.IPPacket;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class IPv4Packet implements IPPacket {
    private final InetAddress source, destination;
    private final int protocol;
    private final ByteBuffer datagram;

    private IPv4Packet(InetAddress src, InetAddress dst, int proto, ByteBuffer payload) {
        source = src;
        destination = dst;
        protocol = proto;
        datagram = payload;
    }

    public static IPv4Packet of(ByteBuffer packet) throws BadIPPacketException {
        assert (((int) packet.get(0)) >>> 4) == IPPacket.PROTOCOL_IPv4;
        final int hdr_len;
        hdr_len = ((int) packet.get(0)) & 15;
        if (hdr_len < 5)
            throw new BadIPPacketException("Длина заголовка слишком мала");
        try {
            final int total_len;
            total_len = Short.toUnsignedInt(packet.getShort(2));
            final int flags;
            flags = Short.toUnsignedInt(packet.getShort(6)) & 57344;
            final boolean flag_df = (flags & 16384) != 0;
            final boolean flag_mf = (flags & 8192) != 0;
            final int proto;
            proto = Short.toUnsignedInt(packet.getShort(8)) & 0xFF;
            final int header_cs;
            header_cs = Short.toUnsignedInt(packet.getShort(10));
            final ByteBuffer header = (ByteBuffer) packet.duplicate().limit(hdr_len * 4);
            ChecksumComputer cc = new ChecksumComputer();
            cc.moreData(header);
            if (cc.get() != 0)
                throw new BadIPPacketException("Неверная контрольная сумма заголовка IP");
            packet.position(12);
            byte[] src = new byte[4];
            byte[] dst = new byte[4];
            packet.get(src);
            packet.get(dst);
            return new IPv4Packet(InetAddress.getByAddress(src), InetAddress.getByAddress(dst), proto,
                    (ByteBuffer) ((ByteBuffer) packet.position(hdr_len * 4)).slice().limit(total_len - hdr_len * 4));
        } catch (
                IndexOutOfBoundsException indexexcp) {
            throw new BadIPPacketException("Буфер не содержит требуемых данных", indexexcp);
        } catch (
                IllegalArgumentException illegalexcp) {
            throw new BadIPPacketException("Содержимое буфера с пакетом не соответствует ограничениям", illegalexcp);
        } catch (
                UnknownHostException uknownhostexcp) {
            Log.e("Создание IPv4-пакета", "Указан несуществующий хост", uknownhostexcp);
            return null;
        }
    }

    @Override
    public InetAddress getSourceAddress() {
        return source;
    }

    @Override
    public InetAddress getDestinationAddress() {
        return destination;
    }

    @Override
    public int getProtocol() {
        return protocol;
    }

    @Override
    public ByteBuffer getDatagram() {
        return datagram.duplicate();
    }

    @Override
    public ByteBuffer getChecksumPseudoHeader() {
        final ByteBuffer buffer = ByteBuffer.allocate(12);
        buffer.put(source.getAddress());
        buffer.put(destination.getAddress());
        buffer.put(9, (byte) protocol);
        buffer.putShort(10, (short) datagram.limit());
        buffer.position(0);
        return buffer;
    }
}
