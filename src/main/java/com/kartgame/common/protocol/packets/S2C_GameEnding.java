package com.kartgame.common.protocol.packets;

import com.kartgame.common.protocol.Packet;
import com.kartgame.common.protocol.PacketType;

import java.nio.ByteBuffer;

public class S2C_GameEnding extends Packet {
    public S2C_GameEnding() {
        super(PacketType.S2C_GAME_END);
        // TODO: finish this
    }

    @Override
    protected void writePayload(ByteBuffer buffer) {

    }

    @Override
    protected void readPayload(ByteBuffer buffer, short length) {

    }
}
