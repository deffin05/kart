package com.kartgame.client;

import com.kartgame.common.protocol.packets.C2S_LoginPacket;
import com.kartgame.common.security.AESEngine;
import com.kartgame.common.security.RSAEngineClient;
import com.kartgame.server.network.TCPServer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class TCPClient {
    private final AESEngine aesEngine;
    private final RSAEngineClient rsaEngine;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    public TCPClient() throws IOException {
        this.aesEngine = new AESEngine();
        this.rsaEngine = new RSAEngineClient();
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
    }

    public void sendLogin(String username, String password) throws IOException {
        C2S_LoginPacket packet = new C2S_LoginPacket(username, password);
        byte[] payload = packet.serializePayload();
        byte[] encryptedPayload = aesEngine.encrypt(payload);
        byte[] packetBytes = packet.serialize(encryptedPayload);

        out.write(packetBytes);
        out.flush();
    }

    public void close() throws IOException {
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
