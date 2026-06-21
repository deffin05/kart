package com.kartgame.common.protocol;

import java.nio.ByteBuffer;

public abstract class Packet {
    public static final byte MAGIC_BYTE = 0x4B;
    public static final byte HEADER_SIZE = 8;

    private final PacketType type;
    private int playerId; // ??

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
        buffer.putInt(playerId);
        buffer.putShort(payloadLength);

        buffer.put(payloadBuffer);
        buffer.flip();

        return buffer;
    }
}
