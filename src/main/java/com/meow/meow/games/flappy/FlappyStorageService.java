package com.meow.meow.games.flappy;

import com.meow.meow.core.model.ScoreEntry;
import com.meow.meow.core.storage.GlobalStorageService;

import java.util.List;

public class FlappyStorageService {
    private static final String FLAPPY_GAME_KEY = "flappy";
    private final GlobalStorageService globalStorage;

    public FlappyStorageService() {
        this.globalStorage = GlobalStorageService.getInstance();
    }

    public double getBalance() {
        return globalStorage.getBalance();
    }

    public void setBalance(double balance) {
        globalStorage.setBalance(balance);
    }

    public void addScore(int score, double multiplier, double wager, double payout) {
        globalStorage.addScore(FLAPPY_GAME_KEY, score, multiplier, wager, payout);
    }

    public int getBestScore() {
        return globalStorage.getBestScore(FLAPPY_GAME_KEY);
    }

    public List<ScoreEntry> getTopScores(int limit) {
        return globalStorage.getTopScores(FLAPPY_GAME_KEY, limit);
    }
}
