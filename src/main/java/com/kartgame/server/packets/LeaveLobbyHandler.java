package com.kartgame.server.packets;

import com.kartgame.common.protocol.packets.C2S_LeaveLobbyPacket;
import com.kartgame.server.lobby.LobbyManager;
import com.kartgame.server.lobby.Player;
import com.kartgame.server.network.TCPClientHandler;

public class LeaveLobbyHandler implements PacketHandler<C2S_LeaveLobbyPacket> {
    private final LobbyManager lobbyManager;

    public LeaveLobbyHandler(LobbyManager lobbyManager) {
        this.lobbyManager = lobbyManager;
    }

    @Override
    public void handle(C2S_LeaveLobbyPacket packet, TCPClientHandler client) {
        Player player = lobbyManager.getPlayer(client.getPlayerToken());
        if (player == null) {
            return;
        }

        lobbyManager.leaveLobby(player);
    }
}
