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
        seedDefaults();
    }

    public static GlobalStorageService getInstance() {
        return INSTANCE;
    }

    public synchronized double getBalance() {
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

    public synchronized void setBalance(double balance) {
        String sql = "UPDATE profile SET balance = ? WHERE id = 1";
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDouble(1, balance);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to update balance.", exception);
        }
    }

    public synchronized void addScore(String gameKey, int score, double multiplier, double wager, double payout) {
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

    public synchronized int getBestScore(String gameKey) {
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

    public synchronized List<ScoreEntry> getTopScores(String gameKey, int limit) {
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

    public synchronized BlackjackStats getBlackjackStats() {
        String sql = """
                SELECT hands_played, wins, losses, pushes, net_profit
                FROM blackjack_stats
                WHERE id = 1
                """;
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return new BlackjackStats(
                        resultSet.getInt("hands_played"),
                        resultSet.getInt("wins"),
                        resultSet.getInt("losses"),
                        resultSet.getInt("pushes"),
                        resultSet.getDouble("net_profit")
                );
            }
            return BlackjackStats.empty();
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to read blackjack stats.", exception);
        }
    }

    public synchronized void recordBlackjackOutcome(BlackjackOutcome outcome, double netChange) {
        BlackjackStats current = getBlackjackStats();
        int wins = current.wins() + (outcome == BlackjackOutcome.WIN ? 1 : 0);
        int losses = current.losses() + (outcome == BlackjackOutcome.LOSS ? 1 : 0);
        int pushes = current.pushes() + (outcome == BlackjackOutcome.PUSH ? 1 : 0);

        String upsert = """
                INSERT INTO blackjack_stats(id, hands_played, wins, losses, pushes, net_profit)
                VALUES (1, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    hands_played = excluded.hands_played,
                    wins = excluded.wins,
                    losses = excluded.losses,
                    pushes = excluded.pushes,
                    net_profit = excluded.net_profit
                """;
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement statement = connection.prepareStatement(upsert)) {
            statement.setInt(1, current.handsPlayed() + 1);
            statement.setInt(2, wins);
            statement.setInt(3, losses);
            statement.setInt(4, pushes);
            statement.setDouble(5, current.netProfit() + netChange);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to update blackjack stats.", exception);
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
                    game_key TEXT NOT NULL DEFAULT 'flappy',
                    score INTEGER NOT NULL,
                    multiplier REAL NOT NULL,
                    wager REAL NOT NULL,
                    payout REAL NOT NULL,
                    played_at TEXT NOT NULL
                )
                """;
        String createScoresIndex = "CREATE INDEX IF NOT EXISTS idx_scores_game_key ON scores(game_key)";
        String createBlackjackStats = """
                CREATE TABLE IF NOT EXISTS blackjack_stats (
                    id INTEGER PRIMARY KEY CHECK (id = 1),
                    hands_played INTEGER NOT NULL,
                    wins INTEGER NOT NULL,
                    losses INTEGER NOT NULL,
                    pushes INTEGER NOT NULL,
                    net_profit REAL NOT NULL
                )
                """;

        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             Statement statement = connection.createStatement()) {
            statement.execute(createProfile);
            statement.execute(createScores);
            statement.execute(createBlackjackStats);
            ensureColumn(connection, "scores", "game_key", "ALTER TABLE scores ADD COLUMN game_key TEXT NOT NULL DEFAULT 'flappy'");
            statement.execute("UPDATE scores SET game_key = 'flappy' WHERE game_key IS NULL OR game_key = ''");
            statement.execute(createScoresIndex);
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to initialize database schema.", exception);
        }
    }

    private void seedDefaults() {
        String seedProfile = "INSERT OR IGNORE INTO profile(id, balance) VALUES (1, ?)";
        String seedBlackjack = """
                INSERT OR IGNORE INTO blackjack_stats(id, hands_played, wins, losses, pushes, net_profit)
                VALUES (1, 0, 0, 0, 0, 0)
                """;
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement profile = connection.prepareStatement(seedProfile);
             Statement statement = connection.createStatement()) {
            profile.setDouble(1, STARTING_BALANCE);
            profile.executeUpdate();
            statement.execute(seedBlackjack);
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to seed local profile.", exception);
        }
    }

    private void ensureColumn(Connection connection, String table, String column, String alterSql) throws SQLException {
        if (columnExists(connection, table, column)) {
            return;
        }
        try (Statement alter = connection.createStatement()) {
            alter.execute(alterSql);
        }
    }

    private boolean columnExists(Connection connection, String table, String column) throws SQLException {
        String pragma = "PRAGMA table_info(" + table + ")";
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(pragma)) {
            while (resultSet.next()) {
                if (column.equalsIgnoreCase(resultSet.getString("name"))) {
                    return true;
                }
            }
            return false;
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
