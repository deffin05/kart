package com.kartgame.common.protocol.packets;

import com.kartgame.common.protocol.Packet;
import com.kartgame.common.protocol.PacketType;

import java.nio.ByteBuffer;

public class S2C_LoginResponse extends Packet {
    private int token;
    private String message;

    public S2C_LoginResponse(int token, String message) {
        super(PacketType.S2C_LOGIN_RESPONSE);
        this.token = token;
        this.message = message;
    }

    public S2C_LoginResponse() {
        super(PacketType.S2C_LOGIN_RESPONSE);
    }

    @Override
    protected void writePayload(ByteBuffer buffer) {
        buffer.putInt(token);
        writeString(buffer, message);
    }

    @Override
    protected void readPayload(ByteBuffer buffer, short length) {
        this.token = buffer.getInt();
        this.message = readString(buffer);
    }

    public int getToken() {
        return token;
    }

    public void setToken(int token) {
        this.token = token;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
