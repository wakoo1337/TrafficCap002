package ru.mtuci.trafficcap002.networking.protocols.transport.tcp;

import java.net.Inet6Address;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import ru.mtuci.trafficcap002.networking.ChecksumComputer;
import ru.mtuci.trafficcap002.networking.protocols.ip.IPPacket;
import ru.mtuci.trafficcap002.networking.protocols.transport.BadDatagramException;
import ru.mtuci.trafficcap002.networking.protocols.transport.Datagram;

public final class TCPPacket implements Datagram {
    public static final int POS_URG = 0;
    public static final int POS_ACK = 1;
    public static final int POS_PSH = 2;
    public static final int POS_RST = 3;
    public static final int POS_SYN = 4;
    public static final int POS_FIN = 5;
    private static final byte OPTION_END = 0;
    private static final byte OPTION_NOOP = 1;
    private static final byte OPTION_MSS = 2;
    private static final byte OPTION_WINSCALE = 3;
    private final IPPacket parent;
    private final int src_port, dst_port;
    private final int seq, ack;
    private final boolean[] flags;
    private final int window;
    private final int urgent;
    private final ByteBuffer payload, urgent_payload;
    private final TCPOption mss, scale;

    private TCPPacket(IPPacket parent, int src_port, int dst_port, int seq, int ack, boolean[] flags, int window, int urgent, ByteBuffer payload, ByteBuffer urgent_payload, TCPOption mss, TCPOption scale) {
        this.parent = parent;
        this.src_port = src_port;
        this.dst_port = dst_port;
        this.seq = seq;
        this.ack = ack;
        this.flags = flags;
        this.window = window;
        this.urgent = urgent;
        this.payload = payload;
        this.urgent_payload = urgent_payload;
        this.mss = mss;
        this.scale = scale;
    }

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
            payload = ((ByteBuffer) datagram.position((4 * data_offset) + (flags[POS_URG] ? urgent : 0))).slice();
            final ByteBuffer urgent_payload;
            urgent_payload = flags[POS_URG] ? (ByteBuffer) ((ByteBuffer) datagram.position(4 * data_offset)).slice().limit(urgent) : ByteBuffer.allocate(0);
            datagram.position(20);
            boolean end_not_reached = true;
            TCPOption mss = new TCPOption((parent.getDestinationAddress() instanceof Inet6Address) ? 1220 : 536, false);
            TCPOption scale = new TCPOption(0, false);
            while (end_not_reached && (datagram.position() < (4 * data_offset))) {
                final byte kind;
                kind = datagram.get();
                switch (kind) {
                    case OPTION_END:
                        end_not_reached = false;
                    case OPTION_NOOP:
                        break;
                    case OPTION_MSS:
                        if (!mss.getPresence()) {
                            final int mss_len;
                            mss_len = Byte.toUnsignedInt(datagram.get());
                            checkOptionAllowed(datagram.position(), data_offset, mss_len, 4);
                            mss = new TCPOption(Short.toUnsignedInt(datagram.getShort()), true);
                        } else
                            throw new BadDatagramException("Опция MSS встречается более 1 раза");
                        break;
                    case OPTION_WINSCALE:
                        if (!scale.getPresence()) {
                            final int scale_len;
                            scale_len = Byte.toUnsignedInt(datagram.get());
                            checkOptionAllowed(datagram.position(), data_offset, scale_len, 3);
                            scale = new TCPOption(Byte.toUnsignedInt(datagram.get()), true);
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
            return new TCPPacket(parent, src_port, dst_port, seq, ack, flags, window, urgent, payload, urgent_payload, mss, scale);
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
    public ByteBuffer getPayload() {
        return payload;
    }

    public ByteBuffer getUrgentPayload() {
        return urgent_payload;
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

    public int getWindow(int scale) {
        return window << scale;
    }

    public int getUrgent() {
        return urgent;
    }

    public TCPOption getMSS() {
        return mss;
    }

    public TCPOption getScale() {
        return scale;
    }

    @Override
    public IPPacket getParent() {
        return parent;
    }
}
