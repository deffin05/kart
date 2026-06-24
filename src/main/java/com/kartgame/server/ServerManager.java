package com.kartgame.server;

import com.kartgame.common.security.RSAEngineServer;
import com.kartgame.server.database.DatabaseManager;
import com.kartgame.server.lobby.LobbyManager;
import com.kartgame.server.network.TCPServer;
import com.kartgame.server.packets.PacketDispatcher;

public class ServerManager {
    private TCPServer tcpServer;
    private DatabaseManager dbManager;

    public static void main(String[] args) {
        new ServerManager().boot();
    }

    public void boot() {
        System.out.println("Starting the server");

        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

        try {
            RSAEngineServer rsaEngine = new RSAEngineServer();
            rsaEngine.generateKeyPair();

            dbManager = new DatabaseManager();
            dbManager.init();

            LobbyManager lobbyManager = new LobbyManager();

            PacketDispatcher packetDispatcher = new PacketDispatcher(dbManager, lobbyManager);

            this.tcpServer = new TCPServer(rsaEngine, packetDispatcher);
            Thread tcpThread = new Thread(tcpServer);

            tcpThread.start();
        } catch (Exception e) {
            System.err.println("Server failed to start.");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void shutdown() {
        System.out.println("Initiating shutdown");

        tcpServer.stop();
        dbManager.shutdown();
    }
}
