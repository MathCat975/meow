package com.meow.meow.snake;

public enum Direction {
    UP(0, -1),
    RIGHT(1, 0),
    DOWN(0, 1),
    LEFT(-1, 0);

    private final int dx;
    private final int dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    public int dx() {
        return dx;
    }

    public int dy() {
        return dy;
    }

    public boolean isOpposite(Direction other) {
        return other != null && (dx + other.dx == 0) && (dy + other.dy == 0);
    }
}

