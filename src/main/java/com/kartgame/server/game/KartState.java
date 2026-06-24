package com.kartgame.server.game;

public class KartState {
    private int playerToken;
    private float x;
    private float y;
    private int hp = 100;
    private float angle = 0.0f;
    private float speed = 0.0f;

    public KartState(int playerToken, float x, float y) {
        this.playerToken = playerToken;
        this.x = x;
        this.y = y;
    }

    public int getPlayerToken() {
        return playerToken;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public float getAngle() {
        return angle;
    }

    public void setAngle(float angle) {
        this.angle = angle;
    }
}
