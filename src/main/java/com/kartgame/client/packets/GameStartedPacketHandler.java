package com.kartgame.client.packets;

import com.kartgame.client.TCPClient;
import com.kartgame.common.protocol.packets.S2C_GameStartedPacket;

public class GameStartedPacketHandler implements PacketHandler<S2C_GameStartedPacket> {
	@Override
	public void handle(S2C_GameStartedPacket packet, TCPClient client) {
		if (client.getGameStartedListener() != null) {
			client.getGameStartedListener().accept(packet);
		}
	}
}
