import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Random;

public class PlusOuMoinsGame extends Application {
    private static final int MIN_NUMBER = 1;
    private static final int MAX_NUMBER = 1000;
    private static final int MAX_ATTEMPTS = 10;
    private static final int WINDOW_WIDTH = 920;
    private static final int WINDOW_HEIGHT = 500;

    private final Random random = new Random();

    private int secretNumber;
    private int attemptsLeft;
    private int attemptsUsed;
    private boolean gameFinished;

    private Label hintLabel;
    private Label scoreLabel;
    private Label attemptsLabel;
    private Label historyLabel;
    private ProgressBar attemptsBar;
    private TextField guessField;
    private Button validateButton;
    private Button restartButton;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("Higher or Lower");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(8, 20, 10, 20));
        root.setStyle("""
                -fx-background-color: linear-gradient(to bottom right, #0A0E22, #121A36);
                -fx-font-family: "Manrope", "Segoe UI", sans-serif;
                """);

        VBox content = new VBox(12);
        content.setMaxWidth(800);
        content.setAlignment(Pos.CENTER);
        content.setFillWidth(true);

        Button backButton = new Button("<- Back");
        backButton.setStyle(backButtonStyle());
        backButton.setOnAction(event -> stage.close());

        HBox headerRow = new HBox(backButton);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setMaxWidth(Double.MAX_VALUE);

        HBox mainRow = new HBox(16);
        mainRow.setAlignment(Pos.TOP_CENTER);
        mainRow.setMaxWidth(Double.MAX_VALUE);

        VBox leftColumn = new VBox(16);
        VBox rightColumn = new VBox(16);
        leftColumn.setPrefWidth(0);
        rightColumn.setPrefWidth(0);
        leftColumn.setMaxWidth(Double.MAX_VALUE);
        rightColumn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(leftColumn, Priority.ALWAYS);
        HBox.setHgrow(rightColumn, Priority.ALWAYS);

        VBox controlCard = createCardContainer(16);
        Label instructionLabel = sectionTitle("Enter your guess");

        guessField = new TextField();
        guessField.setPromptText("Number between 1 and 1000");
        guessField.setStyle("""
                -fx-background-color: #212845;
                -fx-background-radius: 14;
                -fx-border-color: #414B79;
                -fx-border-radius: 14;
                -fx-text-fill: #F4F2FF;
                -fx-prompt-text-fill: #8F96BF;
                -fx-font-size: 14px;
                -fx-padding: 11 14 11 14;
                """);
        guessField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleGuess();
            }
        });

        validateButton = new Button("Submit");
        validateButton.setStyle(primaryButtonStyle());
        validateButton.setOnAction(event -> handleGuess());

        restartButton = new Button("New game");
        restartButton.setStyle(secondaryButtonStyle());
        restartButton.setOnAction(event -> resetGame());

        HBox buttonRow = new HBox(10, validateButton, restartButton);
        buttonRow.setAlignment(Pos.CENTER_LEFT);

        hintLabel = new Label();
        hintLabel.setWrapText(true);
        hintLabel.setMinHeight(44);
        hintLabel.setStyle(messageStyle("#FFF7E1", "#FFD700"));

        historyLabel = new Label("History: no attempts yet.");
        historyLabel.setWrapText(true);
        historyLabel.setStyle("""
                -fx-text-fill: #C8CCE8;
                -fx-font-size: 13px;
                """);

        controlCard.getChildren().addAll(instructionLabel, guessField, buttonRow, hintLabel, historyLabel);

        VBox scoreCard = createCardContainer(16);
        Label scoreTitle = sectionTitle("Progress");

        attemptsLabel = infoPill();
        scoreLabel = infoPill();
        attemptsBar = new ProgressBar(1);
        attemptsBar.setPrefWidth(Double.MAX_VALUE);
        attemptsBar.setStyle("""
                -fx-accent: #9D50BB;
                """);

        VBox progressGroup = new VBox(10, attemptsLabel, scoreLabel, attemptsBar);
        scoreCard.getChildren().addAll(scoreTitle, progressGroup);

        VBox goalCard = createCardContainer(16);
        Label goalTitle = sectionTitle("Goal");
        Label goalText = new Label("Find the secret number before you run out of attempts.");
        goalText.setWrapText(true);
        goalText.setStyle("""
                -fx-text-fill: #C8CCE8;
                -fx-font-size: 13px;
                """);
        goalCard.getChildren().addAll(goalTitle, goalText);

        leftColumn.getChildren().add(controlCard);
        rightColumn.getChildren().addAll(scoreCard, goalCard);
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

    private void handleGuess() {
        if (gameFinished) {
            setHint("The game is over. Start a new game.", "#FFF7E1", "#FFD700");
            return;
        }

        String rawGuess = guessField.getText().trim();
        if (rawGuess.isEmpty()) {
            setHint("Enter a number before submitting.", "#FFE5EC", "#FF8CAB");
            return;
        }

        int guessedNumber;
        try {
            guessedNumber = Integer.parseInt(rawGuess);
        } catch (NumberFormatException exception) {
            setHint("Input must be a valid integer.", "#FFE5EC", "#FF8CAB");
            return;
        }

        if (guessedNumber < MIN_NUMBER || guessedNumber > MAX_NUMBER) {
            setHint("The number must stay between 1 and 1000.", "#FFE5EC", "#FF8CAB");
            return;
        }

        attemptsUsed++;
        attemptsLeft--;

        if (guessedNumber == secretNumber) {
            gameFinished = true;
            setHint("You found " + secretNumber + " in " + attemptsUsed + " attempt(s).", "#EEF7FF", "#9D50BB");
            historyLabel.setText("History: " + guessedNumber + " was the correct answer.");
            updateGameStatus();
            setControlsDisabled(true);
            return;
        }

        if (guessedNumber < secretNumber) {
            setHint("Higher. Try a bigger number.", "#FFF7E1", "#FFD700");
        } else {
            setHint("Lower. Try a smaller number.", "#FFF7E1", "#FFD700");
        }

        historyLabel.setText("History: attempt " + attemptsUsed + " -> " + guessedNumber);

        if (attemptsLeft == 0) {
            gameFinished = true;
            setHint("You lost. The secret number was " + secretNumber + ".", "#FFE5EC", "#FF8CAB");
            setControlsDisabled(true);
        }

        updateGameStatus();
        guessField.clear();
    }

    private void resetGame() {
        secretNumber = random.nextInt(MAX_NUMBER - MIN_NUMBER + 1) + MIN_NUMBER;
        attemptsLeft = MAX_ATTEMPTS;
        attemptsUsed = 0;
        gameFinished = false;

        setControlsDisabled(false);
        guessField.clear();
        historyLabel.setText("History: no attempts yet.");
        setHint("The game has started. Make your first guess.", "#EEF7FF", "#9D50BB");
        updateGameStatus();
    }

    private void updateGameStatus() {
        attemptsLabel.setText("Attempts left: " + attemptsLeft + " / " + MAX_ATTEMPTS);
        scoreLabel.setText("Score: " + (attemptsUsed * 10));
        attemptsBar.setProgress((double) attemptsLeft / MAX_ATTEMPTS);
    }

    private void setControlsDisabled(boolean disabled) {
        guessField.setDisable(disabled);
        validateButton.setDisable(disabled);
        validateButton.setOpacity(disabled ? 0.65 : 1);
        guessField.setOpacity(disabled ? 0.75 : 1);
    }

    private void setHint(String text, String textColor, String borderColor) {
        hintLabel.setText(text);
        hintLabel.setStyle(messageStyle(textColor, borderColor));
    }

    private HBox createCard() {
        HBox box = new HBox();
        box.setMaxWidth(Double.MAX_VALUE);
        box.setStyle("""
                -fx-background-color: rgba(28, 34, 64, 0.96);
                -fx-background-radius: 22;
                -fx-border-color: #39416B;
                -fx-border-radius: 22;
                """);
        return box;
    }

    private VBox createCardContainer(double spacing) {
        VBox box = new VBox(spacing);
        box.setPadding(new Insets(18));
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
                -fx-font-size: 16px;
                -fx-font-weight: 700;
                """);
        return label;
    }

    private Label infoPill() {
        Label label = new Label();
        label.setMaxWidth(Double.MAX_VALUE);
        label.setPadding(new Insets(10, 14, 10, 14));
        label.setStyle("""
                -fx-background-color: #212845;
                -fx-background-radius: 14;
                -fx-text-fill: #F4F2FF;
                -fx-font-size: 13px;
                """);
        return label;
    }

    private String messageStyle(String textColor, String borderColor) {
        return """
                -fx-background-color: #161C37;
                -fx-background-radius: 16;
                -fx-border-color: %s;
                -fx-border-radius: 16;
                -fx-text-fill: %s;
                -fx-font-size: 13px;
                -fx-padding: 10 14 10 14;
                """.formatted(borderColor, textColor);
    }

    private String primaryButtonStyle() {
        return """
                -fx-background-color: #FFD700;
                -fx-background-radius: 999;
                -fx-text-fill: #121A36;
                -fx-font-size: 13px;
                -fx-font-weight: 700;
                -fx-padding: 10 18 10 18;
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
                -fx-font-size: 13px;
                -fx-font-weight: 600;
                -fx-padding: 10 18 10 18;
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
}
