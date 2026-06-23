package com.kartgame.server.lobby;

import java.util.concurrent.ConcurrentHashMap;

public class LobbyManager {
    private final ConcurrentHashMap<Integer, Player> playerMap;
    private final ConcurrentHashMap<Integer, Lobby> lobbyMap;


    public LobbyManager() {
        this.playerMap = new ConcurrentHashMap<>();
        this.lobbyMap = new ConcurrentHashMap<>();
    }
}
