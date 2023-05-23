package ru.mtuci.trafficcap002.networking.protocols.ip.ipv6;

import static ru.mtuci.trafficcap002.networking.protocols.transport.DatagramConsumer.PROTOCOL_TCP;
import static ru.mtuci.trafficcap002.networking.protocols.transport.DatagramConsumer.PROTOCOL_UDP;

import android.util.Log;

import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.util.function.Consumer;

import ru.mtuci.trafficcap002.networking.protocols.ip.BadIPPacketException;
import ru.mtuci.trafficcap002.networking.protocols.transport.tcp.TCPDatagramConsumer;
import ru.mtuci.trafficcap002.networking.protocols.transport.udp.UDPDatagramConsumer;

public final class IPv6BufferConsumer implements Consumer<ByteBuffer> {
    private final FileOutputStream out;
    private final Selector sel;
    private final TCPDatagramConsumer tcp;
    private final UDPDatagramConsumer udp;

    public IPv6BufferConsumer(Selector sel, FileOutputStream out, TCPDatagramConsumer tcp, UDPDatagramConsumer udp) {
        this.out = out;
        this.sel = sel;
        this.tcp = tcp;
        this.udp = udp;
    }

    @Override
    public void accept(ByteBuffer byteBuffer) {
        try {
            final IPv6Packet packet = IPv6Packet.of(byteBuffer);
            // TODO сделать восстановление после фрагментации
            switch (packet.getProtocol()) {
                case PROTOCOL_TCP:
                    tcp.accept(packet);
                    break;
                case PROTOCOL_UDP:
                    udp.accept(packet);
                    break;
            }
        } catch (
                BadIPPacketException badpacket) {
            Log.e("Захват пакетов", "Плохой IP-пакет", badpacket);
        }
    }
}
