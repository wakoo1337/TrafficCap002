package ru.mtuci.trafficcap002.networking.protocols.transport.udp;

import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.util.HashMap;
import java.util.Map;

import ru.mtuci.trafficcap002.networking.HttpWriter;
import ru.mtuci.trafficcap002.networking.PcapWriter;
import ru.mtuci.trafficcap002.networking.protocols.ip.IPPacket;
import ru.mtuci.trafficcap002.networking.protocols.transport.BadDatagramException;
import ru.mtuci.trafficcap002.networking.protocols.transport.DatagramConsumer;

public final class UDPDatagramConsumer implements DatagramConsumer {
    private final Map<Integer, UDPExternalPort> ports;
    private final Selector selector;
    private final FileOutputStream out;
    private final PcapWriter pcap_writer;
    private final HttpWriter http_writer;

    public UDPDatagramConsumer(Selector selector, FileOutputStream out, PcapWriter pcap_writer, HttpWriter http_writer) {
        ports = new HashMap<>();
        this.selector = selector;
        this.out = out;
        this.pcap_writer = pcap_writer;
        this.http_writer = http_writer;
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
                external = new UDPExternalPort(src_port, selector, out, pcap_writer);
                ports.put(src_port, external);
            }
            if (http_writer != null)
                http_writer.send(packet.getPayload(), packet.getParent().getSourceAddress(), packet.getParent().getDestinationAddress(), packet.getSourcePort(), packet.getDestinationPort(), "udp");
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
