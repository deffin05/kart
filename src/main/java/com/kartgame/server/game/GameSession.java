package com.kartgame.server.game;

import com.kartgame.common.protocol.Packet;
import com.kartgame.common.protocol.packets.C2S_UserInput;
import com.kartgame.common.protocol.packets.S2C_WorldState;
import com.kartgame.server.lobby.Player;
import com.kartgame.server.network.UDPServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;

public class GameSession {
    private final int lobbyId;
    private final Map<Integer, KartState> kartStates = new ConcurrentHashMap<>();
    private final Map<Integer, Player> players = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<PlayerInputEvent> incomingInputs = new ConcurrentLinkedQueue<>();
    private final UDPServer udpServer;

    private ScheduledFuture<?> gameLoopFuture;

    public GameSession(int lobbyId, Map<Integer, Player> lobbyPlayers, UDPServer udpServer) {
        this.lobbyId = lobbyId;
        this.udpServer = udpServer;
        this.players.putAll(lobbyPlayers);

        int gridSpot = 0;
        for (Player player : players.values()) {
            // TODO: align with the actual map
            float startX = 160.0f + (gridSpot * 320.0f);
            float startY = 360.0f;
            kartStates.put(player.getToken(), new KartState(player.getToken(), startX, startY));
            gridSpot++;
        }
    }

    public void queueInput(int playerToken, C2S_UserInput inputPacket) {
        incomingInputs.offer(new PlayerInputEvent(playerToken, inputPacket));
    }

    public void tick() {
        try {
            PlayerInputEvent event;
            while ((event = incomingInputs.poll()) != null) {
                applyInput(event.playerToken, event.input);
            }
            simulatePhysics();
            broadcastWorldState();
        } catch (Exception e) {
            System.err.println("Error inside game loop " + lobbyId);
            e.printStackTrace();
        }
    }

    private void applyInput(int token, C2S_UserInput input) {
        KartState kart = kartStates.get(token);
        if (kart == null) return;

        if (input.isLeft())  kart.setAngle(kart.getAngle() - 0.05f);
        if (input.isRight()) kart.setAngle(kart.getAngle() + 0.05f);

        if (input.isAccelerating()) {
            kart.setSpeed(Math.min(kart.getSpeed() + 1.0f, 80.0f));
        } else if (input.isSlowing()) {
            kart.setSpeed(Math.max(kart.getSpeed() - 2.0f, -20.0f));
        } else {
            kart.setSpeed(kart.getSpeed() * 0.95f);
        }
    }

    private void simulatePhysics() {
        for (KartState kart : kartStates.values()) {
            float vx = (float) (Math.cos(kart.getAngle()) * kart.getSpeed());
            float vy = (float) (Math.sin(kart.getAngle()) * kart.getSpeed());

            kart.setX(kart.getX() + vx);
            kart.setY(kart.getY() + vy);
        }
    }

    private void broadcastWorldState() {
        List<S2C_WorldState.KartData> karts = new ArrayList<>();
        for (KartState state : kartStates.values()) {
            karts.add(new S2C_WorldState.KartData(
                    state.getPlayerToken(),
                    state.getX(),
                    state.getY(),
                    state.getAngle(),
                    state.getHp()
            ));
        }

        S2C_WorldState worldState = new S2C_WorldState(karts);
        broadcastUdp(worldState);
    }

    public void broadcastUdp(Packet packet) {
        for (Player player : players.values()) {
            udpServer.sendUdpPacket(packet, player);
        }
    }

    public void setGameLoopFuture(ScheduledFuture<?> future) {
        this.gameLoopFuture = future;
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
