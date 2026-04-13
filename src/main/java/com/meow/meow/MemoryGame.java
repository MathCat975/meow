import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.application.Application;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MemoryGame extends Application {
    private static final int WINDOW_WIDTH = 920;
    private static final int WINDOW_HEIGHT = 560;
    private static final int GRID_SIZE = 4;
    private static final int UNIQUE_CARD_COUNT = 8;
    private static final Path DATABASE_PATH = Path.of("data", "memory_cards.csv");

    private final Random random = new Random();
    private final List<CardDefinition> databaseCards = new ArrayList<>();
    private final List<MemoryCard> boardCards = new ArrayList<>();

    private GridPane boardGrid;
    private Label statusLabel;
    private Label movesLabel;
    private Label timerLabel;
    private Label pairsLabel;
    private Label scoreLabel;
    private TextField cardField;
    private Button restartButton;

    private MemoryCard firstSelected;
    private MemoryCard secondSelected;
    private boolean boardLocked;
    private int moves;
    private int matchedPairs;
    private int elapsedSeconds;
    private Timeline timer;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        loadCards();

        stage.setTitle("Memory");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(8, 20, 10, 20));
        root.setStyle("""
                -fx-background-color: linear-gradient(to bottom right, #0A0E22, #121A36);
                -fx-font-family: "Manrope", "Segoe UI", sans-serif;
                """);

        VBox content = new VBox(8);
        content.setMaxWidth(820);
        content.setFillWidth(true);
        content.setAlignment(Pos.TOP_CENTER);

        Button backButton = new Button("<- Back");
        backButton.setStyle(backButtonStyle());
        backButton.setOnAction(event -> stage.close());

        HBox headerRow = new HBox(backButton);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setMaxWidth(Double.MAX_VALUE);

        HBox mainRow = new HBox(10);
        mainRow.setMaxWidth(Double.MAX_VALUE);
        mainRow.setAlignment(Pos.TOP_CENTER);

        VBox leftColumn = new VBox(8);
        VBox rightColumn = new VBox(8);
        leftColumn.setPrefWidth(0);
        rightColumn.setPrefWidth(0);
        leftColumn.setMaxWidth(Double.MAX_VALUE);
        rightColumn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(leftColumn, Priority.ALWAYS);
        HBox.setHgrow(rightColumn, Priority.ALWAYS);

        VBox boardCard = createCardContainer(8);
        boardCard.setAlignment(Pos.CENTER);
        Label boardTitle = sectionTitle("Memory");

        boardGrid = new GridPane();
        boardGrid.setAlignment(Pos.CENTER);
        boardGrid.setHgap(6);
        boardGrid.setVgap(6);
        for (int index = 0; index < GRID_SIZE; index++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(25);
            column.setHalignment(HPos.CENTER);
            column.setHgrow(Priority.ALWAYS);
            boardGrid.getColumnConstraints().add(column);

            RowConstraints row = new RowConstraints();
            row.setVgrow(Priority.ALWAYS);
            boardGrid.getRowConstraints().add(row);
        }

        statusLabel = new Label("Find every pair in as few moves as possible.");
        statusLabel.setWrapText(true);
        statusLabel.setAlignment(Pos.CENTER);
        statusLabel.setMinHeight(28);
        statusLabel.setStyle(messageStyle("#EEF7FF", "#9D50BB"));

        restartButton = new Button("New game");
        restartButton.setStyle(primaryButtonStyle());
        restartButton.setOnAction(event -> resetGame());

        boardCard.getChildren().addAll(boardTitle, boardGrid, restartButton, statusLabel);

        VBox progressCard = createCardContainer(8);
        Label progressTitle = sectionTitle("Progress");
        movesLabel = infoPill();
        timerLabel = infoPill();
        pairsLabel = infoPill();
        scoreLabel = infoPill();
        progressCard.getChildren().addAll(progressTitle, movesLabel, timerLabel, pairsLabel, scoreLabel);

        VBox databaseCard = createCardContainer(8);
        databaseCard.setAlignment(Pos.CENTER);
        Label databaseTitle = sectionTitle("Card database");

        Label databaseText = new Label("Symbols are loaded from the local database. Add a short label to enrich the deck.");
        databaseText.setWrapText(true);
        databaseText.setStyle(infoTextStyle());
        databaseText.setMaxWidth(Double.MAX_VALUE);

        cardField = new TextField();
        cardField.setPromptText("Example: comet");
        cardField.setMaxWidth(Double.MAX_VALUE);
        cardField.setStyle(inputStyle());

        Button addCardButton = new Button("Save card");
        addCardButton.setStyle(secondaryButtonStyle());
        addCardButton.setOnAction(event -> addCardToDatabase());

        databaseCard.getChildren().addAll(databaseTitle, databaseText, cardField, addCardButton);

        leftColumn.getChildren().add(boardCard);
        rightColumn.getChildren().addAll(progressCard, databaseCard);
        mainRow.getChildren().addAll(leftColumn, rightColumn);

        content.getChildren().addAll(headerRow, mainRow);
        root.setCenter(content);
        BorderPane.setAlignment(content, Pos.CENTER);

        resetGame();

        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        stage.setResizable(false);
        stage.setWidth(WINDOW_WIDTH);
        stage.setHeight(WINDOW_HEIGHT);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        stopTimer();
    }

    private void resetGame() {
        if (databaseCards.size() < UNIQUE_CARD_COUNT) {
            stopTimer();
            boardGrid.getChildren().clear();
            boardCards.clear();
            firstSelected = null;
            secondSelected = null;
            boardLocked = true;
            moves = 0;
            matchedPairs = 0;
            elapsedSeconds = 0;
            setStatus("Add at least " + UNIQUE_CARD_COUNT + " cards to start a game.", "#FFE5EC", "#FF8CAB");
            updateStats();
            return;
        }

        stopTimer();
        elapsedSeconds = 0;
        moves = 0;
        matchedPairs = 0;
        firstSelected = null;
        secondSelected = null;
        boardLocked = false;
        boardCards.clear();
        boardGrid.getChildren().clear();

        List<CardDefinition> selection = new ArrayList<>(databaseCards);
        Collections.shuffle(selection, random);
        selection = new ArrayList<>(selection.subList(0, UNIQUE_CARD_COUNT));

        List<CardDefinition> deck = new ArrayList<>(selection);
        deck.addAll(selection);
        Collections.shuffle(deck, random);

        for (int index = 0; index < deck.size(); index++) {
            MemoryCard card = new MemoryCard(deck.get(index));
            boardCards.add(card);
            boardGrid.add(card.button(), index % GRID_SIZE, index / GRID_SIZE);
        }

        setStatus("Find every pair in as few moves as possible.", "#EEF7FF", "#9D50BB");
        updateStats();
        startTimer();
    }

    private void handleCardSelection(MemoryCard card) {
        if (boardLocked || card.matched() || card.faceUp()) {
            return;
        }

        card.setFaceUp(true);
        playFlip(card);

        if (firstSelected == null) {
            firstSelected = card;
            setStatus("Pick a second card.", "#EEF7FF", "#9D50BB");
            return;
        }

        if (secondSelected == null) {
            secondSelected = card;
            moves++;
            boardLocked = true;
            updateStats();
            evaluateTurn();
        }
    }

    private void evaluateTurn() {
        if (firstSelected == null || secondSelected == null) {
            boardLocked = false;
            return;
        }

        if (firstSelected.label().equals(secondSelected.label())) {
            firstSelected.setMatched(true);
            secondSelected.setMatched(true);
            matchedPairs++;
            setStatus("Pair found.", "#E8FFF2", "#59D68C");
            clearSelection();
            boardLocked = false;
            updateStats();
            checkVictory();
            return;
        }

        setStatus("No match. Cards will flip back.", "#FFF7E1", "#FFD700");
        PauseTransition pause = new PauseTransition(Duration.millis(700));
        pause.setOnFinished(event -> {
            firstSelected.setFaceUp(false);
            secondSelected.setFaceUp(false);
            playFlip(firstSelected);
            playFlip(secondSelected);
            clearSelection();
            boardLocked = false;
        });
        pause.play();
    }

    private void checkVictory() {
        if (matchedPairs < UNIQUE_CARD_COUNT) {
            return;
        }

        stopTimer();
        boardLocked = true;
        int score = computeScore();
        setStatus("All pairs found in " + moves + " moves and " + formatTime(elapsedSeconds) + ".", "#FFF7E1", "#FFD700");
        scoreLabel.setText("Score: " + score);
    }

    private void clearSelection() {
        firstSelected = null;
        secondSelected = null;
    }

    private void playFlip(MemoryCard card) {
        ScaleTransition shrink = new ScaleTransition(Duration.millis(110), card.button());
        shrink.setFromX(1);
        shrink.setToX(0.08);

        ScaleTransition expand = new ScaleTransition(Duration.millis(110), card.button());
        expand.setFromX(0.08);
        expand.setToX(1);

        shrink.setOnFinished(event -> updateCardVisual(card));
        new SequentialTransition(shrink, expand).play();
    }

    private void updateCardVisual(MemoryCard card) {
        Button button = card.button();
        if (card.faceUp()) {
            button.setText(card.label());
            button.setStyle(cardFaceStyle(card.matched()));
        } else {
            button.setText("?");
            button.setStyle(cardBackStyle());
        }
        button.setDisable(card.matched() || boardLocked && !card.faceUp());
        button.setOpacity(card.matched() ? 0.9 : 1);
    }

    private void updateStats() {
        movesLabel.setText("Moves: " + moves);
        timerLabel.setText("Time: " + formatTime(elapsedSeconds));
        pairsLabel.setText("Pairs: " + matchedPairs + " / " + UNIQUE_CARD_COUNT);
        scoreLabel.setText("Score: " + computeScore());
    }

    private int computeScore() {
        int rawScore = 1000 - moves * 35 - elapsedSeconds * 3;
        return Math.max(rawScore, 0);
    }

    private void startTimer() {
        timer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            elapsedSeconds++;
            updateStats();
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
    }

    private void stopTimer() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
    }

    private String formatTime(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return "%02d:%02d".formatted(minutes, seconds);
    }

    private void addCardToDatabase() {
        String value = cardField.getText().trim().toUpperCase();
        if (value.length() < 2 || value.length() > 10 || !value.matches("[A-Z0-9]+")) {
            setStatus("Use 2 to 10 letters or numbers for a card label.", "#FFE5EC", "#FF8CAB");
            return;
        }

        boolean alreadyExists = databaseCards.stream().anyMatch(card -> card.label().equals(value));
        if (alreadyExists) {
            setStatus("This card already exists in the database.", "#FFF7E1", "#FFD700");
            return;
        }

        databaseCards.add(new CardDefinition(value));
        saveCards();
        cardField.clear();
        setStatus("Card saved to the local database.", "#E8FFF2", "#59D68C");
    }

    private void loadCards() {
        databaseCards.clear();

        if (!Files.exists(DATABASE_PATH)) {
            return;
        }

        try {
            for (String line : Files.readAllLines(DATABASE_PATH, StandardCharsets.UTF_8)) {
                String value = line.trim().toUpperCase();
                if (!value.isEmpty()) {
                    databaseCards.add(new CardDefinition(value));
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load the memory database.", exception);
        }
    }

    private void saveCards() {
        List<String> lines = new ArrayList<>();
        for (CardDefinition card : databaseCards) {
            lines.add(card.label());
        }

        try {
            Files.createDirectories(DATABASE_PATH.getParent());
            Files.write(DATABASE_PATH, lines, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to save the memory database.", exception);
        }
    }

    private void setStatus(String text, String textColor, String borderColor) {
        statusLabel.setText(text);
        statusLabel.setStyle(messageStyle(textColor, borderColor));
    }

    private VBox createCardContainer(double spacing) {
        VBox box = new VBox(spacing);
        box.setPadding(new Insets(10));
        box.setMaxWidth(Double.MAX_VALUE);
        box.setStyle("""
                -fx-background-color: rgba(28, 34, 64, 0.96);
                -fx-background-radius: 22;
                -fx-border-color: #39416B;
                -fx-border-radius: 22;
                """);
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.setStyle("""
                -fx-text-fill: #F4F2FF;
                -fx-font-size: 14px;
                -fx-font-weight: 700;
                """);
        return label;
    }

    private Label infoPill() {
        Label label = new Label();
        label.setMaxWidth(Double.MAX_VALUE);
        label.setPadding(new Insets(7, 10, 7, 10));
        label.setStyle("""
                -fx-background-color: #212845;
                -fx-background-radius: 14;
                -fx-text-fill: #F4F2FF;
                -fx-font-size: 11px;
                """);
        return label;
    }

    private String infoTextStyle() {
        return """
                -fx-text-fill: #C8CCE8;
                -fx-font-size: 11px;
                """;
    }

    private String inputStyle() {
        return """
                -fx-background-color: #212845;
                -fx-background-radius: 14;
                -fx-border-color: #414B79;
                -fx-border-radius: 14;
                -fx-text-fill: #F4F2FF;
                -fx-prompt-text-fill: #8F96BF;
                -fx-font-size: 12px;
                -fx-padding: 8 10 8 10;
                """;
    }

    private String messageStyle(String textColor, String borderColor) {
        return """
                -fx-background-color: #161C37;
                -fx-background-radius: 16;
                -fx-border-color: %s;
                -fx-border-radius: 16;
                -fx-text-fill: %s;
                -fx-font-size: 11px;
                -fx-padding: 7 10 7 10;
                """.formatted(borderColor, textColor);
    }

    private String primaryButtonStyle() {
        return """
                -fx-background-color: #FFD700;
                -fx-background-radius: 999;
                -fx-text-fill: #121A36;
                -fx-font-size: 11px;
                -fx-font-weight: 700;
                -fx-padding: 7 12 7 12;
                -fx-cursor: hand;
                """;
    }

    private String secondaryButtonStyle() {
        return """
                -fx-background-color: transparent;
                -fx-background-radius: 999;
                -fx-border-color: #C8CCE8;
                -fx-border-radius: 999;
                -fx-text-fill: #F4F2FF;
                -fx-font-size: 11px;
                -fx-font-weight: 600;
                -fx-padding: 7 12 7 12;
                -fx-cursor: hand;
                """;
    }

    private String backButtonStyle() {
        return """
                -fx-background-color: transparent;
                -fx-border-color: #39416B;
                -fx-border-radius: 999;
                -fx-background-radius: 999;
                -fx-text-fill: #F4F2FF;
                -fx-font-size: 13px;
                -fx-font-weight: 600;
                -fx-padding: 6 12 6 12;
                -fx-cursor: hand;
                """;
    }

    private String cardBackStyle() {
        return """
                -fx-background-color: linear-gradient(to bottom right, #2A3157, #1E2545);
                -fx-background-radius: 18;
                -fx-border-color: #4A5A94;
                -fx-border-radius: 18;
                -fx-text-fill: #FFD700;
                -fx-font-size: 20px;
                -fx-font-weight: 700;
                -fx-min-width: 80px;
                -fx-min-height: 58px;
                -fx-pref-width: 80px;
                -fx-pref-height: 58px;
                -fx-cursor: hand;
                """;
    }

    private String cardFaceStyle(boolean matched) {
        String border = matched ? "#59D68C" : "#FFD700";
        String background = matched ? "#203A35" : "#212845";
        return """
                -fx-background-color: %s;
                -fx-background-radius: 18;
                -fx-border-color: %s;
                -fx-border-radius: 18;
                -fx-text-fill: #F4F2FF;
                -fx-font-size: 15px;
                -fx-font-weight: 700;
                -fx-min-width: 80px;
                -fx-min-height: 58px;
                -fx-pref-width: 80px;
                -fx-pref-height: 58px;
                """.formatted(background, border);
    }

    private final class MemoryCard {
        private final CardDefinition definition;
        private final Button button;
        private boolean matched;
        private boolean faceUp;

        private MemoryCard(CardDefinition definition) {
            this.definition = definition;
            this.button = new Button("?");
            this.button.setStyle(cardBackStyle());
            this.button.setOnAction(event -> handleCardSelection(this));
        }

        private Button button() {
            return button;
        }

        private String label() {
            return definition.label();
        }

        private boolean matched() {
            return matched;
        }

        private void setMatched(boolean matched) {
            this.matched = matched;
            updateCardVisual(this);
        }

        private boolean faceUp() {
            return faceUp;
        }

        private void setFaceUp(boolean faceUp) {
            this.faceUp = faceUp;
        }
    }

    private record CardDefinition(String label) {
    }
}
