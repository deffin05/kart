package com.kartgame.client.packets;

import com.kartgame.common.protocol.packets.S2C_LobbyInfoPacket;
import com.kartgame.client.TCPClient;

public class LobbyInfoHandler implements PacketHandler<S2C_LobbyInfoPacket> {
    @Override
    public void handle(S2C_LobbyInfoPacket packet, TCPClient client) {
        if (client.getLobbyInfoListener() != null) {
            client.getLobbyInfoListener().accept(packet);
        }
    }
}
