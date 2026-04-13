package com.meow.meow;

import java.time.LocalDateTime;

public record ScoreEntry(int score, double multiplier, double wager, double payout, LocalDateTime playedAt) {
}
