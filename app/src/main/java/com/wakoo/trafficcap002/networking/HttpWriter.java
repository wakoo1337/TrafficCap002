package com.wakoo.trafficcap002.networking;

import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpWriter {
    private final Set<String> active;
    private final String site_root;
    private final ExecutorService pool;

    public HttpWriter(Set<String> active, String site_root) {
        this.active = active;
        this.site_root = site_root;
        this.pool = Executors.newSingleThreadExecutor();
    }

    public void send(ByteBuffer data, InetAddress src, InetAddress dst, int src_port, int dst_port, String protocol) {
        try {
            final StringBuilder sb = new StringBuilder(site_root);
            sb.append("/packet?src=").append(URLEncoder.encode(src.toString(), "utf-8"))
                    .append("&dst=").append(URLEncoder.encode(dst.toString(), "utf-8"))
                    .append("&src_port=").append(src_port)
                    .append("&dst_port=").append(dst_port)
                    .append("&protocol=").append(protocol);
            if (!active.isEmpty()) {
                sb.append("&labels=");
                for (String label : active)
                    sb.append(label).append(",");
                sb.deleteCharAt(sb.length() - 1);
            }
            final String path = sb.toString();
            final URL url;
            url = new URL(path);
            pool.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        final HttpURLConnection connection;
                        connection = (HttpURLConnection) url.openConnection();
                        try {
                            connection.setRequestMethod("POST");
                            connection.setRequestProperty("Content-Type", "application/octet-stream");
                            connection.setInstanceFollowRedirects(false);
                            connection.setDoOutput(true);
                            connection.setDoInput(false);
                            connection.setFixedLengthStreamingMode(data.limit());
                            connection.connect();
                            final OutputStream out_stream = connection.getOutputStream();
                            out_stream.write(data.array(), data.arrayOffset(), data.limit());
                            out_stream.close();
                        } catch (
                                IOException ioexcp) {
                            Log.e("Сброс пакетов на сервер", "Ошибка ввода-вывода", ioexcp);
                        } finally {
                                connection.disconnect();
                        }
                    } catch (
                            IOException ioexcp) {
                        Log.e("Сброс пакетов на сервер", "Невозможно открыть соединение", ioexcp);
                    }
                }
            });
        } catch (
                MalformedURLException malformedurl) {
        } catch (
                UnsupportedEncodingException unsupportedEncodingException) {
            throw new RuntimeException(unsupportedEncodingException);
        }
    }
}
