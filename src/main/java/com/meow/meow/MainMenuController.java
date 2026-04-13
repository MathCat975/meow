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

    private void refreshBalance() {
        balanceLabel.setText("Balance: $" + String.format("%.2f", storage.getBalance()));
    }
}
