package com.meow.meow;

import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.geometry.Rectangle2D;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BlackjackGameController {
    private static final double BLACKJACK_PAYOUT = 2.5;
    private static final double STANDARD_WIN_PAYOUT = 2.0;
    private static final double PUSH_PAYOUT = 1.0;
    private static final int DEALER_STAND_THRESHOLD = 17;
    private static final int CARD_WIDTH = 88;
    private static final int CARD_HEIGHT = 128;

    @FXML
    private Label balanceLabel;
    @FXML
    private Label dealerValueLabel;
    @FXML
    private Label playerValueLabel;
    @FXML
    private Label roundLabel;
    @FXML
    private Label winsLabel;
    @FXML
    private Label lossesLabel;
    @FXML
    private Label pushesLabel;
    @FXML
    private Label netLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Label signBadgeLabel;
    @FXML
    private ImageView blackjackSignView;
    @FXML
    private TextField betField;
    @FXML
    private Button dealButton;
    @FXML
    private Button hitButton;
    @FXML
    private Button standButton;
    @FXML
    private HBox dealerCardsBox;
    @FXML
    private HBox playerCardsBox;

    private final GlobalStorageService storage = GlobalStorageService.getInstance();
    private final List<Card> playerHand = new ArrayList<>();
    private final List<Card> dealerHand = new ArrayList<>();
    private final List<ImageView> dealerCardViews = new ArrayList<>();

    private Deck deck;
    private boolean roundActive;
    private boolean dealerHoleHidden;
    private double currentBet;
    private int roundCount;

    @FXML
    private void initialize() {
        hitButton.setDisable(true);
        standButton.setDisable(true);
        dealButton.setOnAction(event -> startRound());
        hitButton.setOnAction(event -> onHit());
        standButton.setOnAction(event -> onStand());
        configureSign();
        refreshBankAndStats();
        refreshRoundLabels();
    }

    public void bindInput(Scene scene) {
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                startRound();
            } else if (event.getCode() == KeyCode.H) {
                onHit();
            } else if (event.getCode() == KeyCode.S) {
                onStand();
            }
        });
    }

    public void onShown() {
        betField.requestFocus();
    }

    private void startRound() {
        if (roundActive) {
            return;
        }

        double bet = parseBet();
        double balance = storage.getBalance();
        if (bet <= 0) {
            statusLabel.setText("Bet must be greater than 0.");
            return;
        }
        if (bet > balance) {
            statusLabel.setText("Insufficient balance for this bet.");
            return;
        }

        storage.setBalance(balance - bet);
        refreshBankAndStats();
        currentBet = bet;
        roundActive = true;
        dealerHoleHidden = true;
        roundCount++;

        playerHand.clear();
        dealerHand.clear();
        dealerCardViews.clear();
        dealerCardsBox.getChildren().clear();
        playerCardsBox.getChildren().clear();

        deck = Deck.shuffled();
        setActionButtons(false);
        statusLabel.setText("Dealing cards...");

        List<Runnable> dealSteps = List.of(
                () -> dealToPlayer(false),
                () -> dealToDealer(true),
                () -> dealToPlayer(false),
                () -> dealToDealer(false)
        );
        runStepsSequentially(dealSteps, 220, this::afterInitialDeal);
    }

    private void afterInitialDeal() {
        refreshRoundLabels();
        int playerValue = handValue(playerHand);
        int dealerValue = handValue(dealerHand);
        boolean playerBlackjack = playerHand.size() == 2 && playerValue == 21;
        boolean dealerBlackjack = dealerHand.size() == 2 && dealerValue == 21;

        if (playerBlackjack || dealerBlackjack) {
            revealDealerHoleCard();
            refreshRoundLabels();
            if (playerBlackjack && dealerBlackjack) {
                settleRound("Both have BlackJack. Push.", BlackjackOutcome.PUSH, PUSH_PAYOUT);
            } else if (playerBlackjack) {
                settleRound("BlackJack! You win 3:2.", BlackjackOutcome.WIN, BLACKJACK_PAYOUT);
            } else {
                settleRound("Dealer has BlackJack. You lose.", BlackjackOutcome.LOSS, 0);
            }
            return;
        }

        statusLabel.setText("Your turn: Hit (H) or Stand (S).");
        setActionButtons(true);
    }

    private void onHit() {
        if (!roundActive) {
            return;
        }
        dealToPlayer(false);
        refreshRoundLabels();
        int playerValue = handValue(playerHand);
        if (playerValue > 21) {
            settleRound("Bust! You lose.", BlackjackOutcome.LOSS, 0);
        } else if (playerValue == 21) {
            onStand();
        }
    }

    private void onStand() {
        if (!roundActive) {
            return;
        }
        setActionButtons(false);
        revealDealerHoleCard();
        refreshRoundLabels();
        dealerTurn();
    }

    private void dealerTurn() {
        int dealerValue = handValue(dealerHand);
        if (dealerValue < DEALER_STAND_THRESHOLD) {
            PauseTransition delay = new PauseTransition(Duration.millis(260));
            delay.setOnFinished(event -> {
                dealToDealer(false);
                refreshRoundLabels();
                dealerTurn();
            });
            delay.play();
            return;
        }
        resolveRound();
    }

    private void resolveRound() {
        int playerValue = handValue(playerHand);
        int dealerValue = handValue(dealerHand);
        if (dealerValue > 21) {
            settleRound("Dealer busts. You win!", BlackjackOutcome.WIN, STANDARD_WIN_PAYOUT);
            return;
        }
        if (playerValue > dealerValue) {
            settleRound("You win this hand!", BlackjackOutcome.WIN, STANDARD_WIN_PAYOUT);
        } else if (playerValue < dealerValue) {
            settleRound("Dealer wins this hand.", BlackjackOutcome.LOSS, 0);
        } else {
            settleRound("Push. Bet returned.", BlackjackOutcome.PUSH, PUSH_PAYOUT);
        }
    }

    private void settleRound(String message, BlackjackOutcome outcome, double payoutMultiplier) {
        roundActive = false;
        revealDealerHoleCard();
        setActionButtons(false);

        double payout = currentBet * payoutMultiplier;
        double netChange = payout - currentBet;
        if (payout > 0) {
            storage.setBalance(storage.getBalance() + payout);
        }

        storage.recordBlackjackOutcome(outcome, netChange);
        storage.addScore("blackjack", (int) Math.round(netChange), payoutMultiplier, currentBet, payout);

        refreshBankAndStats();
        refreshRoundLabels();
        statusLabel.setText(message);

        if (storage.getBalance() <= 0) {
            statusLabel.setText("Bankrupt. End of game. Restart app to reset DB manually.");
        }
    }

    private void refreshBankAndStats() {
        balanceLabel.setText("Balance: $" + String.format("%.2f", storage.getBalance()));
        BlackjackStats stats = storage.getBlackjackStats();
        winsLabel.setText("Wins: " + stats.wins());
        lossesLabel.setText("Losses: " + stats.losses());
        pushesLabel.setText("Pushes: " + stats.pushes());
        netLabel.setText("Net: $" + String.format("%.2f", stats.netProfit()));
    }

    private void refreshRoundLabels() {
        roundLabel.setText("Round: " + roundCount);
        playerValueLabel.setText("Player: " + handValue(playerHand));
        if (dealerHoleHidden && dealerHand.size() > 1) {
            int visibleValue = handValue(List.of(dealerHand.get(1)));
            dealerValueLabel.setText("Dealer: " + visibleValue + " + ?");
        } else {
            dealerValueLabel.setText("Dealer: " + handValue(dealerHand));
        }
    }

    private void dealToPlayer(boolean hidden) {
        Card card = deck.draw();
        playerHand.add(card);
        playerCardsBox.getChildren().add(createAnimatedCard(card, hidden));
    }

    private void dealToDealer(boolean hidden) {
        Card card = deck.draw();
        dealerHand.add(card);
        ImageView cardView = createAnimatedCard(card, hidden);
        dealerCardViews.add(cardView);
        dealerCardsBox.getChildren().add(cardView);
    }

    private void revealDealerHoleCard() {
        dealerHoleHidden = false;
        if (!dealerCardViews.isEmpty() && !dealerHand.isEmpty()) {
            dealerCardViews.get(0).setImage(renderCardImage(dealerHand.get(0), false));
        }
    }

    private ImageView createAnimatedCard(Card card, boolean hidden) {
        ImageView cardView = new ImageView(renderCardImage(card, hidden));
        cardView.setFitWidth(CARD_WIDTH);
        cardView.setFitHeight(CARD_HEIGHT);
        cardView.setPreserveRatio(false);
        cardView.setTranslateY(-40);
        cardView.setTranslateX(-220);
        TranslateTransition transition = new TranslateTransition(Duration.millis(240), cardView);
        transition.setToX(0);
        transition.setToY(0);
        transition.play();
        return cardView;
    }

    private Image renderCardImage(Card card, boolean hidden) {
        Canvas cardCanvas = new Canvas(CARD_WIDTH, CARD_HEIGHT);
        GraphicsContext gc = cardCanvas.getGraphicsContext2D();

        gc.setFill(Color.web("#f8fafc"));
        gc.fillRoundRect(0, 0, CARD_WIDTH, CARD_HEIGHT, 12, 12);
        gc.setStroke(Color.web("#1f2937"));
        gc.setLineWidth(2);
        gc.strokeRoundRect(1, 1, CARD_WIDTH - 2, CARD_HEIGHT - 2, 12, 12);

        if (hidden) {
            gc.setFill(Color.web("#0b143b"));
            gc.fillRoundRect(8, 8, CARD_WIDTH - 16, CARD_HEIGHT - 16, 10, 10);
            gc.setStroke(Color.web("#eab308"));
            gc.strokeRoundRect(12, 12, CARD_WIDTH - 24, CARD_HEIGHT - 24, 8, 8);
            gc.setFill(Color.web("#eab308"));
            gc.fillText("MEOW", 25, 70);
        } else {
            Color suitColor = card.isRed() ? Color.web("#dc2626") : Color.web("#111827");
            gc.setFill(suitColor);
            gc.setFont(Font.font(16));
            gc.fillText(card.rank, 10, 20);
            gc.fillText(card.symbol(), 10, 38);

            gc.save();
            gc.translate(CARD_WIDTH, CARD_HEIGHT);
            gc.rotate(180);
            gc.fillText(card.rank, 10, 20);
            gc.fillText(card.symbol(), 10, 38);
            gc.restore();

            gc.setFill(suitColor);
            gc.setFont(Font.font(30));
            gc.fillText(card.symbol(), CARD_WIDTH / 2.0 - 10, CARD_HEIGHT / 2.0 + 10);
        }

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        WritableImage image = new WritableImage(CARD_WIDTH, CARD_HEIGHT);
        return cardCanvas.snapshot(params, image);
    }

    private int handValue(List<Card> hand) {
        int total = 0;
        int aces = 0;
        for (Card card : hand) {
            total += card.value;
            if ("A".equals(card.rank)) {
                aces++;
            }
        }
        while (total > 21 && aces > 0) {
            total -= 10;
            aces--;
        }
        return total;
    }

    private void runStepsSequentially(List<Runnable> steps, int stepDelayMs, Runnable done) {
        runStep(steps, 0, stepDelayMs, done);
    }

    private void runStep(List<Runnable> steps, int index, int stepDelayMs, Runnable done) {
        if (index >= steps.size()) {
            done.run();
            return;
        }
        steps.get(index).run();
        PauseTransition delay = new PauseTransition(Duration.millis(stepDelayMs));
        delay.setOnFinished(event -> runStep(steps, index + 1, stepDelayMs, done));
        delay.play();
    }

    private void setActionButtons(boolean inRound) {
        hitButton.setDisable(!inRound);
        standButton.setDisable(!inRound);
        dealButton.setDisable(inRound);
    }

    private double parseBet() {
        try {
            return Double.parseDouble(betField.getText().trim().replace(',', '.'));
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    private void configureSign() {
        Image signImage = loadImage("bjsign.webp");
        if (signImage == null || signImage.isError() || signImage.getWidth() <= 0) {
            signImage = loadImage("bj.png");
        }
        if (signImage == null) {
            signImage = new WritableImage(1, 1);
        }
        blackjackSignView.setImage(signImage);
        if (signImage.getWidth() > 0 && signImage.getHeight() > 0) {
            double cropWidth = signImage.getWidth() * 0.64;
            double cropHeight = signImage.getHeight() * 0.58;
            double x = (signImage.getWidth() - cropWidth) / 2.0;
            double y = (signImage.getHeight() - cropHeight) * 0.36;
            blackjackSignView.setViewport(new Rectangle2D(x, y, cropWidth, cropHeight));
        }

        ColorAdjust colorAdjust = new ColorAdjust();
        colorAdjust.setSaturation(0.24);
        colorAdjust.setContrast(0.18);
        colorAdjust.setBrightness(0.05);
        DropShadow glow = new DropShadow();
        glow.setRadius(20);
        glow.setColor(Color.web("#eab308", 0.58));
        glow.setInput(colorAdjust);
        blackjackSignView.setEffect(glow);
        signBadgeLabel.setText("♠ BLACKJACK ♥");
    }

    private Image loadImage(String resourceName) {
        try (var stream = MainApplication.class.getResourceAsStream(resourceName)) {
            if (stream == null) {
                return null;
            }
            return new Image(stream);
        } catch (Exception exception) {
            return null;
        }
    }

    private static class Deck {
        private final List<Card> cards;
        private int index;

        private Deck(List<Card> cards) {
            this.cards = cards;
            this.index = 0;
        }

        private static Deck shuffled() {
            String[] suits = {"hearts", "diamonds", "clubs", "spades"};
            String[] ranks = {"A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"};
            List<Card> cards = new ArrayList<>(52);
            for (String suit : suits) {
                for (String rank : ranks) {
                    int value = switch (rank) {
                        case "A" -> 11;
                        case "J", "Q", "K" -> 10;
                        default -> Integer.parseInt(rank);
                    };
                    cards.add(new Card(rank, suit, value));
                }
            }
            Collections.shuffle(cards);
            return new Deck(cards);
        }

        private Card draw() {
            if (index >= cards.size()) {
                Collections.shuffle(cards);
                index = 0;
            }
            return cards.get(index++);
        }
    }

    private record Card(String rank, String suit, int value) {
        private boolean isRed() {
            return "hearts".equals(suit) || "diamonds".equals(suit);
        }

        private String symbol() {
            return switch (suit) {
                case "hearts" -> "\u2665";
                case "diamonds" -> "\u2666";
                case "clubs" -> "\u2663";
                default -> "\u2660";
            };
        }
    }
}
