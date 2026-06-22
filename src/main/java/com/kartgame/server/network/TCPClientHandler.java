package com.kartgame.server.network;

import com.kartgame.common.protocol.Packet;
import com.kartgame.common.security.AESEngine;
import com.kartgame.common.security.RSAEngineServer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;

public class TCPClientHandler implements Runnable {
    private final RSAEngineServer rsaEngine;
    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;

    private AESEngine aesEngine;

    public TCPClientHandler(Socket socket, RSAEngineServer rsaEngine) throws IOException {
        this.rsaEngine = rsaEngine;
        this.socket = socket;
        this.out = new DataOutputStream(socket.getOutputStream());
        this.in = new DataInputStream(socket.getInputStream());
    }

    @Override
    public void run() {
        try {
            executeHandshake();
            while (!socket.isClosed() && !Thread.currentThread().isInterrupted()) {
                byte[] header = new byte[Packet.HEADER_SIZE];
                in.readFully(header);
            }
        } catch (EOFException e) {
            System.out.println("Client disconnected: " + socket);
        } catch (IOException e) {
            System.err.println("Client connection lost: " + e.getMessage());
        } finally {
            close();
        }
    }

    private void executeHandshake() throws IOException {
        /* Write RSA key length (4 bytes) + RSA key
        *  Receive AES key length (4 bytes) + AES key (encrypted)
        * */
        byte[] rsaPublicKey = rsaEngine.getPublicKey();

        out.writeInt(rsaPublicKey.length);
        out.write(rsaPublicKey);
        out.flush();

        int aesLength = in.readInt();
        byte[] encryptedAesKey = new byte[aesLength];
        in.readFully(encryptedAesKey);

        byte[] aesKey = rsaEngine.decryptAESKey(encryptedAesKey);
        this.aesEngine = new AESEngine(aesKey);
    }

    public void close() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing socket for " + socket.getRemoteSocketAddress());
        }
    }
}
