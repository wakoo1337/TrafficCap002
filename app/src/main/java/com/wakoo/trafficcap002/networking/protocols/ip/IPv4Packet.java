package com.wakoo.trafficcap002.networking.protocols.ip;

import android.util.Log;

import com.wakoo.trafficcap002.networking.ChecksumComputer;
import com.wakoo.trafficcap002.networking.protocols.Packet;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class IPv4Packet implements Packet {
    private final InetAddress source, destination;
    private final int protocol;
    private final ByteBuffer payload_buffer;

    public IPv4Packet(InetAddress src, InetAddress dst, int proto, ByteBuffer payload) {
        source = src;
        destination = dst;
        protocol = proto;
        payload_buffer = payload;
    }

    public static IPv4Packet of(ByteBuffer packet) throws BadIPPacketException {
        assert (Byte.toUnsignedInt(packet.get(0)) >>> 4) == Packet.PROTOCOL_IPv4;
        final int hdr_len;
        hdr_len = ((int) packet.get(0)) & 15;
        if (hdr_len < 5) throw new BadIPPacketException("Длина заголовка слишком мала");
        try {
            final int total_len;
            total_len = Short.toUnsignedInt(packet.getShort(2));
            final int flags;
            flags = Short.toUnsignedInt(packet.getShort(6)) & 57344;
            final boolean flag_df = (flags & 16384) != 0;
            final boolean flag_mf = (flags &  8192) != 0;
            if (flag_mf) throw new BadIPPacketException("Фрагментация пакетов IP не поддерживается (и не будет)");
            final int proto;
            proto = Short.toUnsignedInt(packet.getShort(8)) & 0xFF;
            final int header_cs;
            header_cs = Short.toUnsignedInt(packet.getShort(10));
            final ByteBuffer header = (ByteBuffer) packet.duplicate().limit(hdr_len * 4);
            ChecksumComputer cc = new ChecksumComputer();
            cc.moreData(header);
            if (cc.get() != 0) throw new BadIPPacketException("Неверная контрольная сумма заголовка IP");
            packet.position(12);
            byte[] src = new byte[4];
            byte[] dst = new byte[4];
            packet.get(src);
            packet.get(dst);
            return new IPv4Packet(Inet4Address.getByAddress(src), Inet4Address.getByAddress(dst), proto,
                    (ByteBuffer) ((ByteBuffer) packet.position(hdr_len * 4)).slice().limit(total_len - hdr_len * 4));
        } catch (IndexOutOfBoundsException indexexcp) {
            throw new BadIPPacketException("Буфер не содержит требуемых данных", indexexcp);
        } catch (IllegalArgumentException illegalexcp) {
            throw new BadIPPacketException("Содержимое буфера с пакетом не соответствует ограничениям", illegalexcp);
        } catch (UnknownHostException uknownhostexcp) {
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
        return payload_buffer.duplicate();
    }
}
