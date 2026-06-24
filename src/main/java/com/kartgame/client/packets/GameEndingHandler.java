package com.kartgame.client.packets;

import com.kartgame.client.TCPClient;
import com.kartgame.common.protocol.packets.S2C_GameEnding;

public class GameEndingHandler implements PacketHandler<S2C_GameEnding>{
    @Override
    public void handle(S2C_GameEnding packet, TCPClient client) {
        // TODO
    }
}
