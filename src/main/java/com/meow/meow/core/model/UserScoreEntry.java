package com.meow.meow.core.model;

import java.time.LocalDateTime;

public record UserScoreEntry(String username, int score, long durationSeconds, LocalDateTime playedAt) {
}

