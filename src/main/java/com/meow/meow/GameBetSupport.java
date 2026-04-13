package com.meow.meow;

public final class GameBetSupport {
    private final GlobalStorageService storage = GlobalStorageService.getInstance();
    private final String gameKey;

    private double activeWager;
    private boolean activeRound;

    public GameBetSupport(String gameKey) {
        this.gameKey = gameKey;
    }

    public double getBalance() {
        return storage.getBalance();
    }

    public boolean hasActiveRound() {
        return activeRound;
    }

    public double getActiveWager() {
        return activeWager;
    }

    public String startRound(String rawBet) {
        if (activeRound) {
            return "Finish the current game before starting a new one.";
        }

        double wager;
        try {
            wager = Double.parseDouble(rawBet.trim().replace(',', '.'));
        } catch (RuntimeException exception) {
            return "Enter a valid bet amount.";
        }

        if (wager <= 0) {
            return "Bet must be greater than 0.";
        }

        double balance = storage.getBalance();
        if (wager > balance) {
            return "Insufficient balance for this bet.";
        }

        storage.setBalance(balance - wager);
        activeWager = wager;
        activeRound = true;
        return null;
    }

    public BetResult settleRound(int score, double multiplier) {
        if (!activeRound) {
            return new BetResult(0, 0, 0);
        }

        double wager = activeWager;
        double payout = wager * multiplier;
        if (payout > 0) {
            storage.setBalance(storage.getBalance() + payout);
        }

        storage.addScore(gameKey, score, multiplier, wager, payout);
        activeWager = 0;
        activeRound = false;
        return new BetResult(wager, payout, payout - wager);
    }

    public void forfeitRound(int score) {
        if (activeRound) {
            settleRound(score, 0);
        }
    }

    public record BetResult(double wager, double payout, double netChange) {
    }
}
