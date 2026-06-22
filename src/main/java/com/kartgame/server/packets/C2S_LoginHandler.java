package com.kartgame.server.packets;

import com.kartgame.common.protocol.C2S_LoginPacket;
import com.kartgame.server.network.TCPClientHandler;

import java.security.SecureRandom;

public class C2S_LoginHandler implements PacketHandler<C2S_LoginPacket>{
    private SecureRandom random = new SecureRandom();
    @Override
    public void handle(C2S_LoginPacket packet, TCPClientHandler client) {
        String username = packet.getUsername();
        String password = packet.getPassword();

        // TODO: submit this to the database thread pool
        int token = random.nextInt(Integer.MAX_VALUE - 1) + 1;
        client.setPlayerToken(token);
//        client.sendPacket();
    }
}
