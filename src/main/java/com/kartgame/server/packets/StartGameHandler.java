package com.kartgame.server.packets;

import com.kartgame.common.protocol.packets.C2S_LobbyStartPacket;
import com.kartgame.server.lobby.LobbyManager;
import com.kartgame.server.lobby.Player;
import com.kartgame.server.network.TCPClientHandler;

public class StartGameHandler implements PacketHandler<C2S_LobbyStartPacket> {
    private final LobbyManager lobbyManager;

    public StartGameHandler(LobbyManager lobbyManager) {
        this.lobbyManager = lobbyManager;
    }

    @Override
    public void handle(C2S_LobbyStartPacket packet, TCPClientHandler client) {
        Player player = lobbyManager.getPlayer(client.getPlayerToken());
        if (player == null) return;

        Integer lobbyId = player.getCurrentLobbyId();
        if (lobbyId == null) return;

        lobbyManager.startMatch(lobbyId);
    }
}