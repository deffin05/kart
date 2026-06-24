package com.kartgame.server.packets;

import com.kartgame.common.protocol.packets.C2S_RegisterPacket;
import com.kartgame.common.protocol.packets.S2C_LoginResponse;
import com.kartgame.server.database.DatabaseManager;
import com.kartgame.server.lobby.LobbyManager;
import com.kartgame.server.lobby.Player;
import com.kartgame.server.network.TCPClientHandler;

import java.security.SecureRandom;

public class C2S_RegisterHandler implements PacketHandler<C2S_RegisterPacket>{
    private final DatabaseManager db;
    private final LobbyManager lobbyManager;
    private SecureRandom random = new SecureRandom();

    public C2S_RegisterHandler(DatabaseManager db, LobbyManager lobbyManager) {
        this.db = db;
        this.lobbyManager = lobbyManager;
    }

    @Override
    public void handle(C2S_RegisterPacket packet, TCPClientHandler client) {
        String username = packet.getUsername() == null ? "" : packet.getUsername().trim();
        String password = packet.getPassword();

        db.execute(() -> {
            try {
                boolean registred = db.registerUser(username, password);

                if (!registred) {
                    client.sendPacket(new S2C_LoginResponse(-1, "Failed to create user account."));
                    return;
                }

                int token = random.nextInt(Integer.MAX_VALUE - 1) + 1;
                client.setPlayerToken(token);

                client.sendPacket(new S2C_LoginResponse(token, "Success"));

                int dbId = db.getUserId(username);
                if (dbId == -1) {
                    System.err.println("Invalid user id after login");
                    client.sendPacket(new S2C_LoginResponse(-1, "Internal server error."));
                    return;
                }

                Player player = new Player(dbId, username, token, client);
                lobbyManager.registerPlayer(player);
            } catch (Exception e) {
                System.err.println("Error processing registration for " + username);
                e.printStackTrace();
                client.sendPacket(new S2C_LoginResponse(-1, "Internal server error."));
            }
        });
    }
}
