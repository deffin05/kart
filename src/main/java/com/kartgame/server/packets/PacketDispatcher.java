package com.kartgame.server.packets;

import com.kartgame.common.protocol.Packet;
import com.kartgame.common.protocol.PacketType;
import com.kartgame.server.network.TCPClientHandler;

import java.util.HashMap;
import java.util.Map;

public class PacketDispatcher {
    private final Map<PacketType, PacketHandler<? extends Packet>> handlers = new HashMap<>();
    public PacketDispatcher() {
//        handlers.put();
    }

    @SuppressWarnings("unchecked")
    public <T extends Packet> void dispatch(T packet, TCPClientHandler client) {
        PacketType type = packet.getType();
        PacketHandler<T> handler = (PacketHandler<T>) handlers.get(type);

        if (handler == null) {
            System.err.println("No packet handler exists for type " + type);
            return;
        }

        if (type != PacketType.C2S_LOGIN && !client.isAuthenticated()) {
            System.err.println("Security alert: Unauthenticated user attempted: " + type + ", user: " + client.getSocket());
            client.close();
            return;
        }

        try {
            handler.handle(packet, client);
        } catch (Exception e) {
            System.err.println("Error handling packer: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
