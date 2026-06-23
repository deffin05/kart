package com.kartgame.server.database;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

public class PasswordHasher {
    private static final int ITERATIONS = 10000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_SIZE_BYTES = 16;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    private static final SecureRandom random = new SecureRandom();

    public String hashPassword(String password) {
        byte[] salt = new byte[SALT_SIZE_BYTES];
        random.nextBytes(salt);

        byte[] hash = pbkdf2(password.toCharArray(), salt);
        String saltBase64 = Base64.getEncoder().encodeToString(salt);
        String hashBase64 = Base64.getEncoder().encodeToString(hash);

        return saltBase64 + ":" + hashBase64;
    }

    public boolean verifyPassword(String password, String passwordHash) {
        String[] parts = passwordHash.split(":");
        if (parts.length != 2) {
            System.err.println("Invalid password hash received");
            return false;
        }

        byte[] salt = Base64.getDecoder().decode(parts[0]);
        byte[] hash = Base64.getDecoder().decode(parts[1]);

        byte[] testHash = pbkdf2(password.toCharArray(), salt);

        return MessageDigest.isEqual(hash, testHash);
    }

    private static byte[] pbkdf2(char[] password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }
}
