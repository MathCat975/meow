package com.meow.meow.games.hangman;

import com.meow.meow.core.bet.GameBetSupport;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class HangmanGame extends Application {
    private static final int WINDOW_WIDTH = 1040;
    private static final int WINDOW_HEIGHT = 620;
    private static final int MAX_WRONG_GUESSES = 10;
    private static final Path WORDS_PATH = Path.of("data", "hangman_words.txt");
    private static final List<String> DEFAULT_WORDS = List.of(
            "chat", "casino", "java", "pendu", "memoire", "chance", "flappy", "blackjack"
    );

    private final Random random = new Random();
    private final List<String> words = new ArrayList<>();
    private final Set<Character> guessedLetters = new LinkedHashSet<>();
    private final Set<Character> wrongLetters = new LinkedHashSet<>();
    private final GameBetSupport betSupport = new GameBetSupport("hangman");

    private String secretWord = "";
    private boolean gameFinished = true;
    private int score;

    private Label wordLabel;
    private Label statusLabel;
    private Label attemptsLabel;
    private Label scoreLabel;
    private Label balanceLabel;
    private Label activeBetLabel;
    private Label potentialPayoutLabel;
    private Label wrongLettersLabel;
    private TextField letterField;
    private TextField wordField;
    private TextField betField;
    private Canvas hangmanCanvas;
    private FlowPane keyboardPane;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void stop() {
        betSupport.forfeitRound(score);
    }

    @Override
    public void start(Stage stage) {
        loadWords();

        stage.setTitle("Hangman");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(6, 16, 8, 16));
        root.setStyle("""
                -fx-background-color: linear-gradient(to bottom right, #0A0E22, #121A36);
                -fx-font-family: "Manrope", "Segoe UI", sans-serif;
                """);

        VBox content = new VBox(6);
        content.setMaxWidth(Double.MAX_VALUE);
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

        VBox leftColumn = new VBox(6);
        VBox rightColumn = new VBox(6);
        leftColumn.setPrefWidth(0);
        rightColumn.setPrefWidth(0);
        leftColumn.setMaxWidth(Double.MAX_VALUE);
        rightColumn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(leftColumn, Priority.ALWAYS);
        HBox.setHgrow(rightColumn, Priority.ALWAYS);

        VBox gameCard = createCardContainer(8);
        gameCard.setAlignment(Pos.TOP_CENTER);
        Label gameTitle = sectionTitle("Hangman");

        hangmanCanvas = new Canvas(180, 125);

        wordLabel = new Label();
        wordLabel.setWrapText(true);
        wordLabel.setAlignment(Pos.CENTER);
        wordLabel.setStyle("""
                -fx-background-color: #212845;
                -fx-background-radius: 16;
                -fx-border-color: #414B79;
                -fx-border-radius: 16;
                -fx-text-fill: #F4F2FF;
                -fx-font-size: 16px;
                -fx-font-weight: 700;
                -fx-padding: 8 12 8 12;
                """);
        wordLabel.setMaxWidth(Double.MAX_VALUE);

        letterField = new TextField();
        letterField.setPromptText("Type one letter");
        letterField.setMaxWidth(Double.MAX_VALUE);
        letterField.setStyle(inputStyle());
        letterField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue.length() > 1) {
                letterField.setText(newValue.substring(0, 1));
            }
        });

        Button guessButton = new Button("Guess");
        guessButton.setStyle(primaryButtonStyle());
        guessButton.setOnAction(event -> handleTypedGuess());

        Button newGameButton = new Button("New game");
        newGameButton.setStyle(secondaryButtonStyle());
        newGameButton.setOnAction(event -> startNewGame());

        Button cashOutButton = new Button("Cash out");
        cashOutButton.setStyle(secondaryButtonStyle());
        cashOutButton.setOnAction(event -> cashOutRound());

        HBox inputRow = new HBox(6, letterField, guessButton, newGameButton, cashOutButton);
        inputRow.setAlignment(Pos.CENTER);

        statusLabel = new Label();
        statusLabel.setWrapText(true);
        statusLabel.setAlignment(Pos.CENTER);
        statusLabel.setMinHeight(28);

        wrongLettersLabel = new Label("Wrong letters: none");
        wrongLettersLabel.setWrapText(true);
        wrongLettersLabel.setAlignment(Pos.CENTER);
        wrongLettersLabel.setStyle(infoTextStyle());

        gameCard.getChildren().addAll(gameTitle, hangmanCanvas, wordLabel, inputRow, statusLabel, wrongLettersLabel);

        VBox statsCard = createCardContainer(8);
        Label statsTitle = sectionTitle("Progress");
        balanceLabel = infoPill();
        activeBetLabel = infoPill();
        potentialPayoutLabel = infoPill();
        betField = new TextField();
        betField.setPromptText("Bet amount");
        betField.setMaxWidth(Double.MAX_VALUE);
        betField.setStyle(inputStyle());
        attemptsLabel = infoPill();
        scoreLabel = infoPill();
        statsCard.getChildren().addAll(statsTitle, balanceLabel, activeBetLabel, potentialPayoutLabel, betField, attemptsLabel, scoreLabel);

        VBox keyboardCard = createCardContainer(8);
        keyboardCard.setAlignment(Pos.TOP_CENTER);
        Label keyboardTitle = sectionTitle("Keyboard");
        keyboardPane = new FlowPane();
        keyboardPane.setHgap(4);
        keyboardPane.setVgap(4);
        keyboardPane.setAlignment(Pos.CENTER);
        populateKeyboard();
        keyboardCard.getChildren().addAll(keyboardTitle, keyboardPane);

        VBox addWordCard = createCardContainer(8);
        addWordCard.setAlignment(Pos.TOP_CENTER);
        Label addWordTitle = sectionTitle("Add a word");
        wordField = new TextField();
        wordField.setPromptText("Write a new word");
        wordField.setMaxWidth(Double.MAX_VALUE);
        wordField.setStyle(inputStyle());

        Button addWordButton = new Button("Save word");
        addWordButton.setStyle(primaryButtonStyle());
        addWordButton.setOnAction(event -> addWord());

        addWordCard.getChildren().addAll(addWordTitle, wordField, addWordButton);

        leftColumn.getChildren().add(gameCard);
        rightColumn.getChildren().addAll(statsCard, keyboardCard, addWordCard);
        mainRow.getChildren().addAll(leftColumn, rightColumn);

        content.getChildren().addAll(headerRow, mainRow);
        root.setCenter(content);
        BorderPane.setAlignment(content, Pos.CENTER);

        prepareGame();

        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        stage.setResizable(false);
        stage.setScene(scene);
        stage.sizeToScene();
        stage.show();
    }

    private void startNewGame() {
        if (betSupport.hasActiveRound()) {
            betSupport.forfeitRound(score);
        }

        String error = betSupport.startRound(betField.getText());
        if (error != null) {
            setStatus(error, "#FFE5EC", "#FF8CAB");
            refreshBetDisplay();
            return;
        }

        if (words.isEmpty()) {
            setStatus("Add a word to start the game.", "#FFE5EC", "#FF8CAB");
            betSupport.forfeitRound(0);
            refreshBetDisplay();
            return;
        }

        secretWord = words.get(random.nextInt(words.size())).toUpperCase();
        guessedLetters.clear();
        wrongLetters.clear();
        gameFinished = false;
        score = 0;
        letterField.clear();
        setStatus("Pick a letter to guess the hidden word.", "#EEF7FF", "#9D50BB");
        updateWordDisplay();
        updateWrongLetters();
        updateStats();
        drawHangman();
        updateKeyboardState();
        refreshBetDisplay();
    }

    private void prepareGame() {
        guessedLetters.clear();
        wrongLetters.clear();
        secretWord = "";
        score = 0;
        gameFinished = true;
        wordLabel.setText("BET REQUIRED");
        wrongLettersLabel.setText("Wrong letters: none");
        setStatus("Enter a bet, then click New game.", "#EEF7FF", "#9D50BB");
        updateStats();
        drawHangman();
        updateKeyboardState();
        refreshBetDisplay();
    }

    private void cashOutRound() {
        if (!betSupport.hasActiveRound()) {
            setStatus("No active round to cash out.", "#FFF7E1", "#FFD700");
            return;
        }
        gameFinished = true;
        GameBetSupport.BetResult result = betSupport.settleRound(score, currentMultiplier());
        updateKeyboardState();
        setStatus("Cash out successful. Gain: $" + String.format("%.2f", result.netChange()) + ".", "#E8FFF2", "#59D68C");
        refreshBetDisplay();
    }

    private void handleTypedGuess() {
        String value = letterField.getText().trim().toUpperCase();
        if (value.isEmpty()) {
            setStatus("Enter one letter first.", "#FFE5EC", "#FF8CAB");
            return;
        }

        handleGuess(value.charAt(0));
        letterField.clear();
    }

    private void handleGuess(char letter) {
        if (!betSupport.hasActiveRound()) {
            setStatus("Enter a bet and start a round first.", "#FFE5EC", "#FF8CAB");
            return;
        }
        if (gameFinished) {
            return;
        }
        if (letter < 'A' || letter > 'Z') {
            setStatus("Only letters A to Z are allowed.", "#FFE5EC", "#FF8CAB");
            return;
        }
        if (guessedLetters.contains(letter) || wrongLetters.contains(letter)) {
            setStatus("Letter already used.", "#FFF7E1", "#FFD700");
            return;
        }

        if (secretWord.indexOf(letter) >= 0) {
            guessedLetters.add(letter);
            score += 10;
            setStatus("Good guess.", "#E8FFF2", "#59D68C");
            refreshBetDisplay();
        } else {
            wrongLetters.add(letter);
            setStatus("Wrong guess.", "#FFE5EC", "#FF8CAB");
        }

        updateWordDisplay();
        updateWrongLetters();
        updateStats();
        drawHangman();
        updateKeyboardState();

        checkGameState();
    }

    private void checkGameState() {
        if (isWordSolved()) {
            gameFinished = true;
            GameBetSupport.BetResult result = betSupport.settleRound(score, currentMultiplier());
            setStatus("You found the word: " + secretWord + ". Gain: $" + String.format("%.2f", result.netChange()) + ".", "#FFF7E1", "#FFD700");
        } else if (wrongLetters.size() >= MAX_WRONG_GUESSES) {
            gameFinished = true;
            wordLabel.setText(secretWord);
            betSupport.settleRound(score, 0);
            setStatus("Game over. The word was " + secretWord, "#FFE5EC", "#FF8CAB");
        }

        if (gameFinished) {
            updateKeyboardState();
            refreshBetDisplay();
        }
    }

    private boolean isWordSolved() {
        for (char letter : secretWord.toCharArray()) {
            if (!guessedLetters.contains(letter)) {
                return false;
            }
        }
        return true;
    }

    private void updateWordDisplay() {
        if (secretWord.isEmpty()) {
            wordLabel.setText("BET REQUIRED");
            return;
        }

        StringBuilder builder = new StringBuilder();
        for (char letter : secretWord.toCharArray()) {
            if (guessedLetters.contains(letter) || gameFinished && wrongLetters.size() >= MAX_WRONG_GUESSES) {
                builder.append(letter).append(' ');
            } else {
                builder.append("_ ");
            }
        }
        wordLabel.setText(builder.toString().trim());
    }

    private void updateWrongLetters() {
        if (wrongLetters.isEmpty()) {
            wrongLettersLabel.setText("Wrong letters: none");
            return;
        }

        StringBuilder builder = new StringBuilder("Wrong letters: ");
        boolean first = true;
        for (char letter : wrongLetters) {
            if (!first) {
                builder.append(", ");
            }
            builder.append(letter);
            first = false;
        }
        wrongLettersLabel.setText(builder.toString());
    }

    private void updateStats() {
        int attemptsLeft = Math.max(0, MAX_WRONG_GUESSES - wrongLetters.size());
        attemptsLabel.setText("Attempts left: " + attemptsLeft + " / " + MAX_WRONG_GUESSES);
        scoreLabel.setText("Score: " + score);
    }

    private double currentMultiplier() {
        if (!betSupport.hasActiveRound()) {
            return 0;
        }
        return 1.0 + guessedLetters.size() * 0.25;
    }

    private void refreshBetDisplay() {
        balanceLabel.setText("Balance: $" + String.format("%.2f", betSupport.getBalance()));
        activeBetLabel.setText(betSupport.hasActiveRound()
                ? "Active bet: $" + String.format("%.2f", betSupport.getActiveWager())
                : "Active bet: none");
        potentialPayoutLabel.setText(betSupport.hasActiveRound()
                ? "Potential payout: $" + String.format("%.2f", betSupport.getActiveWager() * currentMultiplier())
                : "Potential payout: none");
    }

    private void populateKeyboard() {
        keyboardPane.getChildren().clear();
        for (char letter = 'A'; letter <= 'Z'; letter++) {
            char current = letter;
            Button button = new Button(String.valueOf(letter));
            button.setPrefWidth(32);
            button.setPrefHeight(26);
            button.setStyle(keyButtonStyle());
            button.setOnAction(event -> handleGuess(current));
            keyboardPane.getChildren().add(button);
        }
    }

    private void updateKeyboardState() {
        keyboardPane.getChildren().forEach(node -> {
            if (!(node instanceof Button button)) {
                return;
            }

            char letter = button.getText().charAt(0);
            boolean used = guessedLetters.contains(letter) || wrongLetters.contains(letter) || (!betSupport.hasActiveRound()) || gameFinished;
            button.setDisable(used);
            button.setOpacity(used ? 0.55 : 1);

            if (guessedLetters.contains(letter)) {
                button.setStyle(keyButtonStateStyle("#59D68C", "#121A36"));
            } else if (wrongLetters.contains(letter)) {
                button.setStyle(keyButtonStateStyle("#FF8CAB", "#121A36"));
            } else {
                button.setStyle(keyButtonStyle());
            }
        });
    }

    private void addWord() {
        String value = wordField.getText().trim().toLowerCase();
        if (value.length() < 3 || !value.matches("[a-zA-Z]+")) {
            setStatus("Word must contain only letters and at least 3 characters.", "#FFE5EC", "#FF8CAB");
            return;
        }

        if (words.contains(value)) {
            setStatus("Word already exists in the database.", "#FFF7E1", "#FFD700");
            return;
        }

        words.add(value);
        saveWords();
        wordField.clear();
        setStatus("New word saved.", "#E8FFF2", "#59D68C");
    }

    private void drawHangman() {
        GraphicsContext gc = hangmanCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, hangmanCanvas.getWidth(), hangmanCanvas.getHeight());
        gc.setStroke(Color.web("#F4F2FF"));
        gc.setLineWidth(4);

        int stage = wrongLetters.size();
        if (stage >= 1) {
            gc.strokeLine(20, 112, 110, 112);
        }
        if (stage >= 2) {
            gc.strokeLine(45, 112, 45, 15);
        }
        if (stage >= 3) {
            gc.strokeLine(45, 15, 115, 15);
        }
        if (stage >= 4) {
            gc.strokeLine(115, 15, 115, 28);
        }
        if (stage >= 5) {
            gc.strokeOval(96, 28, 34, 34);
        }
        if (stage >= 6) {
            gc.strokeLine(113, 62, 113, 94);
        }
        if (stage >= 7) {
            gc.strokeLine(113, 70, 92, 83);
        }
        if (stage >= 8) {
            gc.strokeLine(113, 70, 134, 83);
        }
        if (stage >= 9) {
            gc.strokeLine(113, 94, 96, 116);
        }
        if (stage >= 10) {
            gc.strokeLine(113, 94, 130, 116);
        }
    }

    private void loadWords() {
        words.clear();
        if (!Files.exists(WORDS_PATH)) {
            seedDefaultWords();
            return;
        }

        try {
            for (String line : Files.readAllLines(WORDS_PATH, StandardCharsets.UTF_8)) {
                String value = line.trim().toLowerCase();
                if (!value.isEmpty()) {
                    words.add(value);
                }
            }
            ensureDefaultWords();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load hangman words.", exception);
        }
    }

    private void seedDefaultWords() {
        words.addAll(DEFAULT_WORDS);
        saveWords();
    }

    private void ensureDefaultWords() {
        boolean changed = false;
        for (String word : DEFAULT_WORDS) {
            if (!words.contains(word)) {
                words.add(word);
                changed = true;
            }
        }
        if (changed) {
            saveWords();
        }
    }

    private void saveWords() {
        try {
            Files.createDirectories(WORDS_PATH.getParent());
            Files.write(WORDS_PATH, words, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to save hangman words.", exception);
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
                -fx-font-size: 13px;
                -fx-font-weight: 700;
                """);
        return label;
    }

    private Label infoPill() {
        Label label = new Label();
        label.setMaxWidth(Double.MAX_VALUE);
        label.setPadding(new Insets(6, 10, 6, 10));
        label.setStyle("""
                -fx-background-color: #212845;
                -fx-background-radius: 14;
                -fx-text-fill: #F4F2FF;
                -fx-font-size: 10px;
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
                -fx-font-size: 11px;
                -fx-padding: 7 10 7 10;
                """;
    }

    private String messageStyle(String textColor, String borderColor) {
        return """
                -fx-background-color: #161C37;
                -fx-background-radius: 16;
                -fx-border-color: %s;
                -fx-border-radius: 16;
                -fx-text-fill: %s;
                -fx-font-size: 10px;
                -fx-padding: 6 10 6 10;
                """.formatted(borderColor, textColor);
    }

    private String primaryButtonStyle() {
        return """
                -fx-background-color: #FFD700;
                -fx-background-radius: 999;
                -fx-text-fill: #121A36;
                -fx-font-size: 11px;
                -fx-font-weight: 700;
                -fx-padding: 6 12 6 12;
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
                -fx-padding: 6 12 6 12;
                -fx-cursor: hand;
                """;
    }

    private String keyButtonStyle() {
        return """
                -fx-background-color: #2A3157;
                -fx-background-radius: 10;
                -fx-text-fill: #F4F2FF;
                -fx-font-size: 9px;
                -fx-font-weight: 700;
                -fx-padding: 4 0 4 0;
                -fx-cursor: hand;
                """;
    }

    private String keyButtonStateStyle(String backgroundColor, String textColor) {
        return """
                -fx-background-color: %s;
                -fx-background-radius: 10;
                -fx-text-fill: %s;
                -fx-font-size: 9px;
                -fx-font-weight: 700;
                -fx-padding: 4 0 4 0;
                """.formatted(backgroundColor, textColor);
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
}
