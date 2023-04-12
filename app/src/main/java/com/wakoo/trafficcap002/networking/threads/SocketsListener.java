package com.wakoo.trafficcap002.networking.threads;

import static com.wakoo.trafficcap002.networking.protocols.ip.IPPacket.PROTOCOL_IPv4;
import static com.wakoo.trafficcap002.networking.protocols.ip.IPPacket.PROTOCOL_IPv6;

import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.wakoo.trafficcap002.CaptureService;
import com.wakoo.trafficcap002.networking.protocols.ip.IPPacket;
import com.wakoo.trafficcap002.networking.protocols.ip.BadIPPacketException;
import com.wakoo.trafficcap002.networking.protocols.ip.ipv4.IPv4BufferConsumer;
import com.wakoo.trafficcap002.networking.protocols.transport.tcp.TCPDatagramConsumer;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SocketsListener implements Runnable {
    private final FileDescriptor fd;
    private final ConcurrentLinkedQueue<ByteBuffer> packets_queue;
    private final CaptureService cap_svc;
    private Selector selector;

    public SocketsListener(CaptureService cap_svc, ParcelFileDescriptor pfd) {
        this.cap_svc = cap_svc;
        fd = pfd.getFileDescriptor();
        packets_queue = new ConcurrentLinkedQueue<>();
    }

    @Override
    public void run() {
        try (Selector selector = Selector.open()) {
            this.selector = selector;
            final FileOutputStream out = new FileOutputStream(fd);
            final TCPDatagramConsumer tcp = new TCPDatagramConsumer();
            final IPv4BufferConsumer ipv4_consumer = new IPv4BufferConsumer(selector, out, tcp);
            while (!Thread.currentThread().isInterrupted()) {
                selector.select();
                while (!packets_queue.isEmpty()) {
                    final ByteBuffer packet_buffer;
                    packet_buffer = packets_queue.poll();
                    final int protocol = Byte.toUnsignedInt(packet_buffer.get(0)) >>> 4;
                    switch (protocol) {
                        case PROTOCOL_IPv4:
                            ipv4_consumer.accept(packet_buffer);
                            break;
                        case PROTOCOL_IPv6:

                            break;
                    }
                }
            }
        } catch (
                IOException ioexcp) {
            Log.e("Прослушивание сокетов", "Исключение селектора", ioexcp);
            cap_svc.stopSelf();
        }
    }

    public void feedPacket(ByteBuffer bb) {
        packets_queue.add(bb);
        if (selector != null)
            selector.wakeup();
    }

    private interface PacketProcessorFactory {
        IPPacket make(ByteBuffer packet) throws BadIPPacketException;
    }
}
