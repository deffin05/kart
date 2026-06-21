package com.kartgame.common.protocol;

public enum PacketType {
    // C2S = ClientToServer, S2C = ServerToClient
    // TCP
    C2S_LOGIN(1),
    S2C_LOGIN_RESPONSE(2),


    // UDP
    C2S_USER_INPUT(41),
    S2C_WORLD_STATE(42);


    private final byte id;

    PacketType(int id) {
        this.id = (byte) id;
    }

    public byte getId() {
        return id;
    }
}
