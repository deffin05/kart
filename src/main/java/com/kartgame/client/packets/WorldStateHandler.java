package com.kartgame.client.packets;

import com.kartgame.client.TCPClient;
import com.kartgame.common.protocol.packets.S2C_WorldState;

public class WorldStateHandler implements PacketHandler<S2C_WorldState> {
	@Override
	public void handle(S2C_WorldState packet, TCPClient client) {
		if (client.getWorldStateListener() != null) {
			client.getWorldStateListener().accept(packet);
		}
	}
}
