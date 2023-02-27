package com.wakoo.trafficcap002;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Binder;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;

import java.net.Inet4Address;
import java.net.InetAddress;

public class CaptureService extends VpnService {
    public static final String APP_TO_LISTEN = "com.wakoo.trafficcap002.CaptureService.listenapp";

    private ParcelFileDescriptor pfd;

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
                builder.addAddress(Inet4Address.getByAddress(new byte[]{(byte) 192, 88, 99, 3}), 32)
                        .addRoute(Inet4Address.getByAddress(new byte[]{0, 0, 0, 0}), 0)
                        .setBlocking(true);
                pfd = builder.establish();
            } catch (Exception e) {
                stopSelf();
                return null;
            }
        }
        return new CaptureServiceBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return false;
    }

    public class CaptureServiceBinder extends Binder {

    }
}
