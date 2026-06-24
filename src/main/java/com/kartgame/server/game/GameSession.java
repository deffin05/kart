package com.kartgame.server.game;

import com.kartgame.common.protocol.Packet;
import com.kartgame.common.protocol.packets.C2S_UserInput;
import com.kartgame.server.lobby.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class GameSession {
    private final int lobbyId;
    private final Map<Integer, KartState> kartStates = new ConcurrentHashMap<>();
    private final Map<Integer, Player> players = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<PlayerInputEvent> incomingInputs = new ConcurrentLinkedQueue<>();

    public GameSession(int lobbyId, Map<Integer, Player> lobbyPlayers) {
        this.lobbyId = lobbyId;
        this.players.putAll(lobbyPlayers);

        int gridSpot = 0;
        for (Player player : players.values()) {
            // TODO: align with the actual map
            float startX = 100.0f + (gridSpot * 60.0f);
            float startY = 300.0f;
            kartStates.put(player.getToken(), new KartState(player.getToken(), startX, startY));
            gridSpot++;
        }
    }

    public void queueInput(int playerToken, C2S_UserInput inputPacket) {
        incomingInputs.offer(new PlayerInputEvent(playerToken, inputPacket));
    }

    public void tick() {

    }

    private void broadcastWorldState() {
        // TODO
        // S2C_WorldStatePacket packet = new S2C_WorldStatePacket(kartStates.values());
        // broadcastUdp(packet);
    }

    public void broadcastUdp(Packet packet) {
        // TODO
        // Helper to send UDP packets to everyone in this session
        // We will implement this shortly!
    }

    private static class PlayerInputEvent {
        final int playerToken;
        final C2S_UserInput input;

        PlayerInputEvent(int token, C2S_UserInput input) {
            this.playerToken = token;
            this.input = input;
        }
    }
}
