package com.kartgame.common.protocol;

import com.kartgame.common.protocol.packets.*;

import java.nio.ByteBuffer;

public class PacketRegistry {
    public static Packet parse(ByteBuffer buffer) {
        if (buffer.remaining() < Packet.HEADER_SIZE) {
            return null;
        }

        byte magicByte = buffer.get();
        if (magicByte != Packet.MAGIC_BYTE) {
            throw new SecurityException("Invalid packet received: invalid magic byte");
        }

        byte pktId = buffer.get();
        int playerId = buffer.getInt();
        short payloadLength = buffer.getShort();

        if (buffer.remaining() < payloadLength) {
            throw new IllegalArgumentException("Incomplete payload");
        }

        PacketType type = PacketType.fromId(pktId);
        Packet packet = switch (type) {
            case C2S_LOGIN -> new C2S_LoginPacket();
            case S2C_LOGIN_RESPONSE -> new S2C_LoginResponse();
            case C2S_REGISTER -> new C2S_RegisterPacket();
            case S2C_LOBBY_INFO -> new S2C_LobbyInfoPacket();
            case C2S_JOIN_LOBBY -> new C2S_JoinLobbyPacket();
            case C2S_START_LOBBY -> new C2S_LobbyStartPacket();
            case S2C_GAME_STARTED -> new S2C_GameStartedPacket();
            case C2S_USER_INPUT -> new C2S_UserInput();
            case S2C_WORLD_STATE -> new S2C_WorldState();
            case S2C_GAME_END -> new S2C_GameEnding();
            default -> throw new UnsupportedOperationException("Unimplemented packet type: " + type);
        };

        packet.setPlayerToken(playerId);
        packet.readPayload(buffer, payloadLength);

        return packet;
    }
}
