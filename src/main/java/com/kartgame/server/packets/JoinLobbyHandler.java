package com.kartgame.server.packets;

import com.kartgame.common.protocol.packets.C2S_JoinLobbyPacket;
import com.kartgame.common.protocol.packets.C2S_LoginPacket;
import com.kartgame.common.protocol.packets.S2C_LobbyInfoPacket;
import com.kartgame.common.protocol.packets.S2C_LoginResponse;
import com.kartgame.server.database.DatabaseManager;
import com.kartgame.server.lobby.Lobby;
import com.kartgame.server.lobby.LobbyManager;
import com.kartgame.server.lobby.Player;
import com.kartgame.server.network.TCPClientHandler;

import java.security.SecureRandom;

public class JoinLobbyHandler implements PacketHandler<C2S_JoinLobbyPacket>{
    private final LobbyManager lobbyManager;

    public JoinLobbyHandler(LobbyManager lobbyManager) {
        this.lobbyManager = lobbyManager;
    }

    @Override
    public void handle(C2S_JoinLobbyPacket packet, TCPClientHandler client) {
        Player player = lobbyManager.getPlayer(client.getPlayerToken());
        if (player == null) return;

        int targetLobbyId = packet.getLobbyId();

        boolean success = lobbyManager.joinLobby(player, targetLobbyId);

        if (success) {
            Lobby newLobby = lobbyManager.getLobbyMap().get(targetLobbyId);
            if (newLobby != null) {
                 newLobby.broadcast(new S2C_LobbyInfoPacket(newLobby.getId(), newLobby.getLobbyUsernames()));
            }
        } else {
            // TODO: Implement system message packet
            // client.sendPacket(new S2C_SystemMessagePacket("Failed to join lobby."));
        }
    }
}
