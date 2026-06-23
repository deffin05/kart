package com.kartgame.common.protocol.packets;

import com.kartgame.common.protocol.Packet;
import com.kartgame.common.protocol.PacketType;

import java.nio.ByteBuffer;

public abstract class CredentialsPacket extends Packet {
    private String username;
    private String password;

    public CredentialsPacket(PacketType type, String login, String password) {
        super(type);
        this.username = login;
        this.password = password;
    }

    public CredentialsPacket(PacketType type) {
        super(type);
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
