package com.meow.meow.games.snake;

import com.meow.meow.MainApplication;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class SnakeGameWindow {
    private final SnakeStorageService storage;
    private final Runnable onBalanceChanged;

    public SnakeGameWindow(SnakeStorageService storage) {
        this(storage, null);
    }

    public SnakeGameWindow(SnakeStorageService storage, Runnable onBalanceChanged) {
        this.storage = storage;
        this.onBalanceChanged = onBalanceChanged;
    }

    public void show(Stage owner) {
        try {
            FXMLLoader loader = new FXMLLoader(MainApplication.class.getResource("snake-view.fxml"));
            Scene scene = new Scene(loader.load());

            SnakeGameController controller = loader.getController();
            controller.configure(storage, onBalanceChanged);
            controller.bindInput(scene);

            Stage stage = new Stage();
            stage.initOwner(owner);
            stage.initModality(Modality.NONE);
            stage.setTitle("MeowCasino - Snake");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.setOnCloseRequest(event -> controller.shutdown());
            stage.show();
            controller.onShown();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load the Snake FXML interface.", exception);
        }
    }
}

