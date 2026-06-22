package com.kartgame.common.protocol;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public abstract class Packet {
    public static final byte MAGIC_BYTE = 0x4B;
    public static final byte HEADER_SIZE = 8;

    private final PacketType type;
    private int playerToken; // ??

    protected Packet(PacketType type) {
        this.type = type;
    }

    protected abstract void writePayload(ByteBuffer buffer);
    protected abstract void readPayload(ByteBuffer buffer, short length);
    // read payload INTO the buffer

    public ByteBuffer serialize() {
        /*
        * Structure:
        * Magic byte (1 byte)
        * Packet type (1 byte)
        * Player id (4 bytes) (subject to change)
        * Payload length = n (2 bytes)
        * Payload (n bytes)
        * */
        ByteBuffer payloadBuffer = ByteBuffer.allocate(1024);
        writePayload(payloadBuffer);
        payloadBuffer.flip();
        short payloadLength = (short) payloadBuffer.limit();

        ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE + payloadLength);

        buffer.put(MAGIC_BYTE);
        buffer.put(type.getId());
        buffer.putInt(playerToken);
        buffer.putShort(payloadLength);

        buffer.put(payloadBuffer);
        buffer.flip();

        return buffer;
    }

    public byte[] serialize(byte[] encryptedPayload) {
        short payloadLength = (short) encryptedPayload.length;
        ByteBuffer buffer = ByteBuffer.allocate(Packet.HEADER_SIZE + payloadLength);
        buffer.put(MAGIC_BYTE);
        buffer.put(type.getId());
        buffer.putInt(playerToken);
        buffer.putShort(payloadLength);

        buffer.put(encryptedPayload);
        return buffer.array();
    }

    public byte[] serializePayload() {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        writePayload(buffer);
        buffer.flip();

        byte[] payloadBytes = new byte[buffer.limit()];
        buffer.get(payloadBytes);
        return payloadBytes;
    }

    protected void writeString(ByteBuffer buffer, String s) {
        // Write length of string + string
        byte[] strBytes = s.getBytes(StandardCharsets.UTF_8);
        buffer.putShort((short) strBytes.length);
        buffer.put(strBytes);
    }

    protected String readString(ByteBuffer buffer) {
        short strLen = buffer.getShort();
        byte[] strBytes = new byte[strLen];
        buffer.get(strBytes);
        return new String(strBytes, StandardCharsets.UTF_8);
    }

    public PacketType getType() {
        return type;
    }

    public int getPlayerToken() {
        return playerToken;
    }

    public void setPlayerToken(int playerToken) {
        this.playerToken = playerToken;
    }

    @Override
    public String toString() {
        return "Packet{" +
                "type=" + type +
                ", playerId=" + playerToken +
                '}';
    }
}
