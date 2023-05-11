package ru.mtuci.trafficcap002;

import android.content.Intent;
import android.net.VpnService;
import android.os.Binder;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import ru.mtuci.trafficcap002.networking.HttpWriter;
import ru.mtuci.trafficcap002.networking.threads.DescriptorListener;
import ru.mtuci.trafficcap002.networking.threads.SocketsListener;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;

public final class CaptureService extends VpnService {
    public static final String APP_TO_LISTEN = "ru.mtuci.trafficcap002.CaptureService.listenapp";
    public static final String SITE_TO_WRITE = "ru.mtuci.trafficcap002.CaptureService.writesite";

    private String site;
    private ParcelFileDescriptor pfd;
    private SocketsListener sock_listener;
    private DescriptorListener desc_listsner;
    private Thread sock_thread, desc_thread;
    private HttpWriter http_writer;

    public static InetAddress getLocalInet4() {
        try {
            return InetAddress.getByAddress(new byte[]{-64, 88, 99, 3});
        } catch (
                UnknownHostException uhost) {
            return null;
        }
    }

    public static InetAddress getLocalInet6() throws UnknownHostException {
        try {
            return InetAddress.getByAddress(new byte[]{-2, -128, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3});
        } catch (
                UnknownHostException uhost) {
            return null;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (SERVICE_INTERFACE.equals(intent.getAction()))
            return super.onBind(intent);
        else {
            Builder builder = new Builder();
            try {
                final String listen_it = intent.getStringExtra(APP_TO_LISTEN);
                if (listen_it != null) {
                    builder.addAllowedApplication(listen_it);
                } else {
                    builder.addDisallowedApplication(getPackageName());
                }
                site = intent.getStringExtra(SITE_TO_WRITE);
                builder.addAddress(getLocalInet4(), 32)
                        .addRoute(InetAddress.getByAddress(new byte[]{0, 0, 0, 0}), 0)
                        .addAddress(getLocalInet6(), 128)
                        .addRoute(InetAddress.getByAddress(new byte[16]), 0)
                        .setBlocking(true)
                        .setMtu(DescriptorListener.INTERFACE_MTU);
                pfd = builder.establish();
                sock_listener = new SocketsListener(this, pfd);
                desc_listsner = new DescriptorListener(this, pfd, sock_listener);
                sock_thread = new Thread(sock_listener);
                desc_thread = new Thread(desc_listsner);
                sock_thread.start();
                desc_thread.start();
                return new CaptureServiceBinder();
            } catch (
                    Exception e) {
                Log.e("Запуск службы", "Невозможно запустить службу", e);
                stopSelf();
                return null;
            }
        }
    }

    private void stopThread(Thread thread) {
        boolean running = true;
        thread.interrupt();
        while (running) {
            try {
                thread.join();
                running = false;
            } catch (
                    InterruptedException ignored) {
            }
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        stopThread(sock_thread);
        stopThread(desc_thread);
        try {
            if (pfd != null)
                pfd.close();
        } catch (
                IOException ioexcp) {
            Log.e("Остановка службы", "Невозможно закрыть дескриптор", ioexcp);
        }
        return false;
    }

    public class CaptureServiceBinder extends Binder {
        public void setActiveLabelsSet(Set<String> active) {
            if (site != null) {
                http_writer = new HttpWriter(active, site);
                sock_listener.setHttpWriter(http_writer);
            }
        }
    }
}
