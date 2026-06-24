package com.kartgame.common.protocol.packets;

import com.kartgame.common.protocol.Packet;
import com.kartgame.common.protocol.PacketType;

import java.nio.ByteBuffer;

public class C2S_LeaveLobbyPacket extends Packet {
    public C2S_LeaveLobbyPacket() {
        super(PacketType.C2S_LEAVE_LOBBY);
    }

    @Override
    protected void writePayload(ByteBuffer buffer) {
        // No payload needed.
    }

    @Override
    protected void readPayload(ByteBuffer buffer, short length) {
        // No payload needed.
    }
}
