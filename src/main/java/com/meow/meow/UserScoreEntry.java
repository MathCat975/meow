package com.meow.meow;

import java.time.LocalDateTime;

public record UserScoreEntry(String username, int score, long durationSeconds, LocalDateTime playedAt) {
}

