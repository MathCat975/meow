package com.meow.meow;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import com.meow.meow.snake.SnakeGameWindow;
import com.meow.meow.snake.SnakeStorageService;

public class MainMenuController {
    @FXML
    private Label balanceLabel;

    @FXML
    private ImageView snakeCard;

    @FXML
    private ImageView flappyCard;

    @FXML
    private ImageView blackjackCard;

    private final FlappyStorageService storage = new FlappyStorageService();
    private final SnakeStorageService snakeStorage = new SnakeStorageService();

    @FXML
    private void initialize() {
        refreshBalance();
    }

    @FXML
    private void handleSnakeClick() {
        Stage owner = (Stage) snakeCard.getScene().getWindow();
        SnakeGameWindow gameWindow = new SnakeGameWindow(snakeStorage, this::refreshBalance);
        gameWindow.show(owner);
    }

    @FXML
    private void handleFlappyClick() {
        Stage owner = (Stage) flappyCard.getScene().getWindow();
        FlappyGameWindow gameWindow = new FlappyGameWindow(storage, this::refreshBalance);
        gameWindow.show(owner);
    }

    @FXML
    private void handleBlackjackClick() {
        Stage owner = (Stage) blackjackCard.getScene().getWindow();
        BlackjackGameWindow gameWindow = new BlackjackGameWindow();
        gameWindow.show(owner, this::refreshBalance);
    }

    @FXML
    private void handleComingSoonClick() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Comming Soon");
        alert.setHeaderText(null);
        alert.setContentText("Comming Soon");
        alert.showAndWait();
    }

    private void refreshBalance() {
        balanceLabel.setText("Balance: $" + String.format("%.2f", storage.getBalance()));
    }
}
