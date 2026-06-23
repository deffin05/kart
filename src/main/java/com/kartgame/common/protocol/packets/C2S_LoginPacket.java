package com.kartgame.common.protocol.packets;

import com.kartgame.common.protocol.Packet;
import com.kartgame.common.protocol.PacketType;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class C2S_LoginPacket extends CredentialsPacket {
    public C2S_LoginPacket(String login, String password) {
        super(PacketType.C2S_LOGIN, login, password);
    }

    public C2S_LoginPacket() {
        super(PacketType.C2S_LOGIN);
    }
}
