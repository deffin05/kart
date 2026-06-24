package com.kartgame.server.packets;

import com.kartgame.common.protocol.packets.C2S_RecentGamesRequestPacket;
import com.kartgame.common.protocol.packets.S2C_RecentGamesResponsePacket;
import com.kartgame.server.database.DatabaseManager;
import com.kartgame.server.network.TCPClientHandler;

import java.util.ArrayList;
import java.util.List;

public class RecentGamesRequestHandler implements PacketHandler<C2S_RecentGamesRequestPacket> {
    private final DatabaseManager db;

    public RecentGamesRequestHandler(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public void handle(C2S_RecentGamesRequestPacket packet, TCPClientHandler client) {
        db.execute(() -> {
            try {
                List<DatabaseManager.RecentGameLog> logs = db.getRecentBattleLogs(20);
                List<S2C_RecentGamesResponsePacket.GameLogEntry> entries = new ArrayList<>(logs.size());

                for (DatabaseManager.RecentGameLog log : logs) {
                    entries.add(new S2C_RecentGamesResponsePacket.GameLogEntry(
                            log.getWinnerUsername(),
                            log.getBattleDate(),
                            log.getDurationMillis(),
                            log.getPlayersCount()
                    ));
                }

                client.sendPacket(new S2C_RecentGamesResponsePacket(entries));
            } catch (Exception e) {
                e.printStackTrace();
                client.sendPacket(new S2C_RecentGamesResponsePacket(List.of()));
            }
        });
    }
}
