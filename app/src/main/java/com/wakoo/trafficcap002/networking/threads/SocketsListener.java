package com.wakoo.trafficcap002.networking.threads;

import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.wakoo.trafficcap002.CaptureService;

import java.io.FileDescriptor;
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
            while (!Thread.currentThread().isInterrupted()) {
                selector.select();
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
}
