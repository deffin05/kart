package com.kartgame.server.network;

import com.kartgame.common.protocol.Packet;
import com.kartgame.common.protocol.PacketRegistry;
import com.kartgame.common.protocol.PacketType;
import com.kartgame.server.lobby.LobbyManager;
import com.kartgame.server.lobby.Player;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class UDPServer implements Runnable {
    public static int PORT = 13488;

    private final DatagramSocket socket;
    private final LobbyManager lobbyManager;
    private final ExecutorService udpThreadPool;

    private boolean running = true;

    public UDPServer(LobbyManager lobbyManager) throws SocketException {
        socket = new DatagramSocket(PORT);
        this.lobbyManager = lobbyManager;
        this.udpThreadPool = Executors.newFixedThreadPool(8);

        System.out.println("UDP server started");
    }

    @Override
    public void run() {
        byte[] buffer = new byte[1024];

        while (running && !socket.isClosed()) {
            try {
                DatagramPacket incomingDatagram = new DatagramPacket(buffer, buffer.length);

                socket.receive(incomingDatagram);
                byte[] bytesReceived = Arrays.copyOfRange(buffer, incomingDatagram.getOffset(),
                        incomingDatagram.getLength() + incomingDatagram.getOffset());

                udpThreadPool.submit(() -> processDatagram(bytesReceived, incomingDatagram.getAddress(), incomingDatagram.getPort()));
            } catch (IOException e) {
                if (running) {
                    System.err.println("UDP error receiving packet: " + e.getMessage());
                }
            }
        }
    }

    private void processDatagram(byte[] data, InetAddress senderIp, int senderPort) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(data);

            if (buffer.remaining() < Packet.HEADER_SIZE) return;

            byte magic = buffer.get();
            if (magic != Packet.MAGIC_BYTE) {
                throw new SecurityException("Invalid magic byte received");
            }

            byte typeId = buffer.get();
            int playerToken = buffer.getInt();
            short payloadLength = buffer.getShort();

            Player player = lobbyManager.getPlayer(playerToken);
            if (player == null) return;

            if (!player.isUdpBound()) {
                player.setUdpAddress(senderIp);
                player.setUdpPort(senderPort);
            } else if (!player.getUdpAddress().equals(senderIp) || player.getUdpPort() != senderPort) {
                System.err.println("UDP from the wrong address received.");
                return;
            }

            if (typeId == PacketType.PING.getId()) return;

            byte[] encryptedPayload = new byte[payloadLength];
            buffer.get(encryptedPayload);
            byte[] decryptedPayload = player.getTcpHandler().getAesEngine().decrypt(encryptedPayload);

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
                // TODO: UDP packet dispatcher
            }

        } catch (SecurityException e) {
            System.err.println("Bad packet received");
        }
    }

    public void stop() {
        this.running = false;
        socket.close();

        udpThreadPool.shutdown();
        try {
            if (!udpThreadPool.awaitTermination(2, TimeUnit.SECONDS)) {
                udpThreadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            udpThreadPool.shutdownNow();
        }
    }
}
