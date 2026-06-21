package com.kartgame.common.encryption;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class AESEngine {
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int KEY_SIZE_BITS = 128;
    private static final int IV_SIZE_BYTES = 12;
    private static final int TAG_SIZE_BITS = 128;

    private static final SecureRandom secureRandom = new SecureRandom();
    private final SecretKey secretKey;

    public AESEngine() {
        try {
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(KEY_SIZE_BITS);
            this.secretKey = generator.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public AESEngine(byte[] rawKey) {
        this.secretKey = new SecretKeySpec(rawKey, "AES");
    }

    public byte[] getRawKey() {
        return secretKey.getEncoded();
    }

    public byte[] encrypt(byte[] data) {
        byte[] IV = new byte[IV_SIZE_BYTES];
        secureRandom.nextBytes(IV);

        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_SIZE_BITS, IV);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

            byte[] encryptedData = cipher.doFinal(data);

            byte[] encryptedFrame = new byte[IV.length + encryptedData.length];
            System.arraycopy(IV, 0, encryptedFrame, 0, IV.length);
            System.arraycopy(encryptedData, 0, encryptedFrame, IV.length, encryptedData.length);

            return encryptedFrame;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                 InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }


    public byte[] decrypt(byte[] encryptedFrame) {
        if (encryptedFrame.length < IV_SIZE_BYTES) {
            throw new IllegalArgumentException("Encrypted frame is too short.");
        }

        byte[] iv = new byte[IV_SIZE_BYTES];
        System.arraycopy(encryptedFrame, 0, iv, 0, iv.length);

        int encryptedDataLength = encryptedFrame.length - IV_SIZE_BYTES;
        byte[] encryptedData = new byte[encryptedDataLength];
        System.arraycopy(encryptedFrame, iv.length, encryptedData, 0, encryptedDataLength);

        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_SIZE_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            return cipher.doFinal(encryptedData);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                 InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }
}
