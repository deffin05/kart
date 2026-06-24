package com.kartgame.common.protocol.packets;

import com.kartgame.common.protocol.Packet;
import com.kartgame.common.protocol.PacketType;
import com.kartgame.server.game.Collectible;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class S2C_WorldState extends Packet {
    private List<KartData> karts;
    private List<Collectible> collectibles;

    public S2C_WorldState(Collection<KartData> karts, Collection<Collectible> collectibles) {
        super(PacketType.S2C_WORLD_STATE);
        this.karts = new ArrayList<>(karts);
        this.collectibles = new ArrayList<>(collectibles);
    }

    public S2C_WorldState() {
        super(PacketType.S2C_WORLD_STATE);
    }

    @Override
    protected void writePayload(ByteBuffer buffer) {
        buffer.put((byte) karts.size());

        for (KartData kart : karts) {
            buffer.putInt(kart.getPlayerToken());
            buffer.putFloat(kart.getX());
            buffer.putFloat(kart.getY());
            buffer.putFloat(kart.getAngle());
            buffer.putInt(kart.getHp());
        }

        buffer.put((byte) collectibles.size());
        for (Collectible col : collectibles) {
            buffer.putFloat(col.getX());
            buffer.putFloat(col.getY());
        }
    }

    @Override
    protected void readPayload(ByteBuffer buffer, short length) {
        int kartCount = buffer.get() & 0xFF;

        this.karts = new ArrayList<>(kartCount);

        for (int i = 0; i < kartCount; i++) {
            int token = buffer.getInt();
            float x = buffer.getFloat();
            float y = buffer.getFloat();
            float angle = buffer.getFloat();
            int hp = buffer.getInt();

            karts.add(new KartData(token, x, y, angle, hp));
        }

        int collectibleCount = buffer.get() & 0xFF;

        this.collectibles = new ArrayList<>(collectibleCount);
        for (int i = 0; i < collectibleCount; i++) {
            this.collectibles.add(new Collectible(buffer.getFloat(), buffer.getFloat()));
        }
    }

    public static class KartData {
        private final int playerToken;
        private final float x;
        private final float y;
        private final float angle;
        private final int hp;

        public KartData(int playerToken, float x, float y, float angle, int hp) {
            this.playerToken = playerToken;
            this.x = x;
            this.y = y;
            this.angle = angle;
            this.hp = hp;
        }

        public int getPlayerToken() {
            return playerToken;
        }

        public float getX() {
            return x;
        }

        public float getY() {
            return y;
        }

        public float getAngle() {
            return angle;
        }

        public int getHp() {
            return hp;
        }
    }

    public List<KartData> getKarts() {
        return karts;
    }

    public List<Collectible> getCollectibles() {
        return collectibles;
    }
}
