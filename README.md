<div align="center">
  <h1 align="center">MeowCasino</h1>
  <p align="center">
    A JavaFX multi-game application bringing together 7 mini-games in a unified casino experience.
    <br />
    <br />
    <a href="https://github.com/MathCat975/meow/issues">⚠️ Report Bug</a>
    ·
    <a href="https://github.com/MathCat975/meow/issues">💡 Request Feature</a>
  </p>
  <p align="center">
    <img src="https://img.shields.io/badge/language-Java%2021-ED8B00?style=for-the-badge&labelColor=000000" />
    <img src="https://img.shields.io/badge/framework-JavaFX-2196F3?style=for-the-badge&labelColor=000000" />
    <img src="https://img.shields.io/badge/database-SQLite-003B57?style=for-the-badge&labelColor=000000" />
    <img src="https://img.shields.io/badge/status-In%20Progress-F0AD4E?style=for-the-badge&labelColor=000000" />
  </p>
</div>

---

### 🔍 Overview

**MeowCasino** is a single JavaFX application grouping multiple mini-games behind a central main menu. All games share a common bankroll (balance), wager/cashout mechanics, and a global SQLite scoreboard — giving the whole thing a coherent casino feel.

7 games are fully playable. 3 more (Sudoku, 2048, PacMan) are locked and under development.

---

### 🎮 Games

| Game | Points | Status |
|---|---:|---|
| Plus ou Moins | 1 | ✅ Playable |
| True or False | 3 | ✅ Playable |
| Pendu (Hangman) | 4 | ✅ Playable |
| Memory | 4 | ✅ Playable |
| BlackJack | 4 | ✅ Playable |
| Snake | 6 | ✅ Playable |
| Flappy Bird-like | 7 | ✅ Playable |
| Sudoku | 5 | 🔒 Locked |
| 2048 | 6 | 🔒 Locked |
| PacMan | 7 | 🔒 Locked |

---

### ✨ Features

- **Shared bankroll** — a single balance is carried across all games, with wager and cashout support.
- **Global scoreboard** — scores and game history are persisted in a local SQLite database (`~/.meowcasino/meowcasino.db`).
- **Rich JavaFX UI** — Canvas rendering, FXML layouts, animations, keyboard/mouse controls, and real-time game loops via `AnimationTimer`.
- **Content management** — add custom questions (True/False), words (Hangman), or card pairs (Memory) from within the app.
- **Locked game cards** — unimplemented games are shown as locked in the main menu.

---

### 🗂️ Architecture

```
com.meow.meow
├── Launcher.java
├── MainApplication.java          # Entry point + scene management
├── MainMenuController.java       # Main menu (FXML)
├── GlobalStorageService.java     # SQLite: profile, scores, blackjack stats
├── GameBetSupport.java           # Shared wager/cashout logic
├── PlusOuMoinsGame.java
├── TrueOrFalseGame.java
├── HangmanGame.java
├── MemoryGame.java
├── BlackjackGameController.java
├── FlappyGameController.java
└── snake/
    └── SnakeModel.java, SnakeGameWindow.java, ...

resources/
├── hello-view.fxml
├── blackjack-game-view.fxml
├── snake-view.fxml
└── flappy-game-view.fxml

data/
├── true_false_questions.csv
├── hangman_words.txt
└── memory_cards.csv
```

**Stack:** Java 21 · JavaFX (controls, FXML, canvas, media) · SQLite via `org.xerial:sqlite-jdbc` · Maven

---

### 🗃️ Persistence

| Data | Storage |
|---|---|
| Global balance | SQLite — `profile` table |
| Game scores & history | SQLite — `scores` table |
| BlackJack stats | SQLite — `blackjack_stats` table |
| Snake / Flappy best scores | SQLite (dedicated services) |
| True/False questions | `data/true_false_questions.csv` |
| Hangman words | `data/hangman_words.txt` |
| Memory card pairs | `data/memory_cards.csv` |

The database file is created automatically at `~/.meowcasino/meowcasino.db` on first launch.

---

### 🚀 Getting Started

**Requirements:** Java 21+, Maven

```bash
# Clone the repo
git clone https://github.com/MathCat975/meow.git
cd meow

# Run the application
./mvnw javafx:run
```

---

### 🙌 Credits

- [Matthieu Rey](https://github.com/MathCat975)
- [Sylvestre Graziani](https://github.com/Askin242)
- ethanleglise

### 📜 License

This project is licensed under the **MIT License**. See `LICENSE` for details.
