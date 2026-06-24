package com.kartgame.client;

import com.kartgame.common.protocol.packets.S2C_WorldState;

public interface WorldStateListener {
    void onWorldStateReceived(S2C_WorldState packet);
}