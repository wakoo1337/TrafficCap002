package com.wakoo.trafficcap002.networking.protocols.transport.tcp;

import com.wakoo.trafficcap002.networking.ChecksumComputer;
import com.wakoo.trafficcap002.networking.protocols.transport.BadDatagramException;
import com.wakoo.trafficcap002.networking.protocols.transport.Datagram;
import com.wakoo.trafficcap002.networking.protocols.ip.IPPacket;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class TCPPacket implements IPPacket, Datagram {
    public static final int POS_URG = 0;
    public static final int POS_ACK = 1;
    public static final int POS_PSH = 2;
    public static final int POS_RST = 3;
    public static final int POS_SYN = 4;
    public static final int POS_FIN = 5;

    private final IPPacket parent;
    private final int src_port, dst_port;
    private final int seq, ack;
    private final boolean[] flags;
    private final int window;
    private final int urgent;
    private final ByteBuffer payload;
    private final boolean mss_present, scale_present;
    private final int mss, scale;

    private TCPPacket(IPPacket parent, int src_port, int dst_port, int seq, int ack, boolean[] flags, int window, int urgent, ByteBuffer payload, boolean mss_present, int mss, boolean scale_present, int scale) {
        this.parent = parent;
        this.src_port = src_port;
        this.dst_port = dst_port;
        this.seq = seq;
        this.ack = ack;
        this.flags = flags;
        this.window = window;
        this.urgent = urgent;
        this.payload = payload;
        this.mss_present = mss_present;
        this.mss = mss;
        this.scale_present = scale_present;
        this.scale = scale;
    }

    private static final byte OPTION_END = 0;
    private static final byte OPTION_NOOP = 1;
    private static final byte OPTION_MSS = 2;
    private static final byte OPTION_WINSCALE = 3;

    public static TCPPacket of(IPPacket parent) throws BadDatagramException {
        try {
            final ByteBuffer datagram = parent.getDatagram();
            final int src_port;
            src_port = Short.toUnsignedInt(datagram.getShort(0));
            final int dst_port;
            dst_port = Short.toUnsignedInt(datagram.getShort(2));
            final int seq;
            seq = datagram.getInt(4);
            final int ack;
            ack = datagram.getInt(8);
            final int data_offset;
            data_offset = Byte.toUnsignedInt(datagram.get(12)) >>> 4;
            boolean[] flags = new boolean[6];
            flags[0] = ((datagram.getShort(12) & 32) != 0);
            flags[1] = ((datagram.getShort(12) & 16) != 0);
            flags[2] = ((datagram.getShort(12) & 8) != 0);
            flags[3] = ((datagram.getShort(12) & 4) != 0);
            flags[4] = ((datagram.getShort(12) & 2) != 0);
            flags[5] = ((datagram.getShort(12) & 1) != 0);
            final int window;
            window = Short.toUnsignedInt(datagram.getShort(14));
            final int checksum;
            checksum = Short.toUnsignedInt(datagram.getShort(16));
            final int urgent;
            urgent = Short.toUnsignedInt(datagram.getShort(18));
            ChecksumComputer cc = new ChecksumComputer();
            cc.moreData(parent.getChecksumPseudoHeader());
            datagram.position(0);
            cc.moreData(datagram);
            if (cc.get() != 0)
                throw new BadDatagramException("Неверная контрольная сумма");
            final ByteBuffer payload;
            payload = ((ByteBuffer) datagram.position(4 * data_offset)).slice();
            datagram.position(20);
            boolean end_not_reached = true;
            boolean mss_present = false;
            int mss = (parent.getDestinationAddress() instanceof Inet6Address) ? 1280 : 576;
            boolean scale_present = false;
            int scale = 0;
            while (end_not_reached && (datagram.position() < (4 * data_offset))) {
                final byte kind;
                kind = datagram.get();
                switch (kind) {
                    case OPTION_END:
                        end_not_reached = false;
                    case OPTION_NOOP:
                        break;
                    case OPTION_MSS:
                        if (!mss_present) {
                            final int mss_len;
                            mss_len = Byte.toUnsignedInt(datagram.get());
                            checkOptionAllowed(datagram.position(), data_offset, mss_len, 4);
                            mss_present = true;
                            mss = Short.toUnsignedInt(datagram.getShort());
                        } else
                            throw new BadDatagramException("Опция MSS встречается более 1 раза");
                        break;
                    case OPTION_WINSCALE:
                        if (!scale_present) {
                            final int scale_len;
                            scale_len = Byte.toUnsignedInt(datagram.get());
                            checkOptionAllowed(datagram.position(), data_offset, scale_len, 3);
                            scale_present = true;
                            scale = Byte.toUnsignedInt(datagram.get());
                        } else
                            throw new BadDatagramException("Опция масштабирования окна встречается более 1 раза");
                        break;
                    default:
                        final int default_new_pos = datagram.position() + Byte.toUnsignedInt(datagram.get()) - 1;
                        if (default_new_pos < (4 * data_offset))
                            datagram.position(default_new_pos);
                        else
                            throw new BadDatagramException("Опция не влезает в заголовок");
                }
            }
            datagram.position(0);
            return new TCPPacket(parent, src_port, dst_port, seq, ack, flags, window, urgent, payload, mss_present, mss, scale_present, scale);
        } catch (
                IndexOutOfBoundsException indexexcp) {
            throw new BadDatagramException("Пакет не содержит необходимых данных", indexexcp);
        } catch (
                BufferUnderflowException underflow) {
            throw new BadDatagramException("В буфере кончились данные", underflow);
        }
    }

    private static void checkOptionAllowed(final int pos, final int data_offset, final int len, final int len_waited) throws BadDatagramException {
        if ((len != len_waited) || ((pos + len - 2) > 4 * data_offset))
            throw new BadDatagramException("Опция выходит за пределы заголовка или неверного размера");
    }

    @Override
    public InetAddress getSourceAddress() {
        return parent.getSourceAddress();
    }

    @Override
    public InetAddress getDestinationAddress() {
        return parent.getDestinationAddress();
    }

    @Override
    public int getProtocol() {
        return parent.getProtocol();
    }

    @Override
    public ByteBuffer getDatagram() {
        return parent.getDatagram();
    }

    @Override
    public ByteBuffer getChecksumPseudoHeader() {
        return parent.getChecksumPseudoHeader();
    }

    @Override
    public ByteBuffer getPayload() {
        return payload;
    }

    @Override
    public int getSourcePort() {
        return src_port;
    }

    @Override
    public int getDestinationPort() {
        return dst_port;
    }

    public int getSeq() {
        return seq;
    }

    public int getAck() {
        return ack;
    }

    public boolean[] getFlags() {
        return Arrays.copyOf(flags, flags.length);
    }

    public int getWindow() {
        return window;
    }

    public int getUrgent() {
        return urgent;
    }

    public boolean getMSSPresent() {
        return mss_present;
    }

    public int getMSS() {
        return mss;
    }

    public boolean getScalePresent() {
        return scale_present;
    }

    public int getScale() {
        return scale;
    }
}
