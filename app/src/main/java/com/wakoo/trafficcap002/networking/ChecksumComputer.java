package com.wakoo.trafficcap002.networking;

import java.nio.ByteBuffer;

public class ChecksumComputer {
    private int acc;
    private boolean phase;

    public ChecksumComputer() {
    }

    public ChecksumComputer moreData(ByteBuffer bb) {
        byte restore;
        if (phase) restore = bb.get();
        else restore = 0;
        long temp_acc = 0;
        while (bb.remaining() > 7) {
            final long delta = bb.getLong();
            temp_acc += delta;
            temp_acc += (Long.compareUnsigned(temp_acc, delta) < 0) ? 1L : 0L;
        }
        acc += fold16(fold32(temp_acc));
        while (bb.remaining() > 1) {
            acc += ((int) bb.getShort()) & 0xFFFF;
        }
        acc += ((int) restore) & 0xFF;
        phase = bb.remaining() > 0;
        if (phase) {
            acc += (((int) bb.get()) & 0xFF) << 8;
            phase = true;
        }
        acc = fold16(acc);
        return this;
    }

    public int get() {
        return (int) ((~acc) & 0xFFFFL);
    }

    private static int fold32(long x) {
        while ((x >>> 32) != 0) {
            final long hi = x >>> 32;
            final long lo = x  &  0xFFFFFFFFL;
            x = hi + lo;
        }
        return (int) x;
    }

    private static int fold16(int x) {
        while ((x >>> 16) != 0) {
            final int hi = (x >>> 16) & 0xFFFF;
            final int lo =  x  &  0xFFFF;
            x = hi + lo;
        }
        return x;
    }
}