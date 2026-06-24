package com.kartgame.common.protocol.packets;

import com.kartgame.common.protocol.Packet;
import com.kartgame.common.protocol.PacketType;

import java.nio.ByteBuffer;

public class S2C_GameEnding extends Packet {
    private String winnerUsername;

    public S2C_GameEnding(String winnerUsername) {
        super(PacketType.S2C_GAME_END);
        this.winnerUsername = winnerUsername;
    }

    public S2C_GameEnding() {
        super(PacketType.S2C_GAME_END);
    }

    @Override
    protected void writePayload(ByteBuffer buffer) {
        writeString(buffer, winnerUsername);
    }

    @Override
    protected void readPayload(ByteBuffer buffer, short length) {
        winnerUsername = readString(buffer);
    }
}
