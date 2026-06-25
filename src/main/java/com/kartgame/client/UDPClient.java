package com.kartgame.client;

import com.kartgame.common.protocol.Packet;
import com.kartgame.common.protocol.PacketRegistry;
import com.kartgame.common.protocol.PacketType;
import com.kartgame.common.protocol.packets.C2S_UserInput;
import com.kartgame.common.protocol.packets.S2C_WorldState;
import com.kartgame.common.security.AESEngine;
import com.kartgame.server.network.UDPServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

public class UDPClient {
    private final AESEngine aesEngine;
    private final IntSupplier tokenSupplier;
    private final InetAddress serverAddress;

    private final ScheduledExecutorService pingScheduler = Executors.newSingleThreadScheduledExecutor();

    private DatagramSocket socket;
    private volatile boolean running;
    private Thread readerThread;

    private Consumer<S2C_WorldState> worldStateListener;

    public UDPClient(AESEngine aesEngine, IntSupplier tokenSupplier) throws IOException {
        this.aesEngine = aesEngine;
        this.tokenSupplier = tokenSupplier;
        this.serverAddress = InetAddress.getLoopbackAddress();
        this.socket = new DatagramSocket();
    }

    public void start() {
        if (running) {
            return;
        }

        running = true;

        readerThread = new Thread(this::readLoop, "udp-client-reader");
        readerThread.setDaemon(true);
        readerThread.start();

        pingScheduler.scheduleAtFixedRate(() -> {
            try {
                sendPing();
            } catch (IOException ignored) {
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    public void setWorldStateListener(Consumer<S2C_WorldState> listener) {
        this.worldStateListener = listener;
    }

    public void sendInput(boolean accelerating, boolean slowing, boolean left, boolean right) throws IOException {
        C2S_UserInput packet = new C2S_UserInput(accelerating, slowing, left, right);
        sendPacket(packet);
    }

    private void sendPing() throws IOException {
        int token = tokenSupplier.getAsInt();
        if (token <= 0) {
            return;
        }

        ByteBuffer header = ByteBuffer.allocate(Packet.HEADER_SIZE);
        header.put(Packet.MAGIC_BYTE);
        header.put(PacketType.PING.getId());
        header.putInt(token);
        header.putShort((short) 0);
        sendRaw(header.array());
    }

    private void sendPacket(Packet packet) throws IOException {
        int token = tokenSupplier.getAsInt();
        if (token <= 0) {
            return;
        }

        packet.setPlayerToken(token);
        byte[] payload = packet.serializePayload();
        byte[] encryptedPayload = aesEngine.encrypt(payload);
        byte[] packetBytes = packet.serialize(encryptedPayload);
        sendRaw(packetBytes);
    }

    private void sendRaw(byte[] bytes) throws IOException {
        DatagramPacket datagram = new DatagramPacket(bytes, bytes.length, serverAddress, UDPServer.PORT);
        socket.send(datagram);
    }

    private void readLoop() {
        byte[] buffer = new byte[2048];

        while (running && !socket.isClosed()) {
            try {
                DatagramPacket datagram = new DatagramPacket(buffer, buffer.length);
                socket.receive(datagram);

                byte[] received = Arrays.copyOfRange(
                        datagram.getData(),
                        datagram.getOffset(),
                        datagram.getOffset() + datagram.getLength()
                );

                process(received);
            } catch (SocketException e) {
                if (running) {
                    System.err.println("UDP socket error: " + e.getMessage());
                }
                break;
            } catch (Exception e) {
                if (running) {
                    System.err.println("UDP read error: " + e.getMessage());
                }
            }
        }
    }

    private void process(byte[] data) {
        ByteBuffer headerBuffer = ByteBuffer.wrap(data);
        if (headerBuffer.remaining() < Packet.HEADER_SIZE) {
            return;
        }

        byte magicByte = headerBuffer.get();
        byte typeId = headerBuffer.get();
        int playerToken = headerBuffer.getInt();
        short payloadLength = headerBuffer.getShort();

        if (magicByte != Packet.MAGIC_BYTE) {
            return;
        }

        if (typeId == PacketType.PING.getId()) {
            return;
        }

        if (payloadLength < 0 || headerBuffer.remaining() < payloadLength) {
            return;
        }

        byte[] encryptedPayload = new byte[payloadLength];
        headerBuffer.get(encryptedPayload);

        byte[] decryptedPayload = aesEngine.decrypt(encryptedPayload);

        ByteBuffer packetBuffer = ByteBuffer.allocate(Packet.HEADER_SIZE + decryptedPayload.length);
        packetBuffer.put(magicByte);
        packetBuffer.put(typeId);
        packetBuffer.putInt(playerToken);
        packetBuffer.putShort((short) decryptedPayload.length);
        packetBuffer.put(decryptedPayload);
        packetBuffer.flip();

        Packet packet = PacketRegistry.parse(packetBuffer);
        if (packet instanceof S2C_WorldState worldState && worldStateListener != null) {
            worldStateListener.accept(worldState);
        }
    }

    public void close() {
        running = false;
        pingScheduler.shutdownNow();

        if (socket != null && !socket.isClosed()) {
            socket.close();
        }

        if (readerThread != null) {
            readerThread.interrupt();
        }
    }
}
