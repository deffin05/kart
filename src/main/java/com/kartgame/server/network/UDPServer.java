package com.kartgame.server.network;

import com.kartgame.common.protocol.Packet;
import com.kartgame.server.lobby.LobbyManager;
import com.kartgame.server.lobby.Player;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class UDPServer implements Runnable {
    public static int PORT = 13488;

    private final DatagramSocket socket;
    private final LobbyManager lobbyManager;

    private boolean running = true;

    public UDPServer(LobbyManager lobbyManager) throws SocketException {
        socket = new DatagramSocket(PORT);
        this.lobbyManager = lobbyManager;

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

                processDatagram(bytesReceived, incomingDatagram.getAddress(), incomingDatagram.getPort());

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
            ;

            byte typeId = buffer.get();
            int playerToken = buffer.getInt();
            short payloadLength = buffer.getShort();

            Player player = lobbyManager.getPlayer(playerToken);
            if (player == null) return;

            if (!player.isUdpBound()) {
                player.setUdpAddress(senderIp);
                player.setUdpPort(senderPort);
            } else {
                if (!player.getUdpAddress().equals(senderIp) || player.getUdpPort() != senderPort) {
                    System.err.println("UDP from the wrong address received.");
                    return;
                }
            }

            byte[] encryptedPayload = new byte[payloadLength];
            buffer.get(encryptedPayload);
            byte[] decryptedPayload = player.getTcpHandler().getAesEngine().decrypt(encryptedPayload);

            Integer lobbyId = player.getCurrentLobbyId();
            if (lobbyId != null) {
                // TODO: process something further
            }
        } catch (SecurityException e) {
            System.err.println("Bad packet received");
        }
    }

    public void stop() {
        this.running = false;
        socket.close();
    }
}
