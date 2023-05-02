package com.wakoo.trafficcap002.networking.threads;

import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.wakoo.trafficcap002.CaptureService;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public final class DescriptorListener implements Runnable {
    public static final int INTERFACE_MTU = 8192;
    private final FileDescriptor fd;
    private final SocketsListener sock_listener;
    private final CaptureService cap_svc;

    public DescriptorListener(CaptureService cap_svc, ParcelFileDescriptor pfd, SocketsListener sock_listener) {
        this.cap_svc = cap_svc;
        this.fd = pfd.getFileDescriptor();
        this.sock_listener = sock_listener;
    }

    @Override
    public void run() {
        try (FileInputStream in_stream = new FileInputStream(fd)) {
            while (!Thread.currentThread().isInterrupted()) {
                final byte[] bytes = new byte[INTERFACE_MTU];
                final int readed = in_stream.read(bytes);
                final ByteBuffer bytes_buffer = (ByteBuffer) ByteBuffer.wrap(bytes).limit(readed);
                sock_listener.feedPacket(bytes_buffer);
            }
        } catch (
                IOException ioexcp) {
            Log.e("Прослушивание дескриптора", "Невозможно прочитать дескриптор", ioexcp);
        }
    }
}
