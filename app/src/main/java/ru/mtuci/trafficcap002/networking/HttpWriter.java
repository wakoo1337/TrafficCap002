package ru.mtuci.trafficcap002.networking;

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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ru.mtuci.trafficcap002.labels.Category;

public final class HttpWriter {
    private final List<Category> categories;
    private final String site_root;
    private final ExecutorService pool;

    public HttpWriter(List<Category> categories, String site_root) {
        this.categories = categories;
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
            for (Category c : categories) {
                if (c.getEnabled()) {
                    sb.append(c.getName()).append("=").append(c.getLabels().get(c.getIndex()).getName());
                }
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
                        connection.setRequestMethod("POST");
                        connection.setRequestProperty("Content-Type", "application/octet-stream");
                        connection.setDoOutput(true);
                        connection.setDoInput(false);
                        connection.setFixedLengthStreamingMode(data.limit());
                        try (final OutputStream out_stream = connection.getOutputStream()) {
                            out_stream.write(data.array(), data.arrayOffset(), data.limit());
                            out_stream.flush();
                        } catch (
                                IOException ioexcp) {
                            Log.e("Сброс пакетов на сервер", "Ошибка ввода-вывода", ioexcp);
                        } finally {
                            final int response = connection.getResponseCode();
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
            Log.e("Сброс пакетов на сервер", "Неверный формат адреса", malformedurl);
        } catch (
                UnsupportedEncodingException unsupportedEncodingException) {
            throw new RuntimeException(unsupportedEncodingException);
        }
    }
}
