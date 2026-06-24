package com.kartgame.common.protocol.packets;

import com.kartgame.common.protocol.Packet;
import com.kartgame.common.protocol.PacketType;

import java.nio.ByteBuffer;

public class C2S_RecentGamesRequestPacket extends Packet {
    public C2S_RecentGamesRequestPacket() {
        super(PacketType.C2S_RECENT_GAMES_REQUEST);
    }

    @Override
    protected void writePayload(ByteBuffer buffer) {
        // Empty payload
    }

    @Override
    protected void readPayload(ByteBuffer buffer, short length) {
        // Empty payload
    }
}
