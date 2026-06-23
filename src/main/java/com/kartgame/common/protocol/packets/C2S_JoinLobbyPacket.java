package com.kartgame.common.protocol.packets;

import com.kartgame.common.protocol.Packet;
import com.kartgame.common.protocol.PacketType;

import java.nio.ByteBuffer;

public class C2S_JoinLobbyPacket extends Packet {
    private int lobbyId;

    public C2S_JoinLobbyPacket(int lobbyId) {
        super(PacketType.C2S_JOIN_LOBBY);
        this.lobbyId = lobbyId;
    }

    public C2S_JoinLobbyPacket() {
        super(PacketType.C2S_JOIN_LOBBY);
    }

    @Override
    public void writePayload(ByteBuffer buffer) {
        buffer.putInt(lobbyId);
    }

    @Override
    public void readPayload(ByteBuffer buffer, short length) {
        this.lobbyId = buffer.getInt();
    }

    public int getLobbyId() {
        return lobbyId;
    }

    public void setLobbyId(int lobbyId) {
        this.lobbyId = lobbyId;
    }
}
