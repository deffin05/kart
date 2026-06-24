package com.kartgame.common.protocol.packets;

import com.kartgame.common.protocol.Packet;
import com.kartgame.common.protocol.PacketType;

import java.nio.ByteBuffer;

public class C2S_LobbyStartPacket extends Packet {
    public C2S_LobbyStartPacket() {
        super(PacketType.C2S_START_LOBBY);
    }

    @Override
    protected void writePayload(ByteBuffer buffer) {

    }

    @Override
    protected void readPayload(ByteBuffer buffer, short length) {

    }
}
