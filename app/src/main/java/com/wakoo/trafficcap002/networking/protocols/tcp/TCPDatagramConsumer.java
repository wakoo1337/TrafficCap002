package com.wakoo.trafficcap002.networking.protocols.tcp;

import android.util.Log;

import com.wakoo.trafficcap002.networking.protocols.BadDatagramException;
import com.wakoo.trafficcap002.networking.protocols.DatagramConsumer;
import com.wakoo.trafficcap002.networking.protocols.ip.IPPacket;

public class TCPDatagramConsumer implements DatagramConsumer {
    @Override
    public void accept(IPPacket parent) {
        try {
            TCPPacket packet;
            packet = TCPPacket.of(parent);
        } catch (
                BadDatagramException badexcp) {
            Log.e("Разбор пакета TCP", "Плохой пакет TCP", badexcp);
        }
    }
}
