package com.kartgame.common.protocol.packets;

import com.kartgame.common.protocol.Packet;
import com.kartgame.common.protocol.PacketType;

import java.nio.ByteBuffer;

public class C2S_UserInput extends Packet {
    private boolean accelerating;
    private boolean left;
    private boolean right;
    private boolean slowing;

    public C2S_UserInput(boolean accelerating, boolean slowing, boolean left, boolean right) {
        super(PacketType.C2S_USER_INPUT);
        this.accelerating = accelerating;
        this.slowing = slowing;
        this.left = left;
        this.right = right;
    }

    public C2S_UserInput() {
        super(PacketType.C2S_USER_INPUT);
    }

    @Override
    protected void writePayload(ByteBuffer buffer) {
        byte inputMask = 0;

        if (accelerating) inputMask |= (1);
        if (slowing) inputMask |= (1 << 1);
        if (left) inputMask |= (1 << 2);
        if (right) inputMask |= (1 << 3);

        buffer.put(inputMask);
    }

    @Override
    protected void readPayload(ByteBuffer buffer, short length) {
        byte inputMask = buffer.get();

        this.accelerating = (inputMask & (1)) != 0;
        this.slowing = (inputMask & (1 << 1)) != 0;
        this.left = (inputMask & (1 << 2)) != 0;
        this.right = (inputMask & (1 << 3)) != 0;
    }

    public boolean isAccelerating() {
        return accelerating;
    }

    public void setAccelerating(boolean accelerating) {
        this.accelerating = accelerating;
    }

    public boolean isLeft() {
        return left;
    }

    public void setLeft(boolean left) {
        this.left = left;
    }

    public boolean isRight() {
        return right;
    }

    public void setRight(boolean right) {
        this.right = right;
    }

    public boolean isSlowing() {
        return slowing;
    }

    public void setSlowing(boolean slowing) {
        this.slowing = slowing;
    }

    @Override
    public String toString() {
        return "C2S_UserInput{" +
                "accelerating=" + accelerating +
                ", left=" + left +
                ", right=" + right +
                ", slowing=" + slowing +
                '}';
    }
}
