package com.meow.meow.core.model;

import java.time.LocalDateTime;

public record ScoreEntry(int score, double multiplier, double wager, double payout, LocalDateTime playedAt) {
}
