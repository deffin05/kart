package com.kartgame.common.encryption;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class TestAES {
    private AESEngine engine;
    @BeforeEach
    void setup() {
        engine = new AESEngine();
    }

    @Test
    void testFullCycle() {
        byte[] arr = new byte[2000];
        Random random = new Random();
        random.nextBytes(arr);

        byte[] encryptedFrame = engine.encrypt(arr);

        assertFalse(Arrays.equals(encryptedFrame, arr));

        byte[] decodedArr = engine.decrypt(encryptedFrame);

        assertArrayEquals(arr, decodedArr);
    }
}
