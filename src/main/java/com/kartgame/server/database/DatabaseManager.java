package com.kartgame.server.database;

import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:game.db";

    private ExecutorService dbExecutor;

    // Database management

    public void init() {
        this.dbExecutor = Executors.newFixedThreadPool(2);
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA busy_timeout=5000;");

            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS Users (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            username TEXT UNIQUE NOT NULL,
                            password TEXT NOT NULL
                        );
                    """);

            String createScoresTable = """
                    CREATE TABLE IF NOT EXISTS BattleLog (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        winner_id INTEGER NOT NULL,
                        battle_date DATETIME NOT NULL,
                        duration_milis INTEGER NOT NULL,
                        FOREIGN KEY (winner_id) REFERENCES Users(id)
                            ON UPDATE CASCADE
                            ON DELETE NO ACTION
                    )
                    """;
            stmt.execute(createScoresTable);

            String logToUserTable = """
                    CREATE TABLE IF NOT EXISTS LogUser (
                        player_id INTEGER NOT NULL,
                        log_id INTEGER NOT NULL,
                        PRIMARY KEY (player_id, log_id)
                    )
                    """;
            stmt.execute(logToUserTable);

            System.out.println("Database initialized successfully.");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    public void execute(Runnable task) {
        if (dbExecutor != null && !dbExecutor.isShutdown()) {
            dbExecutor.submit(task);
        }
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public void shutdown() {
        System.out.println("Shutting down the database...");
        if (dbExecutor != null) {
            dbExecutor.shutdown();
            try {
                if (!dbExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    dbExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                dbExecutor.shutdownNow();
            }
        }
    }

    // Database queries

    public boolean authenticate(String username, String password) {
        String query = "SELECT password FROM Users WHERE username = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, username.trim());

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password");
                    return PasswordHasher.verifyPassword(password, storedHash);
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to access the database: " + e.getMessage());
        }
        return false;
    }

    public boolean registerUser(String username, String password) {
        String query = "INSERT INTO Users (username, password) VALUES (?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            String passwordHash = PasswordHasher.hashPassword(password);

            pstmt.setString(1, username);
            pstmt.setString(2, passwordHash);

            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void insertBattleLog(int winner_id, long time, List<Integer> players) {
        String queryBattleLog = "INSERT INTO BattleLog (winner_id, battle_date, duration_milis) VALUES (?, ?, ?)";
        Integer log_id = null;

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(queryBattleLog)) {

            LocalDateTime now = LocalDateTime.now();
            pstmt.setInt(1, winner_id);
            pstmt.setObject(2, now);
            pstmt.setLong(3, time);

            log_id = pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (log_id == null) return;;
        String queryLogUser = "INSERT INTO LogUser (player_id, log_id) VALUES (?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(queryLogUser)) {

            for (int playerId : players) {
                pstmt.setInt(1, playerId);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public int getUserId(String username) {
        String query = "SELECT id FROM Users WHERE username = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Invalid user requested");
        }
        return -1;
    }
}
