package com.kartgame.server.game;

public class Collectible {
    public static final float RADIUS = 16f;
    private final float x;
    private final float y;

    public Collectible(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }
}
