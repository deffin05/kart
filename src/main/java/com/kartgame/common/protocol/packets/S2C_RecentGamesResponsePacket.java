package com.kartgame.common.protocol.packets;

import com.kartgame.common.protocol.Packet;
import com.kartgame.common.protocol.PacketType;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class S2C_RecentGamesResponsePacket extends Packet {
    private List<GameLogEntry> entries = new ArrayList<>();

    public S2C_RecentGamesResponsePacket(List<GameLogEntry> entries) {
        super(PacketType.S2C_RECENT_GAMES_RESPONSE);
        this.entries = entries;
    }

    public S2C_RecentGamesResponsePacket() {
        super(PacketType.S2C_RECENT_GAMES_RESPONSE);
    }

    @Override
    protected void writePayload(ByteBuffer buffer) {
        buffer.put((byte) entries.size());
        for (GameLogEntry entry : entries) {
            writeString(buffer, entry.getWinnerUsername());
            writeString(buffer, entry.getBattleDate());
            buffer.putLong(entry.getDurationMillis());
            buffer.putInt(entry.getPlayersCount());
        }
    }

    @Override
    protected void readPayload(ByteBuffer buffer, short length) {
        int count = buffer.get() & 0xFF;
        entries = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            String winnerUsername = readString(buffer);
            String battleDate = readString(buffer);
            long durationMillis = buffer.getLong();
            int playersCount = buffer.getInt();
            entries.add(new GameLogEntry(winnerUsername, battleDate, durationMillis, playersCount));
        }
    }

    public List<GameLogEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<GameLogEntry> entries) {
        this.entries = entries;
    }

    public static class GameLogEntry {
        private final String winnerUsername;
        private final String battleDate;
        private final long durationMillis;
        private final int playersCount;

        public GameLogEntry(String winnerUsername, String battleDate, long durationMillis, int playersCount) {
            this.winnerUsername = winnerUsername;
            this.battleDate = battleDate;
            this.durationMillis = durationMillis;
            this.playersCount = playersCount;
        }

        public String getWinnerUsername() {
            return winnerUsername;
        }

        public String getBattleDate() {
            return battleDate;
        }

        public long getDurationMillis() {
            return durationMillis;
        }

        public int getPlayersCount() {
            return playersCount;
        }
    }
}
