package com.wakoo.trafficcap002.networking.protocols.ip.ipv4;

import static com.wakoo.trafficcap002.networking.protocols.DatagramConsumer.PROTOCOL_TCP;
import static com.wakoo.trafficcap002.networking.protocols.DatagramConsumer.PROTOCOL_UDP;

import android.util.Log;

import com.wakoo.trafficcap002.networking.protocols.ip.BadIPPacketException;
import com.wakoo.trafficcap002.networking.protocols.tcp.TCPDatagramConsumer;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.util.function.Consumer;

public class IPv4BufferConsumer implements Consumer<ByteBuffer> {
    private final FileOutputStream out;
    private final Selector sel;
    private final TCPDatagramConsumer tcp;

    public IPv4BufferConsumer(Selector sel, FileDescriptor fd, TCPDatagramConsumer tcp) {
        this.out = new FileOutputStream(fd);
        this.sel = sel;
        this.tcp = tcp;
    }

    @Override
    public void accept(ByteBuffer byteBuffer) {
        try {
            final IPv4Packet packet = IPv4Packet.of(byteBuffer);
            // TODO сделать восстановление после фрагментации
            switch (packet.getProtocol()) {
                case PROTOCOL_TCP:
                    tcp.accept(packet);
                    break;
                case PROTOCOL_UDP:
                    break;
            }
        } catch (
                BadIPPacketException badpacket) {
            Log.e("Захват пакетов", "Плохой IP-пакет", badpacket);
        }
    }
}
