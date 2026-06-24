package com.kartgame.client.packets;

import com.kartgame.common.protocol.Packet;

public interface PacketHandler<T extends Packet> {
    void handle(T packet, com.kartgame.client.TCPClient client);
}
