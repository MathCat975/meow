package com.meow.meow;

public record BlackjackStats(int handsPlayed, int wins, int losses, int pushes, double netProfit) {
    public static BlackjackStats empty() {
        return new BlackjackStats(0, 0, 0, 0, 0);
    }
}
