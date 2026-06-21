package com.kartgame.server.network;

import com.kartgame.common.security.RSAEngineServer;

import java.net.Socket;

public class TCPClientHandler implements Runnable {
    private final RSAEngineServer RSAEngine;
    private final Socket socket;

    public TCPClientHandler(Socket serverSocket, RSAEngineServer rsaEngine) {
        RSAEngine = rsaEngine;
        this.socket = serverSocket;
    }

    @Override
    public void run() {

    }
}
