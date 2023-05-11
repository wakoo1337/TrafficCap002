package ru.mtuci.trafficcap002.networking;

import android.content.Context;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public final class PcapWriter implements AutoCloseable {
    private final FileOutputStream out_stream;
    private final Instant beginning;

    public PcapWriter(Context ctx, String name) throws IOException {
        out_stream = ctx.openFileOutput(name, Context.MODE_PRIVATE);
        out_stream.write(new byte[]{
                -95, -78, -61, -44,
                0, 2,
                0, 4,
                0, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, -1, -1,
                0, 0, 0, 101});
        beginning = LocalDateTime.now().toInstant(ZoneOffset.UTC);
    }

    public void writePacket(byte[] packet, int len) throws IOException {
        final Instant now = LocalDateTime.now().toInstant(ZoneOffset.UTC);
        final Duration delta = Duration.between(beginning, now);
        final ByteBuffer header = ByteBuffer.allocate(16);
        header.putInt((int) delta.getSeconds());
        header.putInt(delta.getNano() / 1000);
        header.putInt(len);
        header.putInt(len);
        header.flip();
        out_stream.write(header.array());
        out_stream.write(packet, 0, len);
    }

    @Override
    public void close() throws Exception {
        out_stream.close();
    }
}
