package com.wakoo.trafficcap002.networking;

import android.content.Context;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class PcapWriter implements AutoCloseable {
    final FileOutputStream out_stream;

    public PcapWriter(Context ctx, String name) throws IOException {
        out_stream = ctx.openFileOutput(name, Context.MODE_PRIVATE);
        out_stream.write(new byte[]{
                (byte) 0xa1, (byte) 0xb2, (byte) 0xc3, (byte) 0xd4,
                0, 2,
                0, 4,
                0, 0, 0, 0,
                0, 0, 0, 0,
                0, 2, 0, 0,
                0, 0, 0, 101});
    }

    public void writePacket(byte[] packet, int len) throws IOException {
        final LocalDateTime localdt = LocalDateTime.now();
        ByteBuffer header = ByteBuffer.allocate(16);
        header.putInt((int) localdt.toEpochSecond(ZoneOffset.UTC));
        header.putInt(localdt.getNano() / 1000);
        for (int a : new int[]{1, 2})
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
