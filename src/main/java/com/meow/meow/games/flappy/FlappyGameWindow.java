package com.meow.meow.games.flappy;

import com.meow.meow.MainApplication;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class FlappyGameWindow {
    private final FlappyStorageService storage;
    private final Runnable onBalanceChanged;

    public FlappyGameWindow(FlappyStorageService storage, Runnable onBalanceChanged) {
        this.storage = storage;
        this.onBalanceChanged = onBalanceChanged;
    }

    public void show(Stage owner) {
        try {
            FXMLLoader loader = new FXMLLoader(MainApplication.class.getResource("flappy-game-view.fxml"));
            Scene scene = new Scene(loader.load());

            FlappyGameController controller = loader.getController();
            controller.configure(storage, onBalanceChanged);
            controller.bindInput(scene);

            Stage stage = new Stage();
            stage.initOwner(owner);
            stage.initModality(Modality.NONE);
            stage.setTitle("MeowCasino - Flappy");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.setOnCloseRequest(event -> controller.shutdown());
            stage.show();
            controller.onShown();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load the Flappy FXML interface.", exception);
        }
    }
}
