package com.wakoo.trafficcap002.networking.protocols.ip;

public final class BadIPPacketException extends Exception {
    public BadIPPacketException() {
        super();
    }

    public BadIPPacketException(String message) {
        super(message);
    }

    public BadIPPacketException(String message, Throwable cause) {
        super(message, cause);
    }

    public BadIPPacketException(Throwable cause) {
        super(cause);
    }
}
