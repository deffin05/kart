package com.kartgame.client;

import com.kartgame.common.protocol.C2S_LoginPacket;
import com.kartgame.common.security.AESEngine;
import com.kartgame.common.security.RSAEngineClient;
import com.kartgame.server.network.TCPServer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class TCPClient {
    public static void main(String[] args) throws IOException, InterruptedException {
        AESEngine aesEngine = new AESEngine();
        // Connect
        InetAddress address = InetAddress.getLoopbackAddress();
        Socket socket = new Socket(address, TCPServer.PORT);
        DataInputStream in = new DataInputStream(socket.getInputStream());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        // Handshake: get RSA
        int keyLength = in.readInt();
        byte[] rsaKey = new byte[keyLength];
        in.readFully(rsaKey);

        RSAEngineClient rsaEngine = new RSAEngineClient();
        rsaEngine.loadServerPublicKey(rsaKey);

        // Handshake: send AES
        byte[] aesKey = aesEngine.getRawKey();
        byte[] aesKeyEncrypted = rsaEngine.encryptAESKey(aesKey);
        out.writeInt(aesKeyEncrypted.length);
        out.write(aesKeyEncrypted);
        out.flush();

        // Send packet
        C2S_LoginPacket packet = new C2S_LoginPacket("login", "password123");
        byte[] payload = packet.serializePayload();
        byte[] encryptedPayload = aesEngine.encrypt(payload);

        byte[] packetBytes = packet.serialize(encryptedPayload);
        out.write(packetBytes);
        out.flush();

        socket.close();
    }
}
