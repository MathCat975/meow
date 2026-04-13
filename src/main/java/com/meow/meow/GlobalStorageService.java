package com.meow.meow;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class GlobalStorageService {
    private static final double STARTING_BALANCE = 100.0;
    private static final GlobalStorageService INSTANCE = new GlobalStorageService();

    private final String jdbcUrl;

    private GlobalStorageService() {
        this.jdbcUrl = initDatabasePath();
        initializeSchema();
    }

    public static GlobalStorageService getInstance() {
        return INSTANCE;
    }

    public double getBalance() {
        String sql = "SELECT balance FROM profile WHERE id = 1";
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getDouble("balance");
            }
            return STARTING_BALANCE;
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to read balance.", exception);
        }
    }

    public void setBalance(double balance) {
        String sql = "UPDATE profile SET balance = ? WHERE id = 1";
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDouble(1, balance);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to update balance.", exception);
        }
    }

    public void addScore(String gameKey, int score, double multiplier, double wager, double payout) {
        String sql = """
                INSERT INTO scores(game_key, score, multiplier, wager, payout, played_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, gameKey);
            statement.setInt(2, score);
            statement.setDouble(3, multiplier);
            statement.setDouble(4, wager);
            statement.setDouble(5, payout);
            statement.setString(6, LocalDateTime.now().toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to save score.", exception);
        }
    }

    public int getBestScore(String gameKey) {
        String sql = "SELECT COALESCE(MAX(score), 0) AS best_score FROM scores WHERE game_key = ?";
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, gameKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("best_score");
                }
                return 0;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to read best score.", exception);
        }
    }

    public List<ScoreEntry> getTopScores(String gameKey, int limit) {
        String sql = """
                SELECT score, multiplier, wager, payout, played_at
                FROM scores
                WHERE game_key = ?
                ORDER BY score DESC, multiplier DESC, played_at ASC
                LIMIT ?
                """;
        List<ScoreEntry> scores = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, gameKey);
            statement.setInt(2, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    scores.add(new ScoreEntry(
                            resultSet.getInt("score"),
                            resultSet.getDouble("multiplier"),
                            resultSet.getDouble("wager"),
                            resultSet.getDouble("payout"),
                            LocalDateTime.parse(resultSet.getString("played_at"))
                    ));
                }
            }
            return scores;
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to read scoreboard.", exception);
        }
    }

    private void initializeSchema() {
        String createProfile = """
                CREATE TABLE IF NOT EXISTS profile (
                    id INTEGER PRIMARY KEY CHECK (id = 1),
                    balance REAL NOT NULL
                )
                """;
        String createScores = """
                CREATE TABLE IF NOT EXISTS scores (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    game_key TEXT NOT NULL,
                    score INTEGER NOT NULL,
                    multiplier REAL NOT NULL,
                    wager REAL NOT NULL,
                    payout REAL NOT NULL,
                    played_at TEXT NOT NULL
                )
                """;
        String createScoresIndex = "CREATE INDEX IF NOT EXISTS idx_scores_game_key ON scores(game_key)";
        String seedProfile = "INSERT OR IGNORE INTO profile(id, balance) VALUES (1, ?)";

        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             Statement statement = connection.createStatement()) {
            statement.execute(createProfile);
            statement.execute(createScores);
            statement.execute(createScoresIndex);
            try (PreparedStatement seedStatement = connection.prepareStatement(seedProfile)) {
                seedStatement.setDouble(1, STARTING_BALANCE);
                seedStatement.executeUpdate();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to initialize database schema.", exception);
        }
    }

    private String initDatabasePath() {
        try {
            Path dbDir = Paths.get(System.getProperty("user.home"), ".meowcasino");
            Files.createDirectories(dbDir);
            Path dbPath = dbDir.resolve("meowcasino.db");
            return "jdbc:sqlite:" + dbPath;
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to prepare SQLite database.", exception);
        }
    }
}
