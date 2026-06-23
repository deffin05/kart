package com.kartgame.server.lobby;

import com.kartgame.server.network.TCPClientHandler;

public class Player {
    private final int databaseId;
    private final String username;
    private final int token;

    private final TCPClientHandler tcpHandler;

    private volatile Integer currentLobbyId = null;

    public Player(int databaseId, String username, int token, TCPClientHandler tcpHandler) {
        this.databaseId = databaseId;
        this.username = username;
        this.token = token;
        this.tcpHandler = tcpHandler;
    }

    public int getDatabaseId() {
        return databaseId;
    }

    public String getUsername() {
        return username;
    }

    public int getToken() {
        return token;
    }

    public TCPClientHandler getTcpHandler() {
        return tcpHandler;
    }

    public Integer getCurrentLobbyId() {
        return currentLobbyId;
    }

    public void setCurrentLobbyId(Integer currentLobbyId) {
        this.currentLobbyId = currentLobbyId;
    }
}
