package com.meow.meow.snake;

import com.meow.meow.GlobalStorageService;
import com.meow.meow.ScoreEntry;

import java.util.List;

public class SnakeStorageService {
    private static final String SNAKE_GAME_KEY = "snake";
    private final GlobalStorageService globalStorage;

    public SnakeStorageService() {
        this.globalStorage = GlobalStorageService.getInstance();
    }

    public double getBalance() {
        return globalStorage.getBalance();
    }

    public void setBalance(double balance) {
        globalStorage.setBalance(balance);
    }

    public void addScore(int score, double multiplier, double wager, double payout) {
        globalStorage.addScore(SNAKE_GAME_KEY, score, multiplier, wager, payout);
    }

    public int getBestScore() {
        return globalStorage.getBestScore(SNAKE_GAME_KEY);
    }

    public List<ScoreEntry> getTopScores(int limit) {
        return globalStorage.getTopScores(SNAKE_GAME_KEY, limit);
    }
}

