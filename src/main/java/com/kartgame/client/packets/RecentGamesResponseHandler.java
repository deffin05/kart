package com.kartgame.client.packets;

import com.kartgame.client.TCPClient;
import com.kartgame.common.protocol.packets.S2C_RecentGamesResponsePacket;

public class RecentGamesResponseHandler implements PacketHandler<S2C_RecentGamesResponsePacket> {
    @Override
    public void handle(S2C_RecentGamesResponsePacket packet, TCPClient client) {
        if (client.getRecentGamesListener() != null) {
            client.getRecentGamesListener().accept(packet);
        }
    }
}
