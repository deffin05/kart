package com.kartgame.server.network;

import com.kartgame.common.security.RSAEngineServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPServer implements Runnable{
    public static int PORT = 13488;
    private final RSAEngineServer rsaEngine;
    private final ServerSocket serverSocket;
    private final ExecutorService executorService;
    private boolean isRunning = true;

    public TCPServer(RSAEngineServer rsaEngine) throws IOException {
        this.rsaEngine = rsaEngine;
        this.serverSocket = new ServerSocket(PORT);
        this.executorService = Executors.newCachedThreadPool();
        System.out.println("TCP server started: " + serverSocket);
    }

    @Override
    public void run() {
        while (isRunning && !serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New TCP connection: " + clientSocket);

                TCPClientHandler clientHandler = new TCPClientHandler(clientSocket, rsaEngine);
                executorService.submit(clientHandler);

            } catch (IOException e) {
                if (!isRunning) {
                    System.out.println("TCP socket closed");
                } else {
                    System.err.println("Error accepting TCP connection: " + e.getMessage());
                }
            }
        }
    }

    public void stop() {
        isRunning = false;
        try {
            serverSocket.close();
            executorService.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
