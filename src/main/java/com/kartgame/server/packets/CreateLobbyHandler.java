package com.kartgame.server.packets;

import com.kartgame.common.protocol.packets.C2S_CreateLobbyPacket;
import com.kartgame.common.protocol.packets.S2C_LobbyInfoPacket;
import com.kartgame.server.lobby.Lobby;
import com.kartgame.server.lobby.LobbyManager;
import com.kartgame.server.lobby.Player;
import com.kartgame.server.network.TCPClientHandler;

public class CreateLobbyHandler implements PacketHandler<C2S_CreateLobbyPacket> {
    private final LobbyManager lobbyManager;

    public CreateLobbyHandler(LobbyManager lobbyManager) {
        this.lobbyManager = lobbyManager;
    }

    @Override
    public void handle(C2S_CreateLobbyPacket packet, TCPClientHandler client) {
        Player player = lobbyManager.getPlayer(client.getPlayerToken());
        if (player == null) {
            return;
        }

        Lobby lobby = lobbyManager.createLobby(player);
        lobby.broadcast(new S2C_LobbyInfoPacket(lobby.getId(), lobby.getLobbyUsernames()));
    }
}
