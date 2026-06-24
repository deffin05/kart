package com.kartgame.server.game;

import com.kartgame.common.protocol.Packet;
import com.kartgame.common.protocol.packets.C2S_UserInput;
import com.kartgame.common.protocol.packets.S2C_GameEnding;
import com.kartgame.common.protocol.packets.S2C_WorldState;
import com.kartgame.server.database.DatabaseManager;
import com.kartgame.server.lobby.Player;
import com.kartgame.server.network.UDPServer;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;

public class GameSession {
    private final int lobbyId;
    private final Map<Integer, KartState> kartStates = new ConcurrentHashMap<>();
    private final Map<Integer, Player> players = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<PlayerInputEvent> incomingInputs = new ConcurrentLinkedQueue<>();
    private final CopyOnWriteArrayList<Collectible> collectibles = new CopyOnWriteArrayList<>();
    private final UDPServer udpServer;
    private final DatabaseManager db;
    private final IntConsumer matchFinishedCallback;
    private volatile boolean matchActive = true;
    private final Instant startTime = Instant.now();
    private final SecureRandom random = new SecureRandom();
    private int tickCounter = 0;

    private ScheduledFuture<?> gameLoopFuture;

    public GameSession(int lobbyId, Map<Integer, Player> lobbyPlayers, UDPServer udpServer, DatabaseManager db, IntConsumer matchFinishedCallback) {
        this.lobbyId = lobbyId;
        this.udpServer = udpServer;
        this.db = db;
        this.matchFinishedCallback = matchFinishedCallback;
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

            tickCounter++;
            if (tickCounter >= 60) {
                spawnCollectible();
                tickCounter = 0;
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

    private void spawnCollectible() {
        if (collectibles.size() > 60) return;

        float rx = random.nextInt(0, players.size()) * 320f + random.nextFloat(40f, 280f);;
        float ry = random.nextFloat(40f, 680f);

        Collectible col = new Collectible(rx, ry);
        collectibles.add(col);
    }

    private synchronized void triggerEndGame(int winnerToken) {
        if (!matchActive) return;
        this.matchActive = false;

        if (gameLoopFuture != null) {
            gameLoopFuture.cancel(false);
        }

        Player winner = players.get(winnerToken);
        String winnerUsername = winner.getUsername();

        S2C_GameEnding packet = new S2C_GameEnding(winnerUsername);
        for (Player p : players.values()) {
            p.getTcpHandler().sendPacket(packet);
        }

        db.execute(() -> {
            List<Integer> playerIds = new ArrayList<>(4);
            for (Player p : players.values()) {
                playerIds.add(p.getDatabaseId());
            }

            db.insertBattleLog(winner.getDatabaseId(), Duration.between(startTime, Instant.now()).toMillis(), playerIds);
        });

        if (matchFinishedCallback != null) {
            matchFinishedCallback.accept(lobbyId);
        }
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
        Set<Collectible> consumedCollectibles = new HashSet<>();

        for (KartState kart : kartStates.values()) {
            float vx = (float) (Math.sin(kart.getAngle()) * kart.getSpeed());
            float vy = (float) (-Math.cos(kart.getAngle()) * kart.getSpeed());
            float nextX = kart.getX() + vx;
            float nextY = kart.getY() + vy;

            nextX = Math.max(kart.getBoundingBox().getLeft(), Math.min(kart.getBoundingBox().getRight(), nextX));
            nextY = Math.max(kart.getBoundingBox().getBottom(), Math.min(kart.getBoundingBox().getTop(), nextY));
            kart.setX(nextX);
            kart.setY(nextY);

            for (Collectible col : collectibles) {
                if (consumedCollectibles.contains(col)) {
                    continue;
                }

                float dx = kart.getX() - col.getX();
                float dy = kart.getY() - col.getY();
                float collisionDistance = KartState.HIT_RADIUS + Collectible.RADIUS;

                if ((dx * dx + dy * dy) < collisionDistance * collisionDistance) {
                    for (KartState kartState : kartStates.values()) {
                        if (kart.equals(kartState)) continue;
                        kartState.setHp(Math.max(0, kartState.getHp() - 10));
                    }
                    consumedCollectibles.add(col);
                    break;
                }
            }
        }

        if (!consumedCollectibles.isEmpty()) {
            collectibles.removeAll(consumedCollectibles);
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
        S2C_WorldState worldState = new S2C_WorldState(karts, collectibles);
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
