package ru.mtuci.trafficcap002.networking.protocols.transport;

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
        if (first_time || (System.nanoTime() - last > PERIODIC_NANOS)) {
            first_time = false;
            periodicAction();
            update();
        }
    }

    abstract protected void periodicAction() throws IOException;
}
