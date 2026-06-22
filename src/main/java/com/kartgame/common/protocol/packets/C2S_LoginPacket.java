package com.kartgame.common.protocol.packets;

import com.kartgame.common.protocol.Packet;
import com.kartgame.common.protocol.PacketType;

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
