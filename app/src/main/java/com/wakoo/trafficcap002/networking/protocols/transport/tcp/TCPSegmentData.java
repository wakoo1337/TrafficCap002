package com.wakoo.trafficcap002.networking.protocols.transport.tcp;

import com.wakoo.trafficcap002.networking.protocols.transport.Periodic;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

public final class TCPSegmentData {
    private final ByteBuffer data;
    private final int seq;
    private final boolean flag_psh;
    private long last_retry;
    private boolean is_first;

    private TCPSegmentData(ByteBuffer data, int seq, boolean psh) {
        this.data = data;
        this.seq = seq;
        this.last_retry = System.nanoTime();
        this.is_first = true;
        this.flag_psh = psh;
    }

    public static List<TCPSegmentData> makeSegments(ByteBuffer data, int[] seq, int mss) {
        final List<TCPSegmentData> segs;
        segs = new LinkedList<>();
        data.position(0);
        int offset = 0;
        while (offset < data.limit()) {
            final int new_len = Integer.min(mss, data.limit() - offset);
            final ByteBuffer copy;
            copy = (ByteBuffer) ((ByteBuffer) data.position(offset)).slice().limit(new_len);
            segs.add(new TCPSegmentData(copy, seq[0] + offset, new_len == (data.limit() - offset)));
            offset += new_len;
        }
        seq[0] += offset;
        return segs;
    }

    public int getSegmentLength() {
        return data.limit();
    }

    public int getSequenceNumber() {
        return seq;
    }

    public boolean getFlagPush() {
        return flag_psh;
    }

    public ByteBuffer getSegmentData() {
        data.position(0);
        return data;
    }

    public boolean checkTimeoutExpiredThenUpdate() {
        final long now = System.nanoTime();
        if (is_first || (now - last_retry > Periodic.PERIODIC_NANOS)) {
            last_retry = now;
            is_first = false;
            return true;
        } else
            return false;
    }
}
