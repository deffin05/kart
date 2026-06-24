package com.kartgame.client;

import com.kartgame.common.protocol.Packet;
import com.kartgame.common.protocol.PacketRegistry;
import com.kartgame.common.protocol.packets.C2S_UserInput;
import com.kartgame.common.protocol.packets.S2C_WorldState;
import com.kartgame.common.security.AESEngine;
import com.kartgame.server.network.UDPServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class UDPClient {
    private DatagramSocket udpSocket;
    private InetAddress serverAddress;
    private final int serverUdpPort = UDPServer.PORT;
    
    private int playerToken;
    private AESEngine aesEngine;
    private WorldStateListener listener; // For passing world state packets to the controller. Controller should implement the interface
    
    private volatile boolean running = false;
    private static final int BUFFER_SIZE = 1024;

    public void start(String serverIp, int playerToken, AESEngine aesEngine) throws Exception {
        // Call when S2C_GameStartedPacket is received
        this.serverAddress = InetAddress.getByName(serverIp);
        this.playerToken = playerToken;
        this.aesEngine = aesEngine;

        this.udpSocket = new DatagramSocket(); 
        this.running = true;

        Thread receiveThread = new Thread(this::receiveLoop);
        receiveThread.setDaemon(true);
        receiveThread.start();

        System.out.println("UDP Client started on local port " + udpSocket.getLocalPort());

        sendInput(false, false, false, false);
    }

    // For passing world state packets to the controller. Controller should implement the interface
    public void setWorldStateListener(WorldStateListener listener) {
        this.listener = listener;
    }

    public void sendInput(boolean w, boolean a, boolean s, boolean d) {
        if (udpSocket == null || udpSocket.isClosed() || !running) return;

        try {
            C2S_UserInput packet = new C2S_UserInput(w, s, a, d);
            packet.setPlayerToken(this.playerToken);

            byte[] plaintextPayload = packet.serializePayload();
            byte[] encryptedPayload = aesEngine.encrypt(plaintextPayload);
            byte[] packetBytes = packet.serialize(encryptedPayload);

            DatagramPacket datagram = new DatagramPacket(
                packetBytes,
                packetBytes.length,
                serverAddress,
                serverUdpPort
            );
            udpSocket.send(datagram);

        } catch (Exception e) {
            System.err.println("Failed to send UDP input: " + e.getMessage());
        }
    }

    private void receiveLoop() {
        byte[] buffer = new byte[BUFFER_SIZE];

        while (running && !udpSocket.isClosed()) {
            try {
                DatagramPacket incomingPacket = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(incomingPacket);

                byte[] rawData = Arrays.copyOfRange(buffer, incomingPacket.getOffset(),
                        incomingPacket.getLength() + incomingPacket.getOffset());

                ByteBuffer byteBuf = ByteBuffer.wrap(rawData);
                if (byteBuf.remaining() < Packet.HEADER_SIZE) continue;

                byte magic = byteBuf.get();
                byte typeId = byteBuf.get();
                int token = byteBuf.getInt();
                short payloadLength = byteBuf.getShort();

                if (magic != Packet.MAGIC_BYTE) continue;

                byte[] encryptedPayload = new byte[payloadLength];
                byteBuf.get(encryptedPayload);
                byte[] decryptedPayload = aesEngine.decrypt(encryptedPayload);

                short plaintextLength = (short) decryptedPayload.length;
                ByteBuffer plaintextBuffer = ByteBuffer.allocate(Packet.HEADER_SIZE + plaintextLength);
                plaintextBuffer.put(magic);
                plaintextBuffer.put(typeId);
                plaintextBuffer.putInt(token);
                plaintextBuffer.putShort(plaintextLength);
                plaintextBuffer.put(decryptedPayload);
                plaintextBuffer.flip();

                Packet packet = PacketRegistry.parse(plaintextBuffer);

                if (packet instanceof S2C_WorldState && listener != null) {
                    listener.onWorldStateReceived((S2C_WorldState) packet);
                }

            } catch (IOException e) {
                if (running) {
                    System.err.println("UDP Client socket closed cleanly.");
                }
            } catch (Exception e) {
                // Drop bad/corrupted packets silently
            }
        }
    }

    public void stop() {
        this.running = false;
        if (udpSocket != null && !udpSocket.isClosed()) {
            udpSocket.close();
        }
    }
}