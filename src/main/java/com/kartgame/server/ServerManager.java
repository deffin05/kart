package com.kartgame.server;

import com.kartgame.common.security.RSAEngineServer;
import com.kartgame.server.database.DatabaseManager;
import com.kartgame.server.lobby.LobbyManager;
import com.kartgame.server.network.TCPServer;
import com.kartgame.server.network.UDPServer;
import com.kartgame.server.packets.PacketDispatcher;

public class ServerManager {
    private TCPServer tcpServer;
    private UDPServer udpServer;
    private DatabaseManager dbManager;
    private LobbyManager lobbyManager;

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

            lobbyManager = new LobbyManager();

            PacketDispatcher packetDispatcher = new PacketDispatcher(dbManager, lobbyManager);

            this.tcpServer = new TCPServer(rsaEngine, packetDispatcher);
            this.udpServer = new UDPServer(lobbyManager);

            lobbyManager.setUdpServer(udpServer);
            Thread tcpThread = new Thread(tcpServer);
            Thread udpThread = new Thread(udpServer);

            tcpThread.start();
            udpThread.start();
        } catch (Exception e) {
            System.err.println("Server failed to start.");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void shutdown() {
        System.out.println("Initiating shutdown");

        tcpServer.stop();
        udpServer.stop();
        lobbyManager.shutdown();
        dbManager.shutdown();
    }
}
