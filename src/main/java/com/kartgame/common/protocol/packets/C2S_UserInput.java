package com.kartgame.common.protocol.packets;

import com.kartgame.common.protocol.Packet;
import com.kartgame.common.protocol.PacketType;

import java.nio.ByteBuffer;

public class C2S_UserInput extends Packet {
    protected C2S_UserInput() {
        super(PacketType.C2S_USER_INPUT);
    }

    @Override
    protected void writePayload(ByteBuffer buffer) {

    }

    @Override
    protected void readPayload(ByteBuffer buffer, short length) {

    }
}
