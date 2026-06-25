package com.kartgame.common.protocol;

public enum PacketType {
    // C2S = ClientToServer, S2C = ServerToClient
    // TCP
    C2S_LOGIN(1),
    S2C_LOGIN_RESPONSE(2),
    C2S_REGISTER(3),
    S2C_LOBBY_INFO(4),
    C2S_JOIN_LOBBY(5),
    C2S_CREATE_LOBBY(6),
    C2S_START_LOBBY(7),
    C2S_LEAVE_LOBBY(8),
    S2C_GAME_STARTED(9),
    S2C_GAME_END(10),
    C2S_RECENT_GAMES_REQUEST(11),
    S2C_RECENT_GAMES_RESPONSE(12),

    // UDP
    C2S_USER_INPUT(41),
    S2C_WORLD_STATE(42),

    // Universal ping
    PING(100);

    private final byte id;

    PacketType(int id) {
        this.id = (byte) id;
    }

    public static PacketType fromId(byte id) {
        for (PacketType type : values()) {
            if (id == type.getId()) return type;
        }
        throw new IllegalArgumentException("Invalid type id");
    }

    public byte getId() {
        return id;
    }
}
