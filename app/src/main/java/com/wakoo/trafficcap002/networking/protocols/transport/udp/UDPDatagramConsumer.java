package com.wakoo.trafficcap002.networking.protocols.transport.udp;

import android.util.Log;

import com.wakoo.trafficcap002.networking.PcapWriter;
import com.wakoo.trafficcap002.networking.protocols.ip.IPPacket;
import com.wakoo.trafficcap002.networking.protocols.transport.BadDatagramException;
import com.wakoo.trafficcap002.networking.protocols.transport.DatagramConsumer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.util.HashMap;
import java.util.Map;

public class UDPDatagramConsumer implements DatagramConsumer {
    private final Map<Integer, UDPExternalPort> ports;
    private final Selector selector;
    private final FileOutputStream out;
    private final PcapWriter writer;

    public UDPDatagramConsumer(Selector selector, FileOutputStream out, PcapWriter writer) {
        ports = new HashMap<>();
        this.selector = selector;
        this.out = out;
        this.writer = writer;
    }

    @Override
    public void accept(IPPacket parent) {
        try {
            final UDPPacket packet;
            packet = UDPPacket.of(parent);
            final int src_port;
            src_port = packet.getSourcePort();
            UDPExternalPort external;
            external = ports.get(src_port);
            if (external == null) {
                external = new UDPExternalPort(src_port, selector, out, writer);
                ports.put(src_port, external);
            }
            external.getChannel().send(packet.getPayload(), new InetSocketAddress(packet.getParent().getDestinationAddress(), packet.getDestinationPort()));
        } catch (
                BadDatagramException baddataexcp) {
            Log.e("Получение датаграммы UDP", "Датаграмма содержит ошибку", baddataexcp);
        } catch (
                IOException ioexcp) {
            Log.e("Отправка датаграмм UDP", "Невозможно отправить датаграмму", ioexcp);
        }
    }
}
