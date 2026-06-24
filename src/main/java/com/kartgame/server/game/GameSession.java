package com.kartgame.server.game;

import com.kartgame.common.protocol.Packet;
import com.kartgame.common.protocol.packets.C2S_UserInput;
import com.kartgame.common.protocol.packets.S2C_GameEnding;
import com.kartgame.common.protocol.packets.S2C_WorldState;
import com.kartgame.server.lobby.Player;
import com.kartgame.server.network.UDPServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;

public class GameSession {
    private final int lobbyId;
    private final Map<Integer, KartState> kartStates = new ConcurrentHashMap<>();
    private final Map<Integer, Player> players = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<PlayerInputEvent> incomingInputs = new ConcurrentLinkedQueue<>();
    private final UDPServer udpServer;
    private volatile boolean matchActive = true;

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
            kartStates.put(player.getToken(), new KartState(player.getToken(), startX, startY,
                    new KartState.BoundingBox(gridSpot * 320.0f + 40f, (gridSpot + 1) * 320.0f - 40f)));
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
            Optional<Integer> winner = checkWinConditions();
            winner.ifPresent(this::triggerEndGame);
        } catch (Exception e) {
            System.err.println("Error inside game loop " + lobbyId);
            e.printStackTrace();
        }
    }

    private Optional<Integer> checkWinConditions() {
        int alive = 0;
        KartState alivePlayer = null;
        for (KartState kart : kartStates.values()) {
            if (kart.getHp() > 0) {
                alive++;
                alivePlayer = kart;
            }
        }
        if (alive <= 1 && alivePlayer != null) {
            return Optional.of(alivePlayer.getPlayerToken());
        }
        return Optional.empty();
    }

    private synchronized void triggerEndGame(int winnerToken) {
        if (!matchActive) return;
        this.matchActive = false;

        if (gameLoopFuture != null) {
            gameLoopFuture.cancel(false);
        }

        Player winner = players.get(winnerToken);
        String winnerUsername = winner.getUsername();

        S2C_GameEnding packet = new S2C_GameEnding();
        for (Player p : players.values()) {
            p.getTcpHandler().sendPacket(packet);
        }

        // TODO save result to the database
    }

    private void applyInput(int token, C2S_UserInput input) {
        KartState kart = kartStates.get(token);
        if (kart == null) return;

        if (input.isLeft()) kart.setAngle(kart.getAngle() - 0.05f);
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
            float nextX = kart.getX() + vx;
            float nextY = kart.getY() + vy;

            float minX = kart.getBoundingBox().getLeft();
            float maxX = kart.getBoundingBox().getRight();

            if (nextX < minX) {
                nextX = minX;
            } else if (nextX > maxX) {
                nextX = maxX;
            }

            float minY = kart.getBoundingBox().getBottom();
            float maxY = kart.getBoundingBox().getTop();

            if (nextY < minY) {
                nextY = minY;
            } else if (nextY > maxY) {
                nextY = maxY;
            }

            kart.setX(nextX);
            kart.setY(nextY);
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
