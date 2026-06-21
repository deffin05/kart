package com.kartgame.common.protocol;

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
        Packet packet;

        switch (type) {
            case C2S_LOGIN:
                packet = new C2S_LoginPacket();
                break;
            default:
                throw new UnsupportedOperationException("Unimplemented packet type: " + type);
        }

        packet.setPlayerId(playerId);
        packet.readPayload(buffer, payloadLength);

        return packet;
    }
}
