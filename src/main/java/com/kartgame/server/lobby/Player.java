package com.kartgame.server.lobby;

import com.kartgame.server.network.TCPClientHandler;

import java.net.InetAddress;

public class Player {
    private final int databaseId;
    private final String username;
    private final int token;

    private InetAddress udpAddress = null;
    private int udpPort;

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

    public InetAddress getUdpAddress() {
        return udpAddress;
    }

    public int getUdpPort() {
        return udpPort;
    }

    public void setUdpAddress(InetAddress udpAddress) {
        this.udpAddress = udpAddress;
    }

    public void setUdpPort(int udpPort) {
        this.udpPort = udpPort;
    }

    public boolean isUdpBound() {
        return udpAddress != null;
    }
}
