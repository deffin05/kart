package com.kartgame.client.packets;

import com.kartgame.common.protocol.Packet;
import com.kartgame.client.TCPClient;

import java.util.HashMap;
import java.util.Map;

public class PacketDispatcher {
    private final Map<com.kartgame.common.protocol.PacketType, PacketHandler<? extends Packet>> handlers = new HashMap<>();

    public PacketDispatcher() {
        // placeholder handlers can be registered here
    }

    @SuppressWarnings("unchecked")
    public <T extends Packet> void dispatch(T packet, TCPClient client) {
        PacketHandler<T> handler = (PacketHandler<T>) handlers.get(packet.getType());
        if (handler != null) {
            handler.handle(packet, client);
        } else {
            System.err.println("No client packet handler for type: " + packet.getType());
        }
    }

    public void registerHandler(com.kartgame.common.protocol.PacketType type, PacketHandler<? extends Packet> handler) {
        handlers.put(type, handler);
    }
}
