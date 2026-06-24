package com.kartgame.client;

import com.kartgame.client.packets.LoginResponseHandler;
import com.kartgame.client.packets.LobbyInfoHandler;
import com.kartgame.client.packets.PacketDispatcher;
import com.kartgame.common.protocol.Packet;
import com.kartgame.common.protocol.PacketRegistry;
import com.kartgame.common.protocol.PacketType;
import com.kartgame.common.protocol.packets.C2S_CreateLobbyPacket;
import com.kartgame.common.protocol.packets.C2S_JoinLobbyPacket;
import com.kartgame.common.protocol.packets.C2S_LoginPacket;
import com.kartgame.common.protocol.packets.C2S_RegisterPacket;
import com.kartgame.common.protocol.packets.S2C_LobbyInfoPacket;
import com.kartgame.common.protocol.packets.S2C_LoginResponse;
import com.kartgame.common.security.AESEngine;
import com.kartgame.common.security.RSAEngineClient;
import com.kartgame.server.network.TCPServer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

public class TCPClient {
    private final AESEngine aesEngine;
    private final RSAEngineClient rsaEngine;
    private final PacketDispatcher packetDispatcher;
    private final ScheduledExecutorService pingScheduler = Executors.newSingleThreadScheduledExecutor();
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private Thread readerThread;
    private volatile boolean running = true;
    private Consumer<S2C_LoginResponse> loginResponseListener;
    private Consumer<S2C_LobbyInfoPacket> lobbyInfoListener;
    private int loginTag = -1;

    public TCPClient() throws IOException {
        this.aesEngine = new AESEngine();
        this.rsaEngine = new RSAEngineClient();
        this.packetDispatcher = new PacketDispatcher();
        this.packetDispatcher.registerHandler(PacketType.S2C_LOGIN_RESPONSE, new LoginResponseHandler());
        this.packetDispatcher.registerHandler(PacketType.S2C_LOBBY_INFO, new LobbyInfoHandler());
        connect();
    }

    private void connect() throws IOException {
        InetAddress address = InetAddress.getLoopbackAddress();
        socket = new Socket(address, TCPServer.PORT);
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());

        int keyLength = in.readInt();
        byte[] rsaKey = new byte[keyLength];
        in.readFully(rsaKey);

        rsaEngine.loadServerPublicKey(rsaKey);

        byte[] aesKey = aesEngine.getRawKey();
        byte[] aesKeyEncrypted = rsaEngine.encryptAESKey(aesKey);
        out.writeInt(aesKeyEncrypted.length);
        out.write(aesKeyEncrypted);
        out.flush();

        pingScheduler.submit(() -> {
            try {
                sendPing();
            } catch (IOException e) {
                System.err.println("Failed to send ping");
            }
        });

        startReader();
    }

    public void setLoginResponseListener(Consumer<S2C_LoginResponse> listener) {
        this.loginResponseListener = listener;
    }

    public Consumer<S2C_LoginResponse> getLoginResponseListener() {
        return loginResponseListener;
    }

    public void setLobbyInfoListener(Consumer<S2C_LobbyInfoPacket> listener) {
        this.lobbyInfoListener = listener;
    }

    public Consumer<S2C_LobbyInfoPacket> getLobbyInfoListener() {
        return lobbyInfoListener;
    }

    private void startReader() {
        readerThread = new Thread(() -> {
            try {
                while (running && !socket.isClosed()) {
                    byte[] headerBytes = new byte[Packet.HEADER_SIZE];
                    try {
                        in.readFully(headerBytes);
                    } catch (EOFException e) {
                        break;
                    }

                    ByteBuffer headerBuffer = ByteBuffer.wrap(headerBytes);
                    byte magicByte = headerBuffer.get();
                    byte typeId = headerBuffer.get();
                    int playerToken = headerBuffer.getInt();
                    short payloadLength = headerBuffer.getShort();

                    if (magicByte != Packet.MAGIC_BYTE) {
                        throw new SecurityException("Invalid packet received: invalid magic byte");
                    }

                    byte[] payloadBytes = new byte[payloadLength];
                    in.readFully(payloadBytes);
                    byte[] decryptedPayload = aesEngine.decrypt(payloadBytes);

                    short decryptedPayloadLength = (short) decryptedPayload.length;
                    ByteBuffer packetBuffer = ByteBuffer.allocate(Packet.HEADER_SIZE + decryptedPayloadLength);
                    packetBuffer.put(magicByte);
                    packetBuffer.put(typeId);
                    packetBuffer.putInt(playerToken);
                    packetBuffer.putShort(decryptedPayloadLength);
                    packetBuffer.put(decryptedPayload);
                    packetBuffer.flip();

                    Packet packet = PacketRegistry.parse(packetBuffer);
                    if (packet != null) {
                        packetDispatcher.dispatch(packet, this);
                    }
                }
            } catch (IOException e) {
                if (running) {
                    System.err.println("Error reading packet: " + e.getMessage());
                }
            } catch (Exception e) {
                System.err.println("Unexpected packet reader error: " + e.getMessage());
                e.printStackTrace();
            }
        });
        readerThread.setDaemon(true);
        readerThread.start();
    }

    public void sendLogin(String username, String password) throws IOException {
        C2S_LoginPacket packet = new C2S_LoginPacket(username, password);
        sendPacket(packet);
    }

    public void sendRegister(String username, String password) throws IOException {
        C2S_RegisterPacket packet = new C2S_RegisterPacket(username, password);
        sendPacket(packet);
    }

    public void sendCreateLobby() throws IOException {
        C2S_CreateLobbyPacket packet = new C2S_CreateLobbyPacket();
        sendPacket(packet);
    }

    public void sendJoinLobby(int lobbyId) throws IOException {
        C2S_JoinLobbyPacket packet = new C2S_JoinLobbyPacket(lobbyId);
        sendPacket(packet);
    }

    private void sendPacket(Packet packet) throws IOException {
        if (loginTag > 0) {
            packet.setPlayerToken(loginTag);
        }

        byte[] payload = packet.serializePayload();
        byte[] encryptedPayload = aesEngine.encrypt(payload);
        byte[] packetBytes = packet.serialize(encryptedPayload);

        out.write(packetBytes);
        out.flush();
    }

    public void sendPlayerInfoReq() throws IOException {
        // TODO: Write request sender
    }

    public void sendLobbyInfoReq() throws IOException {
        // TODO: Write request sender
    }

    public int getLoginTag() {
        return loginTag;
    }

    public void setLoginTag(int loginTag) {
        this.loginTag = loginTag;
    }

    private void sendPing() throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(8);
        buf.put(Packet.MAGIC_BYTE);
        buf.put(PacketType.PING.getId());
        buf.putInt(loginTag);
        buf.putShort((short) 0);
        buf.flip();
        byte[] pingPacket = buf.array();

        out.write(pingPacket);
        out.flush();
    }

    public void close() throws IOException {
        running = false;
        if (readerThread != null) {
            readerThread.interrupt();
        }
        if (!pingScheduler.isShutdown()) {
            pingScheduler.shutdown();
        }
        if (out != null) {
            out.close();
        }
        if (in != null) {
            in.close();
        }
        if (socket != null) {
            socket.close();
        }
    }
}
