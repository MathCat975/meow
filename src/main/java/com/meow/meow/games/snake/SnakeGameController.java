package com.meow.meow.games.snake;

import com.meow.meow.core.model.ScoreEntry;
import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class SnakeGameController {
    private static final int GRID_WIDTH = 30;
    private static final int GRID_HEIGHT = 20;
    private static final int CELL_SIZE = 24;
    private static final int SCOREBOARD_LIMIT = 10;
    private static final double MAX_FRAME_DELTA = 0.25;
    private static final double MULTIPLIER_PER_FOOD = 0.22;
    private static final double OBSTACLE_MULTIPLIER_BOOST = 1.50;

    @FXML
    private Canvas canvas;
    @FXML
    private Label scoreLabel;
    @FXML
    private Label timeLabel;
    @FXML
    private Label userLabel;
    @FXML
    private Label multiplierLabel;
    @FXML
    private ListView<String> scoreboard;

    @FXML
    private Label balanceLabel;
    @FXML
    private Label bestScoreLabel;
    @FXML
    private TextField wagerField;
    @FXML
    private Button cashOutButton;
    @FXML
    private Label statusLabel;
    @FXML
    private CheckBox obstaclesCheckBox;

    @FXML
    private Pane gameOverPane;
    @FXML
    private Label gameOverTitleLabel;
    @FXML
    private Label finalScoreLabel;
    @FXML
    private Label finalTimeLabel;
    @FXML
    private Label finalUserLabel;
    @FXML
    private Label finalMultiplierLabel;
    @FXML
    private Label finalWagerLabel;
    @FXML
    private Label finalPayoutLabel;

    private SnakeStorageService storage;
    private Runnable onBalanceChanged;
    private AnimationTimer loop;
    private final Random random = new Random();

    private SnakeModel snake;
    private Cell food;
    private Set<Cell> obstacles = Set.of();

    private boolean running;
    private boolean paused;
    private long gameStartNanos;
    private double accumulator;
    private int score;
    private double stepSeconds;
    private Direction queuedDirection;
    private boolean canCashOut;
    private double wager;
    private boolean obstaclesEnabled;
    private boolean awaitingFirstMove;
    private boolean betPlaced;

    @FXML
    private void initialize() {
        canvas.setWidth(GRID_WIDTH * CELL_SIZE);
        canvas.setHeight(GRID_HEIGHT * CELL_SIZE);
        if (cashOutButton != null) {
            cashOutButton.setOnAction(event -> handleActionButton());
        }
        scoreboard.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setTextFill(Color.web("#e7e9f1"));
                setStyle("-fx-background-color: transparent; -fx-padding: 6 4;");
            }
        });
    }

    public void configure(SnakeStorageService storage) {
        configure(storage, null);
    }

    public void configure(SnakeStorageService storage, Runnable onBalanceChanged) {
        this.storage = storage;
        this.onBalanceChanged = onBalanceChanged;
        showHome();
        refreshHeader();
        refreshScoreboard();
        createAndStartLoop();
    }

    public void bindInput(Scene scene) {
        scene.setOnKeyPressed(event -> {
            KeyCode code = event.getCode();
            if (code == KeyCode.P) {
                if (!running) {
                    return;
                }
                paused = !paused;
                return;
            }
            if (code == KeyCode.C) {
                cashOut();
                return;
            }
            if (!running) {
                return;
            }
            Direction next = switch (code) {
                case UP, Z -> Direction.UP;
                case RIGHT, D -> Direction.RIGHT;
                case DOWN, S -> Direction.DOWN;
                case LEFT, Q -> Direction.LEFT;
                default -> null;
            };
            if (next != null) {
                queuedDirection = next;
                if (awaitingFirstMove) {
                    tryStartOnFirstMove();
                }
            }
        });
    }

    public void onShown() {
        canvas.requestFocus();
    }

    public void shutdown() {
        if (loop != null) {
            loop.stop();
        }
    }

    @FXML
    private void closeWindow() {
        shutdown();
        Stage stage = (Stage) canvas.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void restart() {
        showHome();
    }

    private void createAndStartLoop() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        loop = new AnimationTimer() {
            private long last = 0;

            @Override
            public void handle(long now) {
                if (last == 0) {
                    last = now;
                    return;
                }
                double delta = (now - last) / 1_000_000_000.0;
                last = now;
                update(Math.min(MAX_FRAME_DELTA, delta));
                render(gc);
            }
        };
        loop.start();
    }

    private void showHome() {
        gameOverPane.setVisible(false);
        gameOverPane.setManaged(false);
        running = false;
        paused = false;
        canCashOut = false;
        awaitingFirstMove = false;
        betPlaced = false;
        score = 0;
        accumulator = 0;
        queuedDirection = null;
        gameStartNanos = 0;
        scoreLabel.setText("Score: 0");
        timeLabel.setText("Time: 0s");
        if (wagerField != null) {
            wagerField.setDisable(false);
        }
        if (obstaclesCheckBox != null) {
            obstaclesCheckBox.setDisable(false);
        }
        setStatus("Enter a wager then click Place Bet.");
        updateActionButton();
        refreshHeader();
        render(canvas.getGraphicsContext2D());
    }

    private double parseWager() {
        if (wagerField == null) {
            return -1;
        }
        try {
            return Double.parseDouble(wagerField.getText().trim().replace(',', '.'));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private void handleActionButton() {
        if (running) {
            if (awaitingFirstMove) {
                showHome();
            } else {
                cashOut();
            }
            return;
        }
        armRound();
    }

    private void armRound() {
        if (running || storage == null) {
            return;
        }

        double parsedWager = parseWager();
        if (parsedWager <= 0) {
            setStatus("Wager must be greater than 0.");
            return;
        }

        this.score = 0;
        this.accumulator = 0;
        this.paused = false;
        this.stepSeconds = 1.0 / 8.0;
        this.gameStartNanos = 0;
        this.obstaclesEnabled = obstaclesCheckBox != null && obstaclesCheckBox.isSelected();
        this.obstacles = obstaclesEnabled
                ? generateObstacles()
                : Set.of();

        List<Cell> initial = new ArrayList<>();
        int startX = GRID_WIDTH / 2 - 3;
        int startY = GRID_HEIGHT / 2;
        for (int i = 0; i < 5; i++) {
            initial.add(new Cell(startX - i, startY));
        }
        snake = new SnakeModel(initial, Direction.RIGHT);
        food = spawnFood();
        queuedDirection = Direction.RIGHT;
        running = true;
        canCashOut = false;
        awaitingFirstMove = true;
        betPlaced = false;
        canvas.requestFocus();
        setStatus("Press a direction key to start. Bet locks on first move. Press C to cash out.");
        updateActionButton();
        refreshHeader();
    }

    private void tryStartOnFirstMove() {
        if (!running || !awaitingFirstMove || storage == null) {
            return;
        }
        double parsedWager = parseWager();
        double balance = storage.getBalance();
        if (parsedWager <= 0) {
            setStatus("Wager must be greater than 0.");
            return;
        }
        if (parsedWager > balance) {
            setStatus("Insufficient funds for this wager.");
            return;
        }

        wager = parsedWager;
        storage.setBalance(balance - wager);
        if (onBalanceChanged != null) {
            onBalanceChanged.run();
        }

        awaitingFirstMove = false;
        betPlaced = true;
        canCashOut = true;
        gameStartNanos = System.nanoTime();

        if (wagerField != null) {
            wagerField.setDisable(true);
        }
        if (obstaclesCheckBox != null) {
            obstaclesCheckBox.setDisable(true);
        }

        setStatus("Good luck! Press C to cash out.");
        updateActionButton();
        refreshHeader();
    }

    private void update(double delta) {
        if (!running) {
            return;
        }

        if (paused) {
            updateTimeLabel();
            return;
        }

        if (awaitingFirstMove) {
            updateTimeLabel();
            return;
        }

        accumulator += delta;
        int maxSteps = 8;
        while (accumulator >= stepSeconds && maxSteps-- > 0) {
            accumulator -= stepSeconds;
            step();
            if (!running) {
                break;
            }
        }
        updateTimeLabel();
    }

    private double getMultiplier() {
        double base = score * MULTIPLIER_PER_FOOD;
        return obstaclesEnabled ? (base * OBSTACLE_MULTIPLIER_BOOST) : base;
    }

    private void cashOut() {
        if (!running || !canCashOut || storage == null) {
            return;
        }
        double multiplier = getMultiplier();
        double payout = wager * multiplier;
        storage.setBalance(storage.getBalance() + payout);
        if (onBalanceChanged != null) {
            onBalanceChanged.run();
        }
        endRound("Cash-out completed: +" + String.format("$%.2f", payout), payout, true);
    }

    private void step() {
        if (queuedDirection != null) {
            snake.setPendingDirection(queuedDirection);
            queuedDirection = null;
        }

        SnakeModel.StepResult result = snake.step();
        if (result instanceof SnakeModel.StepResult.SelfCollision) {
            endRound("Self-collision! Game over, wager lost.", 0, false);
            return;
        }

        Cell head = snake.head();
        if (head.x() < 0 || head.y() < 0 || head.x() >= GRID_WIDTH || head.y() >= GRID_HEIGHT) {
            endRound("Wall collision! Game over, wager lost.", 0, false);
            return;
        }
        if (obstacles.contains(head)) {
            endRound("Obstacle collision! Game over, wager lost.", 0, false);
            return;
        }

        if (head.equals(food)) {
            score++;
            snake.grow(1);
            food = spawnFood();
            stepSeconds = 1.0 / Math.min(16.0, 8.0 + (score / 4.0));
            scoreLabel.setText("Score: " + score);
            if (multiplierLabel != null) {
                multiplierLabel.setText("Multiplier: x" + String.format("%.2f", getMultiplier()));
            }
        }
    }

    private void endRound(String message, double payout, boolean cashedOut) {
        if (!running || storage == null) {
            return;
        }
        running = false;
        paused = false;
        canCashOut = false;
        awaitingFirstMove = false;
        betPlaced = false;
        if (wagerField != null) {
            wagerField.setDisable(false);
        }
        if (obstaclesCheckBox != null) {
            obstaclesCheckBox.setDisable(false);
        }
        long durationSeconds = Math.max(0, (System.nanoTime() - gameStartNanos) / 1_000_000_000L);
        if (gameStartNanos == 0) {
            durationSeconds = 0;
        }

        updateActionButton();

        storage.addScore(score, getMultiplier(), wager, payout);
        setStatus(message);
        refreshHeader();
        refreshScoreboard();
        showGameOver(score, durationSeconds, payout, cashedOut);
    }

    private void showGameOver(int finalScore, long durationSeconds, double payout, boolean cashedOut) {
        if (gameOverTitleLabel != null) {
            gameOverTitleLabel.setText(cashedOut ? "Cashed Out" : "Game Over");
        }
        finalUserLabel.setText("Player: Profile");
        finalScoreLabel.setText("Score: " + finalScore);
        finalTimeLabel.setText("Time: " + durationSeconds + "s");
        if (finalMultiplierLabel != null) {
            finalMultiplierLabel.setText("Multiplier: x" + String.format("%.2f", getMultiplier()));
        }
        if (finalWagerLabel != null) {
            finalWagerLabel.setText("Wager: " + String.format("$%.2f", wager));
        }
        if (finalPayoutLabel != null) {
            finalPayoutLabel.setText("Payout: " + String.format("$%.2f", payout));
        }
        gameOverPane.setVisible(true);
        gameOverPane.setManaged(true);
    }

    private void updateTimeLabel() {
        long durationSeconds = gameStartNanos == 0
                ? 0
                : Math.max(0, (System.nanoTime() - gameStartNanos) / 1_000_000_000L);
        timeLabel.setText("Time: " + durationSeconds + "s" + (paused ? " (paused)" : ""));
    }

    private void refreshHeader() {
        userLabel.setText("Player: Profile");
        if (multiplierLabel != null) {
            multiplierLabel.setText("Multiplier: x" + String.format("%.2f", getMultiplier()));
        }
        if (storage != null) {
            if (balanceLabel != null) {
                balanceLabel.setText("Balance: $" + String.format("%.2f", storage.getBalance()));
            }
            if (bestScoreLabel != null) {
                bestScoreLabel.setText("Best: " + storage.getBestScore());
            }
        }
    }

    private void refreshScoreboard() {
        if (storage == null) {
            return;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM HH:mm");
        scoreboard.getItems().clear();
        List<ScoreEntry> entries = storage.getTopScores(SCOREBOARD_LIMIT);
        for (int i = 0; i < entries.size(); i++) {
            ScoreEntry entry = entries.get(i);
            String line = String.format(
                    "%d) %d pts | x%.2f | Wager $%.2f | Payout $%.2f | %s",
                    i + 1,
                    entry.score(),
                    entry.multiplier(),
                    entry.wager(),
                    entry.payout(),
                    entry.playedAt().format(formatter)
            );
            scoreboard.getItems().add(line);
        }
    }

    private void setStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message == null ? "" : message);
        }
    }

    private void updateActionButton() {
        if (cashOutButton == null) {
            return;
        }
        if (!running) {
            cashOutButton.setText("Place Bet");
            cashOutButton.setDisable(false);
            return;
        }
        if (awaitingFirstMove) {
            cashOutButton.setText("Cancel");
            cashOutButton.setDisable(false);
            return;
        }
        cashOutButton.setText("Cash Out");
        cashOutButton.setDisable(!canCashOut);
    }

    private Set<Cell> generateObstacles() {
        Set<Cell> result = new HashSet<>();
        int count = 18;
        int attempts = 0;
        while (result.size() < count && attempts++ < 2000) {
            int x = 2 + random.nextInt(GRID_WIDTH - 4);
            int y = 2 + random.nextInt(GRID_HEIGHT - 4);
            Cell cell = new Cell(x, y);
            if ((Math.abs(x - GRID_WIDTH / 2) + Math.abs(y - GRID_HEIGHT / 2)) < 4) {
                continue;
            }
            result.add(cell);
        }
        return result;
    }

    private Cell spawnFood() {
        int attempts = 0;
        while (attempts++ < 5000) {
            int x = random.nextInt(GRID_WIDTH);
            int y = random.nextInt(GRID_HEIGHT);
            Cell cell = new Cell(x, y);
            if (obstacles.contains(cell)) {
                continue;
            }
            if (snake != null && snake.occupies(cell)) {
                continue;
            }
            return cell;
        }
        return new Cell(0, 0);
    }

    private void render(GraphicsContext gc) {
        gc.setFill(Color.web("#0b1220"));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        gc.setFill(Color.web("#111827"));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        gc.setStroke(Color.web("#1f2a44"));
        gc.setLineWidth(2);
        gc.strokeRect(1, 1, canvas.getWidth() - 2, canvas.getHeight() - 2);

        gc.setFill(Color.web("#334155"));
        for (Cell cell : obstacles) {
            drawCell(gc, cell.x(), cell.y(), 0.10);
        }

        if (food != null) {
            gc.setFill(Color.web("#ef4444"));
            drawCell(gc, food.x(), food.y(), 0.20);
        }

        if (snake != null) {
            List<Cell> body = snake.snapshotBody();
            for (int i = body.size() - 1; i >= 0; i--) {
                Cell segment = body.get(i);
                boolean isHead = (i == 0);
                gc.setFill(isHead ? Color.web("#34d399") : Color.web("#22c55e"));
                drawCell(gc, segment.x(), segment.y(), isHead ? 0.18 : 0.12);
            }
        }

        if (!running) {
            gc.setFill(Color.web("#93c5fd"));
            gc.fillText("Place a bet, then use ZQSD / Arrows to move. P to pause. C to cash out.", 12, 24);
        }
    }

    private void drawCell(GraphicsContext gc, int x, int y, double insetRatio) {
        double inset = CELL_SIZE * insetRatio;
        double px = x * CELL_SIZE + inset;
        double py = y * CELL_SIZE + inset;
        double size = CELL_SIZE - inset * 2;
        gc.fillRoundRect(px, py, size, size, 8, 8);
    }
}

