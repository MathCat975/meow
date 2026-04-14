package com.meow.meow.games.blackjack;

import com.meow.meow.MainApplication;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class BlackjackGameWindow {
    public void show(Stage owner, Runnable onClosed) {
        try {
            FXMLLoader loader = new FXMLLoader(MainApplication.class.getResource("blackjack-game-view.fxml"));
            Scene scene = new Scene(loader.load());
            BlackjackGameController controller = loader.getController();
            controller.bindInput(scene);

            Stage stage = new Stage();
            stage.initOwner(owner);
            stage.initModality(Modality.NONE);
            stage.setTitle("MeowCasino - BlackJack");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.setOnHidden(event -> {
                if (onClosed != null) {
                    onClosed.run();
                }
            });
            stage.show();
            controller.onShown();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load the BlackJack FXML interface.", exception);
        }
    }
}
