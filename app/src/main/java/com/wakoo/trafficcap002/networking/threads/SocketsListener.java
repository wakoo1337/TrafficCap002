package com.wakoo.trafficcap002.networking.threads;

import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.wakoo.trafficcap002.CaptureService;
import com.wakoo.trafficcap002.networking.protocols.Packet;
import com.wakoo.trafficcap002.networking.protocols.ip.BadIPPacketException;
import com.wakoo.trafficcap002.networking.protocols.ip.IPv4Packet;
import com.wakoo.trafficcap002.networking.protocols.ip.IPv6Packet;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SocketsListener implements Runnable {
    private final FileDescriptor fd;
    private final ConcurrentLinkedQueue<ByteBuffer> packets_queue;
    private final CaptureService cap_svc;
    private Selector selector;
    private static final Map<Integer, PacketProcessorFactory> processors;
    static {
        processors = new HashMap<>();
        processors.put(Packet.PROTOCOL_IPv4, new PacketProcessorFactory() {
            @Override
            public Packet make(ByteBuffer packet) throws BadIPPacketException {
                return IPv4Packet.of(packet);
            }
        });
        processors.put(Packet.PROTOCOL_IPv6, new PacketProcessorFactory() {
            @Override
            public Packet make(ByteBuffer packet) throws BadIPPacketException {
                return IPv6Packet.of(packet);
            }
        });
    }

    public SocketsListener(CaptureService cap_svc, ParcelFileDescriptor pfd) {
        this.cap_svc = cap_svc;
        fd = pfd.getFileDescriptor();
        packets_queue = new ConcurrentLinkedQueue<>();
    }

    @Override
    public void run() {
        try (Selector selector = Selector.open()) {
            this.selector = selector;
            while (!Thread.currentThread().isInterrupted()) {
                selector.select();
                while (!packets_queue.isEmpty()) {
                    final ByteBuffer packet_buffer;
                    packet_buffer = packets_queue.poll();
                    try {
                        final int protocol = Byte.toUnsignedInt(packet_buffer.get(0)) >>> 4;
                        final PacketProcessorFactory processor_factory;
                        processor_factory = processors.get(protocol);
                        if (processor_factory != null) try {
                            final Packet packet;
                            packet = processor_factory.make(packet_buffer);

                        } catch (BadIPPacketException badipexcp) {
                            Log.e("Разбор пакета сетевого уровня", "Неверный формат пакета", badipexcp);
                        }
                    } catch (IndexOutOfBoundsException indexexcp) {
                        Log.d("Разбор пакета сетевого уровня", "Пакет имеет длину менее одного байта", indexexcp);
                    }
                }
            }
        } catch (IOException ioexcp) {
            Log.e("Прослушивание сокетов", "Исключение селектора", ioexcp);
            cap_svc.stopSelf();
        }
    }

    public void feedPacket(ByteBuffer bb) {
        packets_queue.add(bb);
        if (selector != null) selector.wakeup();
    }

    private interface PacketProcessorFactory {
        Packet make(ByteBuffer packet) throws BadIPPacketException;
    }
}
