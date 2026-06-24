package com.kartgame.server.lobby;

import com.kartgame.common.protocol.Packet;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Lobby {
    public static final int MAX_PLAYERS = 4;

    private final int id;

    private final CopyOnWriteArrayList<Player> players = new CopyOnWriteArrayList<>();

    private volatile boolean gameStarted = false;

    public Lobby(int id) {
        this.id = id;
    }

    public synchronized boolean addPlayer(Player player) {
        if (gameStarted) return false;
        if (players.size() >= MAX_PLAYERS) return false;
        if (players.contains(player)) return false;

        players.add(player);
        player.setCurrentLobbyId(this.id);
        return true;
    }

    public synchronized void removePlayer(Player player) {
        players.remove(player);
        player.setCurrentLobbyId(null);
    }

    public List<String> getLobbyUsernames() {
        List<String> list = new ArrayList<>();

        for (Player player : players) {
            list.add(player.getUsername());
        }

        return list;
    }

    public boolean canStart() {
        if (players.size() < 2) return false; // Minimum 2 players

        return true;
    }

    public void broadcast(Packet packet) {
        for (Player p : players) {
            p.getTcpHandler().sendPacket(packet);
        }
    }

    public int getPlayerCount() {
        return players.size();
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public void setGameStarted(boolean gameStarted) {
        this.gameStarted = gameStarted;
    }

    public int getId() {
        return id;
    }

    public CopyOnWriteArrayList<Player> getPlayers() {
        return players;
    }
}