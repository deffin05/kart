package com.kartgame.server.packets;

import com.kartgame.common.protocol.packets.C2S_LoginPacket;
import com.kartgame.common.protocol.packets.C2S_RegisterPacket;
import com.kartgame.common.protocol.packets.S2C_LoginResponse;
import com.kartgame.server.database.DatabaseManager;
import com.kartgame.server.network.TCPClientHandler;

import java.security.SecureRandom;

public class C2S_RegisterHandler implements PacketHandler<C2S_RegisterPacket>{
    private final DatabaseManager db;
    private SecureRandom random = new SecureRandom();

    public C2S_RegisterHandler(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public void handle(C2S_LoginPacket packet, TCPClientHandler client) {
        String username = packet.getUsername();
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

                // TODO: register user token in player hashmap in LobbyManager with Player object

                client.sendPacket(new S2C_LoginResponse(token, "Success"));
            } catch (Exception e) {
                System.err.println("Error processing registration for " + username);
                e.printStackTrace();
                client.sendPacket(new S2C_LoginResponse(-1, "Internal server error."));
            }
        });
    }
}
