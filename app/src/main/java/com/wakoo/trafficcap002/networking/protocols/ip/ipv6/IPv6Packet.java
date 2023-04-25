package com.wakoo.trafficcap002.networking.protocols.ip.ipv6;

import static com.wakoo.trafficcap002.networking.protocols.transport.DatagramConsumer.PROTOCOL_ICMPv6;
import static com.wakoo.trafficcap002.networking.protocols.transport.DatagramConsumer.PROTOCOL_TCP;
import static com.wakoo.trafficcap002.networking.protocols.transport.DatagramConsumer.PROTOCOL_UDP;

import com.wakoo.trafficcap002.networking.protocols.ip.BadIPPacketException;
import com.wakoo.trafficcap002.networking.protocols.ip.IPPacket;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

public class IPv6Packet implements IPPacket {
    public static final int HEADER_FRAGMENT=44;

    private final InetAddress src, dst;
    private final int proto;
    private final ByteBuffer header;
    private final ByteBuffer datagram;

    private IPv6Packet(InetAddress src, InetAddress dst, int proto, ByteBuffer header, ByteBuffer datagram) {
        this.src = src;
        this.dst = dst;
        this.proto = proto;
        this.header = header;
        this.datagram = datagram;
    }

    public static IPv6Packet of(ByteBuffer packet) throws BadIPPacketException {
        assert (Byte.toUnsignedInt(packet.get(0)) >>> 4) == PROTOCOL_IPv6;
        try {
            packet.position(4);
            final int payload_length = Short.toUnsignedInt(packet.getShort());
            int next_header = Byte.toUnsignedInt(packet.get());
            final int ttl = Byte.toUnsignedInt(packet.get());
            final byte[] src = new byte[16];
            packet.get(src);
            final byte[] dst = new byte[16];
            packet.get(dst);
            final InetAddress src_addr;
            src_addr = InetAddress.getByAddress(src);
            final InetAddress dst_addr;
            dst_addr = InetAddress.getByAddress(dst);
            int offset = 40;
            do
            {
                final int current_header = next_header;
                if ((current_header == PROTOCOL_TCP) || (current_header == PROTOCOL_UDP) || (current_header == PROTOCOL_ICMPv6)) {
                    final ByteBuffer payload = (ByteBuffer) ((ByteBuffer) packet.position(offset)).slice().limit(payload_length - offset + 40);
                    final ByteBuffer header = (ByteBuffer) packet.position(0).limit(offset);
                    return new IPv6Packet(src_addr, dst_addr, current_header, header, payload);
                } else if (current_header == HEADER_FRAGMENT) {
                    // TODO вытащить информацию о фрагментации
                    next_header = Byte.toUnsignedInt(packet.get());
                    offset += 7;
                    packet.position(offset);
                } else {
                    next_header = Byte.toUnsignedInt(packet.get());
                    if (next_header == 59)
                        return null;
                    final int hdr_len = Byte.toUnsignedInt(packet.get());
                    offset += hdr_len + 2;
                    packet.position(offset);
                }
            } while (offset + 2 < packet.limit());
        } catch (
                BufferUnderflowException underflow) {
            throw new BadIPPacketException("Пакет неожиданно закончился", underflow);
        } catch (
                IllegalArgumentException illarg) {
            throw new BadIPPacketException("Невозможно установить лимит", illarg);
        } catch (
                UnknownHostException uhost) {
            throw new RuntimeException(uhost);
        }
        return null;
    }

    @Override
    public InetAddress getSourceAddress() {
        return src;
    }

    @Override
    public InetAddress getDestinationAddress() {
        return dst;
    }

    @Override
    public int getProtocol() {
        return proto;
    }

    @Override
    public ByteBuffer getDatagram() {
        return datagram;
    }

    @Override
    public ByteBuffer getChecksumPseudoHeader() {
        final ByteBuffer hdr = ByteBuffer.allocate(40);
        hdr.put(src.getAddress());
        hdr.put(dst.getAddress());
        hdr.putInt(datagram.limit());
        hdr.putInt(proto);
        hdr.position(0);
        return hdr;
    }

    @Override
    public byte[] getICMPReplyData() {
        final byte[] reply_bytes;
        reply_bytes = new byte[header.limit() + 8];
        header.position(0);
        header.get(reply_bytes, 0, header.limit());
        datagram.position(0);
        datagram.get(reply_bytes, header.limit(), 8);
        datagram.position(0);
        return reply_bytes;
    }
}
