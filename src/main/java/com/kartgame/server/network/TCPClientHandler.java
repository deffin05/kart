package com.kartgame.server.network;

import com.kartgame.common.protocol.Packet;
import com.kartgame.common.protocol.PacketRegistry;
import com.kartgame.common.protocol.PacketType;
import com.kartgame.common.security.AESEngine;
import com.kartgame.common.security.RSAEngineServer;
import com.kartgame.server.packets.PacketDispatcher;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;

public class TCPClientHandler implements Runnable {
    private final RSAEngineServer rsaEngine;
    private final PacketDispatcher dispatcher;
    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;

    private boolean isRunning = true;
    private AESEngine aesEngine;

    private int playerToken = -1;

    public TCPClientHandler(Socket socket, RSAEngineServer rsaEngine, PacketDispatcher dispatcher) throws IOException {
        this.rsaEngine = rsaEngine;
        this.dispatcher = dispatcher;
        this.socket = socket;
        this.out = new DataOutputStream(socket.getOutputStream());
        this.in = new DataInputStream(socket.getInputStream());
    }

    @Override
    public void run() {
        try {
            socket.setSoTimeout(10_000); // Timeout for handshake
            executeHandshake();

            socket.setSoTimeout(60_000);
            while (isRunning && !socket.isClosed() && !Thread.currentThread().isInterrupted()) {
                byte[] headerBytes = new byte[Packet.HEADER_SIZE];
                in.readFully(headerBytes);

                ByteBuffer headerBuffer = ByteBuffer.wrap(headerBytes);
                byte magic = headerBuffer.get();
                byte typeId = headerBuffer.get();
                int playerToken = headerBuffer.getInt();
                short payloadLength = headerBuffer.getShort();

                if (typeId == PacketType.PING.getId()) {
                    continue;
                }

                if (magic != Packet.MAGIC_BYTE) {
                    throw new SecurityException("Invalid Magic Byte.");
                }

                if (this.playerToken != -1 && playerToken != this.playerToken) {
                    throw new SecurityException("Player token mismatch.");
                }
                byte[] payloadBytes = new byte[payloadLength];
                in.readFully(payloadBytes);

                byte[] decryptedPayload = aesEngine.decrypt(payloadBytes);

                short decryptedPayloadLength = (short) decryptedPayload.length;
                ByteBuffer packetBuffer = ByteBuffer.allocate(Packet.HEADER_SIZE + decryptedPayloadLength);

                packetBuffer.put(magic);
                packetBuffer.put(typeId);
                packetBuffer.putInt(playerToken);
                packetBuffer.putShort(decryptedPayloadLength);

                packetBuffer.put(decryptedPayload);
                packetBuffer.flip();

                Packet packet = PacketRegistry.parse(packetBuffer);

                if (packet != null) {
                    dispatcher.dispatch(packet, this);
                }
            }
        } catch (SecurityException e) {
            System.err.println("Security violation: " + e.getMessage());
        } catch (SocketTimeoutException e) {
            System.out.println("Client timed out: " + socket);
        } catch (EOFException e) {
            System.out.println("Client disconnected: " + socket);
        } catch (IOException e) {
            System.err.println("Client connection lost: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
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

    public void sendPacket(Packet packet) {
        synchronized (this.out) {
            try {
                packet.setPlayerToken(this.playerToken);

                byte[] payloadBytes = packet.serializePayload();

                byte[] payloadEncrypted = aesEngine.encrypt(payloadBytes);
                byte[] packetBytes = packet.serialize(payloadEncrypted);

                out.write(packetBytes);
                out.flush();
            } catch (IOException e) {
                System.err.println("Failed to send packet: " + e.getMessage());
                close();
            }
        }
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

    public boolean isAuthenticated() {
        return playerToken != -1;
    }

    public Socket getSocket() {
        return socket;
    }

    public int getPlayerToken() {
        return playerToken;
    }

    public void setPlayerToken(int playerToken) {
        this.playerToken = playerToken;
    }

    public AESEngine getAesEngine() {
        return aesEngine;
    }
}
