package com.kartgame.common.protocol.packets;

import com.kartgame.common.protocol.Packet;
import com.kartgame.common.protocol.PacketType;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class S2C_LobbyInfoPacket extends Packet {
    private int lobbyId;
    private List<String> playerUsernames;

    public S2C_LobbyInfoPacket(int lobbyId, List<String> playerUsernames) {
        super(PacketType.S2C_LOBBY_INFO);
        this.lobbyId = lobbyId;
        this.playerUsernames = playerUsernames;
    }

    public S2C_LobbyInfoPacket() {
        super(PacketType.S2C_LOBBY_INFO);
    }

    @Override
    public void writePayload(ByteBuffer buffer) {
        buffer.putInt(lobbyId);

        buffer.put((byte) playerUsernames.size());

        for (String username : playerUsernames) {
            writeString(buffer, username);
        }
    }

    @Override
    public void readPayload(ByteBuffer buffer, short length) {
        this.lobbyId = buffer.getInt();

        int playerCount = buffer.get();
        this.playerUsernames = new ArrayList<>(playerCount);

        for (int i = 0; i < playerCount; i++) {
            this.playerUsernames.add(readString(buffer));
        }
    }

}
