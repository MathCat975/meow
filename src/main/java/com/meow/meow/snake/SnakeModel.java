package com.meow.meow.snake;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class SnakeModel {
    private final Deque<Cell> body;
    private final Set<Cell> occupied;
    private Direction direction;
    private Direction pendingDirection;
    private int growPending;

    SnakeModel(List<Cell> initialBody, Direction initialDirection) {
        if (initialBody == null || initialBody.isEmpty()) {
            throw new IllegalArgumentException("Snake body cannot be empty.");
        }
        this.body = new ArrayDeque<>(initialBody);
        this.occupied = new HashSet<>(initialBody);
        this.direction = initialDirection;
        this.pendingDirection = initialDirection;
    }

    Direction direction() {
        return direction;
    }

    void setPendingDirection(Direction next) {
        if (next == null) {
            return;
        }
        if (direction.isOpposite(next)) {
            return;
        }
        this.pendingDirection = next;
    }

    Cell head() {
        return body.peekFirst();
    }

    List<Cell> snapshotBody() {
        return new ArrayList<>(body);
    }

    boolean occupies(Cell cell) {
        return occupied.contains(cell);
    }

    int length() {
        return body.size();
    }

    void grow(int amount) {
        growPending += Math.max(0, amount);
    }

    StepResult step() {
        direction = pendingDirection;
        Cell currentHead = body.peekFirst();
        Cell nextHead = new Cell(currentHead.x() + direction.dx(), currentHead.y() + direction.dy());

        boolean willRemoveTail = growPending <= 0;
        Cell tail = body.peekLast();
        boolean hitsBody = occupied.contains(nextHead) && !(willRemoveTail && nextHead.equals(tail));

        if (hitsBody) {
            return StepResult.selfCollision(nextHead);
        }

        body.addFirst(nextHead);
        occupied.add(nextHead);

        boolean grew = false;
        if (growPending > 0) {
            growPending--;
            grew = true;
        } else {
            Cell removed = body.removeLast();
            occupied.remove(removed);
        }

        return StepResult.moved(nextHead, grew);
    }

    sealed interface StepResult permits StepResult.Moved, StepResult.SelfCollision {
        static Moved moved(Cell head, boolean grew) {
            return new Moved(head, grew);
        }

        static SelfCollision selfCollision(Cell head) {
            return new SelfCollision(head);
        }

        record Moved(Cell head, boolean grew) implements StepResult {
        }

        record SelfCollision(Cell head) implements StepResult {
        }
    }
}

