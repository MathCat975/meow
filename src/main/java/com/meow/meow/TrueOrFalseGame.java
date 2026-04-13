import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TrueOrFalseGame extends Application {
    private static final int WINDOW_WIDTH = 920;
    private static final int WINDOW_HEIGHT = 500;
    private static final int TARGET_SCORE = 5;
    private static final Path DATABASE_PATH = Path.of("data", "true_false_questions.csv");

    private final Random random = new Random();
    private final List<Question> questions = new ArrayList<>();

    private Question currentQuestion;
    private Label questionLabel;
    private Label scoreLabel;
    private Label statusLabel;
    private Label progressLabel;
    private TextField questionField;
    private Button trueButton;
    private Button falseButton;
    private Button nextButton;

    private int correctAnswers;
    private int wrongAnswers;
    private int totalAnswered;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        loadQuestions();

        stage.setTitle("True or False");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(8, 20, 10, 20));
        root.setStyle("""
                -fx-background-color: linear-gradient(to bottom right, #0A0E22, #121A36);
                -fx-font-family: "Manrope", "Segoe UI", sans-serif;
                """);

        VBox content = new VBox(12);
        content.setMaxWidth(800);
        content.setFillWidth(true);
        content.setAlignment(Pos.CENTER);

        Button backButton = new Button("<- Back");
        backButton.setStyle(backButtonStyle());
        backButton.setOnAction(event -> stage.close());

        HBox headerRow = new HBox(backButton);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setMaxWidth(Double.MAX_VALUE);

        HBox mainRow = new HBox(12);
        mainRow.setMaxWidth(Double.MAX_VALUE);
        mainRow.setAlignment(Pos.TOP_CENTER);

        VBox leftColumn = new VBox(12);
        VBox rightColumn = new VBox(12);
        leftColumn.setPrefWidth(0);
        rightColumn.setPrefWidth(0);
        leftColumn.setMaxWidth(Double.MAX_VALUE);
        rightColumn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(leftColumn, Priority.ALWAYS);
        HBox.setHgrow(rightColumn, Priority.ALWAYS);

        VBox questionCard = createCardContainer(12);
        questionCard.setAlignment(Pos.CENTER);
        Label questionTitle = sectionTitle("True or False");

        questionLabel = new Label();
        questionLabel.setWrapText(true);
        questionLabel.setMinHeight(90);
        questionLabel.setAlignment(Pos.CENTER);
        questionLabel.setStyle("""
                -fx-background-color: #212845;
                -fx-background-radius: 16;
                -fx-border-color: #414B79;
                -fx-border-radius: 16;
                -fx-text-fill: #F4F2FF;
                -fx-font-size: 16px;
                -fx-font-weight: 700;
                -fx-padding: 14 16 14 16;
                """);

        trueButton = new Button("True");
        trueButton.setStyle(primaryButtonStyle());
        trueButton.setOnAction(event -> submitAnswer(true));

        falseButton = new Button("False");
        falseButton.setStyle(secondaryButtonStyle());
        falseButton.setOnAction(event -> submitAnswer(false));

        nextButton = new Button("Next");
        nextButton.setStyle(ghostButtonStyle());
        nextButton.setOnAction(event -> showRandomQuestion());
        nextButton.setDisable(true);
        nextButton.setOpacity(0.65);

        HBox answerRow = new HBox(8, trueButton, falseButton, nextButton);
        answerRow.setAlignment(Pos.CENTER);

        statusLabel = new Label("Pick True or False to answer the question.");
        statusLabel.setWrapText(true);
        statusLabel.setMinHeight(38);
        statusLabel.setAlignment(Pos.CENTER);
        statusLabel.setStyle(messageStyle("#EEF7FF", "#9D50BB"));

        questionCard.getChildren().addAll(questionTitle, questionLabel, answerRow, statusLabel);

        VBox scoreCard = createCardContainer(12);
        Label scoreTitle = sectionTitle("Progress");
        progressLabel = infoPill();
        scoreLabel = infoPill();
        scoreCard.getChildren().addAll(scoreTitle, progressLabel, scoreLabel);

        VBox addCard = createCardContainer(12);
        addCard.setAlignment(Pos.CENTER);
        Label addTitle = sectionTitle("Add a question");

        questionField = new TextField();
        questionField.setPromptText("Write a statement");
        questionField.setMaxWidth(Double.MAX_VALUE);
        questionField.setStyle(inputStyle());

        Button addTrueButton = new Button("Add as True");
        addTrueButton.setStyle(primaryButtonStyle());
        addTrueButton.setOnAction(event -> addQuestion(true));

        Button addFalseButton = new Button("Add as False");
        addFalseButton.setStyle(secondaryButtonStyle());
        addFalseButton.setOnAction(event -> addQuestion(false));

        HBox addRow = new HBox(8, addTrueButton, addFalseButton);
        addRow.setAlignment(Pos.CENTER);

        addCard.getChildren().addAll(addTitle, questionField, addRow);

        leftColumn.getChildren().add(questionCard);
        rightColumn.getChildren().addAll(scoreCard, addCard);
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

    private void resetGame() {
        correctAnswers = 0;
        wrongAnswers = 0;
        totalAnswered = 0;
        updateScore();
        showRandomQuestion();
    }

    private void showRandomQuestion() {
        if (questions.isEmpty()) {
            currentQuestion = null;
            questionLabel.setText("No questions available.");
            statusLabel.setText("Add a question to start playing.");
            disableAnswerButtons(true);
            return;
        }

        currentQuestion = questions.get(random.nextInt(questions.size()));
        questionLabel.setText(currentQuestion.text());
        statusLabel.setText("Pick True or False to answer the question.");
        statusLabel.setStyle(messageStyle("#EEF7FF", "#9D50BB"));
        questionLabel.setStyle(questionStyle("#414B79"));
        disableAnswerButtons(false);
        nextButton.setDisable(true);
        nextButton.setOpacity(0.65);
        playQuestionAnimation();
    }

    private void submitAnswer(boolean answer) {
        if (currentQuestion == null) {
            return;
        }

        boolean isCorrect = currentQuestion.answer() == answer;
        totalAnswered++;

        if (isCorrect) {
            correctAnswers++;
            statusLabel.setText("Correct answer.");
            statusLabel.setStyle(messageStyle("#E8FFF2", "#59D68C"));
            questionLabel.setStyle(questionStyle("#59D68C"));
        } else {
            wrongAnswers++;
            statusLabel.setText("Wrong answer. The correct answer was " + (currentQuestion.answer() ? "True." : "False."));
            statusLabel.setStyle(messageStyle("#FFE5EC", "#FF8CAB"));
            questionLabel.setStyle(questionStyle("#FF8CAB"));
        }

        if (correctAnswers >= TARGET_SCORE) {
            statusLabel.setText("Target reached. You scored " + correctAnswers + " correct answers.");
            statusLabel.setStyle(messageStyle("#FFF7E1", "#FFD700"));
        }

        updateScore();
        disableAnswerButtons(true);
        nextButton.setDisable(false);
        nextButton.setOpacity(1);
        playAnswerAnimation();
    }

    private void addQuestion(boolean answer) {
        String text = questionField.getText().trim();
        if (text.isEmpty()) {
            statusLabel.setText("Write a question before saving.");
            statusLabel.setStyle(messageStyle("#FFE5EC", "#FF8CAB"));
            return;
        }

        Question question = new Question(text, answer);
        questions.add(question);
        saveQuestions();
        questionField.clear();
        statusLabel.setText("Question saved to the local database.");
        statusLabel.setStyle(messageStyle("#E8FFF2", "#59D68C"));
    }

    private void updateScore() {
        progressLabel.setText("Answered: " + totalAnswered + " | Target: " + TARGET_SCORE);
        scoreLabel.setText("Score: " + correctAnswers + " correct / " + wrongAnswers + " wrong");
    }

    private void disableAnswerButtons(boolean disabled) {
        trueButton.setDisable(disabled);
        falseButton.setDisable(disabled);
        trueButton.setOpacity(disabled ? 0.65 : 1);
        falseButton.setOpacity(disabled ? 0.65 : 1);
    }

    private void loadQuestions() {
        questions.clear();

        if (!Files.exists(DATABASE_PATH)) {
            return;
        }

        try {
            List<String> lines = Files.readAllLines(DATABASE_PATH, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line.isBlank()) {
                    continue;
                }

                int separator = line.lastIndexOf(';');
                if (separator <= 0 || separator >= line.length() - 1) {
                    continue;
                }

                String text = line.substring(0, separator).trim();
                boolean answer = Boolean.parseBoolean(line.substring(separator + 1).trim());
                questions.add(new Question(text, answer));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load the question database.", exception);
        }
    }

    private void saveQuestions() {
        List<String> lines = new ArrayList<>();
        for (Question question : questions) {
            lines.add(question.text() + ";" + question.answer());
        }

        try {
            Files.createDirectories(DATABASE_PATH.getParent());
            Files.write(DATABASE_PATH, lines, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to save the question database.", exception);
        }
    }

    private void playQuestionAnimation() {
        FadeTransition fade = new FadeTransition(Duration.millis(180), questionLabel);
        fade.setFromValue(0.5);
        fade.setToValue(1);

        ScaleTransition scale = new ScaleTransition(Duration.millis(180), questionLabel);
        scale.setFromX(0.98);
        scale.setFromY(0.98);
        scale.setToX(1);
        scale.setToY(1);

        new SequentialTransition(fade, scale).play();
    }

    private void playAnswerAnimation() {
        ScaleTransition pulse = new ScaleTransition(Duration.millis(140), statusLabel);
        pulse.setFromX(1);
        pulse.setFromY(1);
        pulse.setToX(1.03);
        pulse.setToY(1.03);
        pulse.setCycleCount(2);
        pulse.setAutoReverse(true);
        pulse.play();
    }

    private VBox createCardContainer(double spacing) {
        VBox box = new VBox(spacing);
        box.setPadding(new Insets(14));
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
                -fx-font-size: 15px;
                -fx-font-weight: 700;
                """);
        return label;
    }

    private Label infoPill() {
        Label label = new Label();
        label.setMaxWidth(Double.MAX_VALUE);
        label.setPadding(new Insets(8, 12, 8, 12));
        label.setStyle("""
                -fx-background-color: #212845;
                -fx-background-radius: 14;
                -fx-text-fill: #F4F2FF;
                -fx-font-size: 12px;
                """);
        return label;
    }

    private String inputStyle() {
        return """
                -fx-background-color: #212845;
                -fx-background-radius: 14;
                -fx-border-color: #414B79;
                -fx-border-radius: 14;
                -fx-text-fill: #F4F2FF;
                -fx-prompt-text-fill: #8F96BF;
                -fx-font-size: 13px;
                -fx-padding: 9 12 9 12;
                """;
    }

    private String questionStyle(String borderColor) {
        return """
                -fx-background-color: #212845;
                -fx-background-radius: 16;
                -fx-border-color: %s;
                -fx-border-radius: 16;
                -fx-text-fill: #F4F2FF;
                -fx-font-size: 16px;
                -fx-font-weight: 700;
                -fx-padding: 14 16 14 16;
                """.formatted(borderColor);
    }

    private String messageStyle(String textColor, String borderColor) {
        return """
                -fx-background-color: #161C37;
                -fx-background-radius: 16;
                -fx-border-color: %s;
                -fx-border-radius: 16;
                -fx-text-fill: %s;
                -fx-font-size: 12px;
                -fx-padding: 8 12 8 12;
                """.formatted(borderColor, textColor);
    }

    private String primaryButtonStyle() {
        return """
                -fx-background-color: #FFD700;
                -fx-background-radius: 999;
                -fx-text-fill: #121A36;
                -fx-font-size: 12px;
                -fx-font-weight: 700;
                -fx-padding: 8 14 8 14;
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
                -fx-font-size: 12px;
                -fx-font-weight: 600;
                -fx-padding: 8 14 8 14;
                -fx-cursor: hand;
                """;
    }

    private String ghostButtonStyle() {
        return """
                -fx-background-color: #2A3157;
                -fx-background-radius: 999;
                -fx-text-fill: #F4F2FF;
                -fx-font-size: 12px;
                -fx-font-weight: 600;
                -fx-padding: 8 14 8 14;
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

    private record Question(String text, boolean answer) {
    }
}
