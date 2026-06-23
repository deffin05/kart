package com.kartgame.common.protocol.packets;

import com.kartgame.common.protocol.PacketType;

public class C2S_RegisterPacket extends CredentialsPacket {
    public C2S_RegisterPacket(String login, String password) {
        super(PacketType.C2S_REGISTER, login, password);
    }

    public C2S_RegisterPacket() {
        super(PacketType.C2S_REGISTER);
    }
}
