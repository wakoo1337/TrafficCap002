package com.wakoo.trafficcap002.networking.protocols.transport;

public class BadDatagramException extends Exception {
    public BadDatagramException() {
        super();
    }

    public BadDatagramException(String message) {
        super(message);
    }

    public BadDatagramException(String message, Throwable cause) {
        super(message, cause);
    }

    public BadDatagramException(Throwable cause) {
        super(cause);
    }
}
