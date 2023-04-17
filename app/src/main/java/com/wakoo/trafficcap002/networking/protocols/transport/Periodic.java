package com.wakoo.trafficcap002.networking.protocols.transport;

import java.io.IOException;

public abstract class Periodic {
    public static final long PERIODIC_NANOS = 300000000L; // 300 миллисекунд
    private long last;
    private boolean first_time;

    public Periodic() {
        first_time = true;
    }

    protected final void update() {
        last = System.nanoTime();
    }

    public final void doPeriodic() throws IOException {
        final long now = System.nanoTime();
        if (first_time || (Long.compareUnsigned(last + PERIODIC_NANOS, now) < 0)) {
            first_time = false;
            periodicAction();
            update();
        }
    }

    abstract protected void periodicAction() throws IOException;
}
