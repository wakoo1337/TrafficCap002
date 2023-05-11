package ru.mtuci.trafficcap002.networking.protocols.ip;

import java.net.InetAddress;
import java.nio.ByteBuffer;

public interface IPPacket {
    int PROTOCOL_IPv4 = 4;
    int PROTOCOL_IPv6 = 6;

    InetAddress getSourceAddress();

    InetAddress getDestinationAddress();

    int getProtocol();

    byte[] getICMPReplyData();

    ByteBuffer getDatagram();

    ByteBuffer getChecksumPseudoHeader();
    // Здесь должна быть ещё информация о сегментации
}