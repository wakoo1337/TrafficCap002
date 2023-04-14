package com.wakoo.trafficcap002.networking.protocols.transport.tcp;

public final class TCPOption {
    private final int value;
    private final boolean presence;

    public TCPOption(int value, boolean presence) {
        this.value = value;
        this.presence = presence;
    }

    public int getValue() {
        return value;
    }

    public boolean getPresence() {
        return presence;
    }
}
