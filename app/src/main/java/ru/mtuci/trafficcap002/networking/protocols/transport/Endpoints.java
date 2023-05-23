package ru.mtuci.trafficcap002.networking.protocols.transport;

import androidx.annotation.Nullable;

import java.net.InetSocketAddress;

public final class Endpoints {
    private final InetSocketAddress application;
    private final InetSocketAddress site;

    public Endpoints(InetSocketAddress application, InetSocketAddress site) {
        this.application = application;
        this.site = site;
    }

    public InetSocketAddress getApplication() {
        return application;
    }

    public InetSocketAddress getSite() {
        return site;
    }

    @Override
    public int hashCode() {
        return application.hashCode() ^ site.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (o instanceof Endpoints) {
            Endpoints endpoints = (Endpoints) o;
            return endpoints.getSite().equals(this.getSite()) && endpoints.getApplication().equals(this.getApplication());
        } else
            return false;
    }
}
