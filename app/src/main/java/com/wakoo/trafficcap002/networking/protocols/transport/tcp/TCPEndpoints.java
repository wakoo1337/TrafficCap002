package com.wakoo.trafficcap002.networking.protocols.transport.tcp;

import androidx.annotation.Nullable;

import java.net.InetSocketAddress;

public class TCPEndpoints {
    private final InetSocketAddress application;
    private final InetSocketAddress site;

    public TCPEndpoints(InetSocketAddress application, InetSocketAddress site) {
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
        if (o instanceof TCPEndpoints) {
            TCPEndpoints endpoints = (TCPEndpoints) o;
            return endpoints.getSite().equals(this.getSite()) && endpoints.getApplication().equals(this.getApplication());
        } else
            return false;
    }
}
