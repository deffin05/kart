package com.kartgame.server.game;

public class KartState {
    public static final float HIT_RADIUS = 24f;

    private int playerToken;
    private float x;
    private float y;
    private final BoundingBox boundingBox;
    private int hp = 100;
    private float angle = 0.0f;
    private float speed = 0.0f;

    public KartState(int playerToken, float x, float y, BoundingBox boundingBox) {
        this.playerToken = playerToken;
        this.x = x;
        this.y = y;
        this.boundingBox = boundingBox;
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

    public static class BoundingBox {
        private final float top = 680f;
        private final float bottom = 40f;
        private final float right;
        private final float left;
        public BoundingBox(float left, float right) {
            this.right = right;
            this.left = left;
        }

        public float getTop() {
            return top;
        }

        public float getBottom() {
            return bottom;
        }

        public float getRight() {
            return right;
        }

        public float getLeft() {
            return left;
        }
    }

    public BoundingBox getBoundingBox() {
        return boundingBox;
    }
}
