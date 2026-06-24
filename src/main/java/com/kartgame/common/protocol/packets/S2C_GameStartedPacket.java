package com.kartgame.common.protocol.packets;

import com.kartgame.common.protocol.Packet;
import com.kartgame.common.protocol.PacketType;

import java.nio.ByteBuffer;

public class S2C_GameStartedPacket extends Packet {
    public S2C_GameStartedPacket() {
        super(PacketType.S2C_GAME_STARTED);
    }

    @Override
    protected void writePayload(ByteBuffer buffer) {

    }

    @Override
    protected void readPayload(ByteBuffer buffer, short length) {

    }
}
