package com.kartgame.server.packets;

import com.kartgame.common.protocol.Packet;
import com.kartgame.server.database.DatabaseManager;
import com.kartgame.server.network.TCPClientHandler;

public interface PacketHandler <T extends Packet>{
    void handle(T packet, TCPClientHandler client);
}
