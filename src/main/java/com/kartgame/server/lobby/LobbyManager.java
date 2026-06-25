package com.kartgame.server.lobby;

import com.kartgame.common.protocol.packets.S2C_GameStartedPacket;
import com.kartgame.common.protocol.packets.S2C_LobbyInfoPacket;
import com.kartgame.server.database.DatabaseManager;
import com.kartgame.server.game.GameSession;
import com.kartgame.server.network.UDPServer;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class LobbyManager {
    private final ConcurrentHashMap<Integer, Player> playerMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Lobby> lobbyMap =  new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, GameSession> activeSessions = new ConcurrentHashMap<>();

    private final ScheduledExecutorService gameLoopScheduler = Executors.newScheduledThreadPool(4);

    private final SecureRandom random = new SecureRandom();
    private UDPServer udpServer;
    private DatabaseManager db;

    public void setUdpServer(UDPServer udpServer) {
        this.udpServer = udpServer;
    }

    public DatabaseManager getDb() {
        return db;
    }

    public void setDb(DatabaseManager db) {
        this.db = db;
    }

    public void registerPlayer(Player player) {
        playerMap.put(player.getToken(), player);
    }

    public Player getPlayer(int token) {
        return playerMap.get(token);
    }

    public void disconnectPlayer(int token) {
        Player player = playerMap.remove(token);
        if (player != null) {
            if (player.getCurrentLobbyId() != null) {
                leaveLobby(player);
            }
            System.out.println("Closed session for player: " + player.getUsername());
        }
    }

    public Lobby createLobby(Player creator) {
        if (creator.getCurrentLobbyId() != null) {
            leaveLobby(creator);
        }

        int lobbyId = generateLobbyCode();
        Lobby newLobby = new Lobby(lobbyId);

        lobbyMap.put(lobbyId, newLobby);
        newLobby.addPlayer(creator);

        System.out.println("Lobby " + lobbyId + " created by " + creator.getUsername());
        return newLobby;
    }

    public boolean joinLobby(Player player, int lobbyId) {
        Lobby lobby = lobbyMap.get(lobbyId);

        if (lobby == null) return false;

        if (player.getCurrentLobbyId() != null) {
            leaveLobby(player);
        }

        return lobby.addPlayer(player);
    }

    public void leaveLobby(Player player) {
        Integer lobbyId = player.getCurrentLobbyId();
        if (lobbyId == null) return;

        Lobby lobby = lobbyMap.get(lobbyId);
        if (lobby != null) {
            lobby.removePlayer(player);

            if (lobby.getPlayerCount() == 0) {
                lobbyMap.remove(lobbyId);
                System.out.println("Lobby " + lobbyId + " destroyed.");
            } else {
                lobby.broadcast(new S2C_LobbyInfoPacket(lobbyId, lobby.getLobbyUsernames()));
            }
        }
    }

    private int generateLobbyCode() {
        int code;
        do {
            code = random.nextInt(100_000) ;
        } while (lobbyMap.containsKey(code));

        return code;
    }

    public ConcurrentHashMap<Integer, Player> getPlayerMap() {
        return playerMap;
    }

    public ConcurrentHashMap<Integer, Lobby> getLobbyMap() {
        return lobbyMap;
    }

    public synchronized void startMatch(int lobbyId) {
        Lobby lobby = lobbyMap.get(lobbyId);
        if (lobby == null || lobby.isGameStarted()) return;

        Map<Integer, Player> sessionPlayers = new HashMap<>();
        for (Player p : lobby.getPlayers()) {
            p.setUdpAddress(null);
            p.setUdpPort(-1);
            sessionPlayers.put(p.getToken(), p);
        }

        GameSession session = new GameSession(lobbyId, sessionPlayers, udpServer, db, this::onMatchFinished);
        activeSessions.put(lobbyId, session);

        lobby.setGameStarted(true);

        ScheduledFuture<?> future = gameLoopScheduler.scheduleAtFixedRate(
                session::tick, 0, 20, TimeUnit.MILLISECONDS
        );
        session.setGameLoopFuture(future);

        lobby.broadcast(new S2C_GameStartedPacket());
        System.out.println("Match started for Lobby " + lobbyId);
    }

    private synchronized void onMatchFinished(int lobbyId) {
        activeSessions.remove(lobbyId);

        Lobby lobby = lobbyMap.get(lobbyId);
        if (lobby != null) {
            lobby.setGameStarted(false);
            lobby.broadcast(new S2C_LobbyInfoPacket(lobbyId, lobby.getLobbyUsernames()));
        }
    }

    public void shutdown() {
        System.out.println("Stopping Game Loop Scheduler...");
        gameLoopScheduler.shutdown();
        try {
            if (!gameLoopScheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                gameLoopScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            gameLoopScheduler.shutdownNow();
        }
    }

    public ConcurrentHashMap<Integer, GameSession> getActiveSessions() {
        return activeSessions;
    }
}
