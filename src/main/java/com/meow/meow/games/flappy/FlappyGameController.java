package com.meow.meow.games.flappy;

import com.meow.meow.MainApplication;
import com.meow.meow.core.model.ScoreEntry;
import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FlappyGameController {
    private static final double WIDTH = 960;
    private static final double HEIGHT = 560;
    private static final double GROUND_HEIGHT = 70;
    private static final double BIRD_X = 220;
    private static final double BIRD_SIZE = 40;
    private static final double BIRD_COLLISION_RADIUS = 15;
    private static final double PIPE_WIDTH = 90;
    private static final double PIPE_COLLISION_INSET = 6;
    private static final double BASE_GAP_HEIGHT = 155;
    private static final double BASE_PIPE_SPEED = 255;
    private static final double BASE_PIPE_INTERVAL = 1.30;
    private static final double GRAVITY = 1500;
    private static final double JUMP_VELOCITY = -450;
    private static final int SCOREBOARD_LIMIT = 10;

    @FXML
    private Canvas canvas;
    @FXML
    private Label scoreLabel;
    @FXML
    private Label bestScoreLabel;
    @FXML
    private Label multiplierLabel;
    @FXML
    private Label difficultyLabel;
    @FXML
    private Label windLabel;
    @FXML
    private Label balanceLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private TextField wagerField;
    @FXML
    private Button startButton;
    @FXML
    private Button cashOutButton;
    @FXML
    private ListView<String> scoreboard;

    private final List<Pipe> pipes = new ArrayList<>();
    private final Image birdImage = new Image(MainApplication.class.getResourceAsStream("flappyicon.png"));
    private final Image backgroundImage = new Image(MainApplication.class.getResourceAsStream("flappybackground.jpg"));
    private final Image pipeImage = new Image(MainApplication.class.getResourceAsStream("flappypipe.png"));

    private FlappyStorageService storage;
    private Runnable onBalanceChanged;
    private AnimationTimer loop;
    private boolean running;
    private boolean canCashOut;
    private double birdY;
    private double velocityY;
    private double spawnTimer;
    private double distance;
    private int score;
    private double wager;
    private double backgroundOffset;
    private double elapsedTime;
    private double windTimer;
    private double nextWindAt;
    private double windTimeLeft;
    private double windForce;

    @FXML
    private void initialize() {
        canvas.setWidth(WIDTH);
        canvas.setHeight(HEIGHT);
        cashOutButton.setDisable(true);
        startButton.setOnAction(event -> startRound());
        cashOutButton.setOnAction(event -> cashOut());
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

    public void configure(FlappyStorageService storage, Runnable onBalanceChanged) {
        this.storage = storage;
        this.onBalanceChanged = onBalanceChanged;
        resetRoundState();
        refreshHeader();
        refreshScoreboard();
        createAndStartLoop();
    }

    public void bindInput(Scene scene) {
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.SPACE || event.getCode() == KeyCode.UP) {
                flap();
            } else if (event.getCode() == KeyCode.C) {
                cashOut();
            }
        });
        scene.setOnMouseClicked(event -> flap());
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
                update(delta);
                render(gc);
            }
        };
        loop.start();
    }

    private void update(double delta) {
        if (!running) {
            return;
        }

        spawnTimer += delta;
        elapsedTime += delta;
        updateWind(delta);

        double pipeSpeed = getCurrentPipeSpeed();
        distance += pipeSpeed * delta;
        backgroundOffset += pipeSpeed * 0.2 * delta;
        velocityY += GRAVITY * delta;
        velocityY += windForce * delta;
        birdY += velocityY * delta;

        if (spawnTimer >= getCurrentSpawnInterval()) {
            spawnTimer = 0;
            pipes.add(Pipe.random(WIDTH, getCurrentGapHeight()));
        }

        Iterator<Pipe> iterator = pipes.iterator();
        while (iterator.hasNext()) {
            Pipe pipe = iterator.next();
            pipe.x -= pipeSpeed * delta;
            double currentGapY = pipe.getGapY(elapsedTime);
            double gapCenterY = currentGapY + (pipe.gapHeight / 2.0);

            if (!pipe.scored && pipe.x + PIPE_WIDTH < BIRD_X) {
                pipe.scored = true;
                score++;
                if (Math.abs(birdY - gapCenterY) < 18) {
                    score++;
                    statusLabel.setText("Perfect pass: +1 bonus point");
                }
            }
            if (pipe.x + PIPE_WIDTH < -10) {
                iterator.remove();
            }
        }

        scoreLabel.setText("Score: " + score);
        multiplierLabel.setText("Multiplier: x" + String.format("%.2f", getMultiplier()));
        difficultyLabel.setText("Difficulty: " + getDifficultyLevel());
        windLabel.setText(windTimeLeft > 0 ? "Wind: " + (windForce < 0 ? "upward" : "downward") : "Wind: calm");

        if (isCollision()) {
            endRound("Collision! Game over, wager lost.");
        }
    }

    private void render(GraphicsContext gc) {
        gc.setFill(Color.web("#60a5fa"));
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        double imgWidth = backgroundImage.getWidth();
        if (imgWidth > 0) {
            double x = -(backgroundOffset % imgWidth);
            gc.drawImage(backgroundImage, x, 0, imgWidth, HEIGHT - GROUND_HEIGHT);
            gc.drawImage(backgroundImage, x + imgWidth, 0, imgWidth, HEIGHT - GROUND_HEIGHT);
        }

        for (Pipe pipe : pipes) {
            double currentGapY = pipe.getGapY(elapsedTime);
            double lowerPipeHeight = HEIGHT - GROUND_HEIGHT - currentGapY - pipe.gapHeight;
            gc.drawImage(pipeImage, pipe.x, 0, PIPE_WIDTH, currentGapY);
            gc.save();
            gc.translate(pipe.x + PIPE_WIDTH / 2, currentGapY + pipe.gapHeight);
            gc.rotate(180);
            gc.drawImage(pipeImage, -PIPE_WIDTH / 2, -lowerPipeHeight, PIPE_WIDTH, lowerPipeHeight);
            gc.restore();
        }

        double rotation = Math.max(-20, Math.min(35, velocityY / 12));
        gc.save();
        gc.translate(BIRD_X, birdY);
        gc.rotate(rotation);
        gc.drawImage(birdImage, -BIRD_SIZE / 2, -BIRD_SIZE / 2, BIRD_SIZE, BIRD_SIZE);
        gc.restore();
    }

    private void startRound() {
        if (running || storage == null) {
            return;
        }
        double parsedWager = parseWager();
        double balance = storage.getBalance();
        if (parsedWager <= 0) {
            statusLabel.setText("Wager must be greater than 0.");
            return;
        }
        if (parsedWager > balance) {
            statusLabel.setText("Insufficient funds for this wager.");
            return;
        }

        wager = parsedWager;
        storage.setBalance(balance - wager);
        if (onBalanceChanged != null) {
            onBalanceChanged.run();
        }

        resetRoundState();
        spawnTimer = BASE_PIPE_INTERVAL - 0.45;
        running = true;
        canCashOut = true;
        cashOutButton.setDisable(false);
        statusLabel.setText("Round started! click to jump. Press C to cash out.");
        refreshHeader();
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
        endRound("Cash-out completed: +" + String.format("$%.2f", payout), payout);
    }

    private void endRound(String message) {
        endRound(message, 0);
    }

    private void endRound(String message, double payout) {
        if (!running || storage == null) {
            return;
        }
        running = false;
        canCashOut = false;
        cashOutButton.setDisable(true);
        storage.addScore(score, getMultiplier(), wager, payout);
        statusLabel.setText(message);
        refreshHeader();
        refreshScoreboard();
    }

    private void flap() {
        if (!running) {
            return;
        }
        velocityY = JUMP_VELOCITY;
    }

    private boolean isCollision() {
        double birdTop = birdY - BIRD_COLLISION_RADIUS;
        double birdBottom = birdY + BIRD_COLLISION_RADIUS;
        if (birdTop < 0 || birdBottom > HEIGHT - GROUND_HEIGHT) {
            return true;
        }

        for (Pipe pipe : pipes) {
            double currentGapY = pipe.getGapY(elapsedTime);
            double pipeLeft = pipe.x + PIPE_COLLISION_INSET;
            double pipeWidth = PIPE_WIDTH - (2 * PIPE_COLLISION_INSET);
            double topPipeHeight = Math.max(0, currentGapY);
            double bottomPipeY = currentGapY + pipe.gapHeight;
            double bottomPipeHeight = Math.max(0, HEIGHT - GROUND_HEIGHT - bottomPipeY);

            boolean hitsTopPipe = circleIntersectsRect(BIRD_X, birdY, BIRD_COLLISION_RADIUS, pipeLeft, 0, pipeWidth, topPipeHeight);
            boolean hitsBottomPipe = circleIntersectsRect(BIRD_X, birdY, BIRD_COLLISION_RADIUS, pipeLeft, bottomPipeY, pipeWidth, bottomPipeHeight);
            if (hitsTopPipe || hitsBottomPipe) {
                return true;
            }
        }
        return false;
    }

    private boolean circleIntersectsRect(double cx, double cy, double radius, double rx, double ry, double rw, double rh) {
        if (rw <= 0 || rh <= 0) {
            return false;
        }
        double closestX = Math.max(rx, Math.min(cx, rx + rw));
        double closestY = Math.max(ry, Math.min(cy, ry + rh));
        double dx = cx - closestX;
        double dy = cy - closestY;
        return (dx * dx + dy * dy) <= radius * radius;
    }

    private void refreshHeader() {
        if (storage == null) {
            return;
        }
        balanceLabel.setText("Balance: $" + String.format("%.2f", storage.getBalance()));
        bestScoreLabel.setText("Best: " + storage.getBestScore());
        multiplierLabel.setText("Multiplier: x" + String.format("%.2f", getMultiplier()));
        scoreLabel.setText("Score: " + score);
        difficultyLabel.setText("Difficulty: " + getDifficultyLevel());
        windLabel.setText(windTimeLeft > 0 ? "Wind: " + (windForce < 0 ? "upward" : "downward") : "Wind: calm");
    }

    private void refreshScoreboard() {
        if (storage == null) {
            return;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM HH:mm");
        scoreboard.getItems().clear();
        List<ScoreEntry> bestScores = storage.getTopScores(SCOREBOARD_LIMIT);
        for (int i = 0; i < bestScores.size(); i++) {
            ScoreEntry entry = bestScores.get(i);
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

    private void resetRoundState() {
        birdY = HEIGHT / 2.0;
        velocityY = 0;
        spawnTimer = 0;
        distance = 0;
        score = 0;
        elapsedTime = 0;
        windTimer = 0;
        nextWindAt = 2.2 + Math.random() * 1.8;
        windTimeLeft = 0;
        windForce = 0;
        pipes.clear();
        running = false;
        canCashOut = false;
        cashOutButton.setDisable(true);
        render(canvas.getGraphicsContext2D());
    }

    private double getMultiplier() {
        return Math.max(0, distance / 900.0);
    }

    private double getCurrentPipeSpeed() {
        return Math.min(360, BASE_PIPE_SPEED + score * 4.2 + distance / 450.0);
    }

    private double getCurrentGapHeight() {
        return Math.max(105, BASE_GAP_HEIGHT - score * 2.2 - distance / 1100.0);
    }

    private double getCurrentSpawnInterval() {
        return Math.max(0.72, BASE_PIPE_INTERVAL - score * 0.018 - distance / 7600.0);
    }

    private int getDifficultyLevel() {
        return 1 + (score / 6);
    }

    private void updateWind(double delta) {
        if (windTimeLeft > 0) {
            windTimeLeft -= delta;
            if (windTimeLeft <= 0) {
                windTimeLeft = 0;
                windForce = 0;
            }
            return;
        }
        windTimer += delta;
        if (windTimer >= nextWindAt) {
            windTimer = 0;
            nextWindAt = 3 + Math.random() * 3;
            windTimeLeft = 0.6 + Math.random() * 0.9;
            windForce = (Math.random() < 0.5 ? -1 : 1) * (140 + Math.random() * 230);
            statusLabel.setText("Wind gust: adjust your jumps");
        }
    }

    private double parseWager() {
        try {
            return Double.parseDouble(wagerField.getText().trim().replace(',', '.'));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static class Pipe {
        private double x;
        private double gapY;
        private double gapHeight;
        private double driftAmplitude;
        private double driftSpeed;
        private double driftPhase;
        private boolean scored;

        private static Pipe random(double startX, double dynamicGapHeight) {
            Pipe pipe = new Pipe();
            pipe.x = startX + 30;
            double min = 70;
            double max = HEIGHT - GROUND_HEIGHT - dynamicGapHeight - 70;
            pipe.gapY = min + Math.random() * (max - min);
            pipe.gapHeight = dynamicGapHeight;
            pipe.driftAmplitude = Math.random() < 0.55 ? (4 + Math.random() * 16) : 0;
            pipe.driftSpeed = 0.7 + Math.random() * 1.0;
            pipe.driftPhase = Math.random() * Math.PI * 2;
            pipe.scored = false;
            return pipe;
        }

        private double getGapY(double elapsedTime) {
            if (driftAmplitude <= 0) {
                return gapY;
            }
            double min = 60;
            double max = HEIGHT - GROUND_HEIGHT - gapHeight - 60;
            double drifted = gapY + Math.sin(elapsedTime * driftSpeed + driftPhase) * driftAmplitude;
            return Math.max(min, Math.min(max, drifted));
        }
    }
}
