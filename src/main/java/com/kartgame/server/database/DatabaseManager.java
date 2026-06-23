package com.kartgame.server.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
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

            // Some high scores table or something
//            String createScoresTable = """
//                        );
//                    """;
//            stmt.execute(createScoresTable);

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





}
