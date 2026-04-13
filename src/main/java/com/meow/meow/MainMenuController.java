package com.meow.meow;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

public class MainMenuController {
    @FXML
    private Label balanceLabel;

    @FXML
    private ImageView flappyCard;

    @FXML
    private ImageView blackjackCard;

    private final FlappyStorageService storage = new FlappyStorageService();

    @FXML
    private void initialize() {
        refreshBalance();
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

    private void refreshBalance() {
        balanceLabel.setText("Balance: $" + String.format("%.2f", storage.getBalance()));
    }
}
