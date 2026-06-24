package com.kartgame.client.packets;

import com.kartgame.common.protocol.packets.S2C_LoginResponse;
import com.kartgame.client.TCPClient;

public class LoginResponseHandler implements PacketHandler<S2C_LoginResponse> {
    @Override
    public void handle(S2C_LoginResponse packet, TCPClient client) {
        if (packet.getToken() > 0) {
            client.setLoginTag(packet.getToken());
        }
        if (client.getLoginResponseListener() != null) {
            client.getLoginResponseListener().accept(packet);
        }
    }
}
