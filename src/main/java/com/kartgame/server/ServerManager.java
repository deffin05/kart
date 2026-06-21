package com.kartgame.server;

import com.kartgame.common.security.RSAEngineServer;
import com.kartgame.server.network.TCPServer;

public class ServerManager {
    public static void main(String[] args) {
        System.out.println("Starting the server");

        try {
            RSAEngineServer rsaEngine = new RSAEngineServer();
            rsaEngine.generateKeyPair();

            TCPServer tcpServer = new TCPServer(rsaEngine);
            Thread tcpThread = new Thread(tcpServer);

            tcpThread.start();
        } catch (Exception e) {
            System.err.println("Server failed to start.");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
