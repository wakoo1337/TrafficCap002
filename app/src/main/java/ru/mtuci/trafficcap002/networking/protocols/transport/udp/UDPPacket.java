package ru.mtuci.trafficcap002.networking.protocols.transport.udp;

import java.nio.ByteBuffer;

import ru.mtuci.trafficcap002.networking.ChecksumComputer;
import ru.mtuci.trafficcap002.networking.protocols.ip.IPPacket;
import ru.mtuci.trafficcap002.networking.protocols.transport.BadDatagramException;
import ru.mtuci.trafficcap002.networking.protocols.transport.Datagram;

public final class UDPPacket implements Datagram {
    private final IPPacket parent;
    private final int src_port, dst_port;
    private final ByteBuffer payload;

    private UDPPacket(IPPacket parent, int src_port, int dst_port, ByteBuffer payload) {
        this.parent = parent;
        this.src_port = src_port;
        this.dst_port = dst_port;
        this.payload = payload;
    }

    public static UDPPacket of(IPPacket parent) throws BadDatagramException {
        final ByteBuffer datagram;
        datagram = parent.getDatagram();
        if (datagram.limit() < 8)
            throw new BadDatagramException("Слишком короткая датаграмма");
        datagram.position(0);
        final int src_port = Short.toUnsignedInt(datagram.getShort());
        final int dst_port = Short.toUnsignedInt(datagram.getShort());
        final int length = Short.toUnsignedInt(datagram.getShort());
        if (length != datagram.limit())
            throw new BadDatagramException("Длина датаграммы не совпадает с реальной");
        if (length < 8)
            throw new BadDatagramException("Слишком короткая датаграмма");
        final int transmitted_checksum = Short.toUnsignedInt(datagram.getShort());
        datagram.putShort(6, (short) 0); // Обнуляем значение чексуммы для её вычисления и сравнения
        final ByteBuffer payload = datagram.slice();
        datagram.position(0);
        if (transmitted_checksum != 0) {
            final ChecksumComputer cc;
            cc = new ChecksumComputer();
            cc.moreData(parent.getChecksumPseudoHeader()).moreData(datagram);
            int sum = cc.get();
            if (sum == 0)
                sum = 65535;
            if (sum != transmitted_checksum)
                throw new BadDatagramException("Контрольная сумма датаграммы присутствует, но неверна");
        }
        return new UDPPacket(parent, src_port, dst_port, payload);
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

    @Override
    public IPPacket getParent() {
        return parent;
    }
}
