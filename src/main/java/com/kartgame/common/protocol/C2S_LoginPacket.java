package com.kartgame.common.protocol;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class C2S_LoginPacket extends Packet {
    private String username;
    private String password;

    public C2S_LoginPacket(String login, String password) {
        super(PacketType.C2S_LOGIN);
        this.username = login;
        this.password = password;
    }

    public C2S_LoginPacket() {
        super(PacketType.C2S_LOGIN);
    }

    @Override
    protected void writePayload(ByteBuffer buffer) {
        writeString(buffer, username);
        writeString(buffer, password);
    }

    @Override
    protected void readPayload(ByteBuffer buffer, short length) {
        username = readString(buffer);
        password = readString(buffer);
    }

    private void writeString(ByteBuffer buffer, String s) {
        // Write length of string + string
        byte[] strBytes = s.getBytes(StandardCharsets.UTF_8);
        buffer.putShort((short) strBytes.length);
        buffer.put(strBytes);
    }

    private String readString(ByteBuffer buffer) {
        short strLen = buffer.getShort();
        byte[] strBytes = new byte[strLen];
        buffer.get(strBytes);
        return new String(strBytes, StandardCharsets.UTF_8);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
