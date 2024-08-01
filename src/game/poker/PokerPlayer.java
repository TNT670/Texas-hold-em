package game.poker;

import card.Card;
import card.Deck;
import game.Player;
import java.util.ArrayList;
import java.util.List;

/**
 * A subclass of {@code game.Player} designed for the Texas hold 'em poker game.
 * 
 * The theory behind the calculations for hand strength and hand potential is
 * taken from
 * https://webdocs.cs.ualberta.ca/~jonathan/PREVIOUS/Grad/papp/node38.html and
 * https://webdocs.cs.ualberta.ca/~jonathan/PREVIOUS/Grad/papp/node40.html
 **/
public final class PokerPlayer extends Player {
    
    // Total money this player has
    int money;

    // Amount of money posted in a round
    int posted;

    // Whether this player has folded
    boolean folded;

    // Whether this player has checked in a round
    boolean checked;

    // Whether this player has raised in a round
    boolean raised;

    // Hand strength
    double ehs;
    
    // Poker game reference
    Poker game;

    /**
     * Constructs a new {@code PokerPlayer} instance.
     * 
     * @param game the {@code Poker} game this Player is a part of
     * @param name the name of this Player
     * @param money the amount of money this Player starts the game with
     **/
    PokerPlayer(Poker game, String name, int money) {
        super(name);
        this.money = money;
        this.game = game;
        
        // Set effective hand strength to -1 for bot initialization purposes
        ehs = -1;
    }

    /**
     * Returns a string representation of this Player.
     * 
     * @return a string containing information about this Player.
     **/
    @Override
    public String toString() {
        return "Player[name=" + name +
                    ";money=" + money +
                    ";posted=" + posted +
                    ";folded=" + folded +
                    ";hand=" + hand.get(0).toShortString() + hand.get(1).toShortString() + "]";
    }

    double effectiveHandStrength() {
        double handStrength = getHandStrength();
        //if (debug)
        //    System.out.printf("HS: %.6f%n", handStrength);
        if (ehs == -1) {
            double[] handPotential = handPotential();
            if (handPotential == null)
                return -1;
        //    if (debug)
        //        System.out.printf("Ppot: %.6f%n", handPotential[0]);
            ehs = handStrength + (1 - handStrength) * handPotential[0];
        }
        return ehs;
    }

    /**
     * Scores hole cards based on the Chen formula.
     * 
     * @return an {@code int} representing the strength of this Player's hole
     *         cards.
     **/
    int holeCardScore() {
        // Sort hand so higher value card is the second card (ace is highest)
        hand.sort(Card.ACE_HIGH_COMPARATOR);
        Card clow = hand.get(0), chigh = hand.get(1);
        
        // Get cards' intrinsic values (5 for five, 11 for jack, etc.)
        // 14 is used for ace (which internally has a value of 1)
        int rlow = clow.rank.intrinsicValue, rhigh = chigh.rank.intrinsicValue;
        if (rlow == 1)
            rlow = 14;
        if (rhigh == 1)
            rhigh = 14;

        // Initialize score variable based on high value card
        double score;
        switch (rhigh) {
            case 14: score = 10; break;
            case 13: score = 8; break;
            case 12: score = 7; break;
            case 11: score = 6; break;
            default: score = rhigh / 2.; break;
        }

        // Double score if both cards have same value (minimum 5 in this case)
        if (rlow == rhigh) {
            score *= 2;
            if (score < 5)
                score = 5;
        }
        // Increase score for matching suit
        if (clow.suit.equals(chigh.suit))
            score += 2;

        // Find difference between ranks; impose higher penalty for larger gaps
        int gapDiff = rhigh - rlow;
        if (gapDiff == 2) // One card gap, e.g. 4 and 6 (5 is the gap)
            score -= 1;
        else if (gapDiff == 3) // Two card gap, etc.
            score -= 2;
        else if (gapDiff == 4)
            score -= 4;
        else if (gapDiff >= 5)
            score -= 5;

        // For higher chances at straights, add a bonus point for a 0 or 1 card
        // gap if both cards are lower than a queen
        if ((gapDiff == 1 || gapDiff == 2) && rlow <= 11 && rhigh <= 11)
            score += 1;

        // Round half scores up
        return (int) Math.round(score + .1);
    }

    /**
     * Gets strength of hole cards based on simulated rounds.
     * 
     * @return the strength of the player's hole cards
     **/
    double getHandStrength() {
        int ahead = 0, tied = 0, behind = 0;
        int score = game.getHandScore(this);
        List<Card> d = new ArrayList<>(Deck.newDeck());
        d.removeAll(game.communityCards);
        d.removeAll(hand);

        // Construct a two card hand another player might hold.
        List<Card> x = new ArrayList<>();
        for (int k = 0; k < d.size() - 1; k++) {
            Card c1 = d.get(k);
            x.add(c1);
            for (int l = k + 1; l < d.size(); l++) {
                Card c2 = d.get(l);
                x.add(c2);

                int[] ranks = new int[13], suits = new int[4];
                x.forEach((Card c) -> {
                    ranks[c.rank.intrinsicValue-1]++;
                    suits[c.suit.ordinal()]++;
                });
                // Simulated score
                int simScore = game.getHandScore(x, suits, ranks);

                // Find how many times this player's score beats or loses to the
                // simulated score
                if (score > simScore)
                    ahead++;
                else if (score == simScore)
                    tied++;
                else
                    behind++;

                x.remove(c2);
            }
            x.remove(c1);
        }
        // Return as a ratio of wins to total rounds of simulation
        return (tied * .5 + ahead) / (ahead + tied + behind);
    }

    /**
     * Returns this player's winning potential based on cards in hand and any
     * community cards in play, calculated through Monte Carlo simulations.
     * 
     * @return an array where the first element is this player's positive
     *         winning potential and the second element is this player's
     *         negative winning potential
     */
    double[] handPotential() {
        if (game.communityCards.isEmpty())
            return null;
        
        int[][] handPotential = {
            // ahead  tied  behind
            {      0,    0,      0},  // ahead
            {      0,    0,      0},  // tied
            {      0,    0,      0}}; // behind
        
        int[] totalPotential = new int[3];
        int score = game.getHandScore(this), ahead = 0, tied = 1, behind = 2;
        List<Card> d = new ArrayList<>(Deck.newDeck());
        d.removeAll(game.communityCards);
        d.removeAll(hand);

        // Runs Monte Carlo simulations for card playouts.
        
        // List of cards in a simulated opponent's hand
        List<Card> simHand = new ArrayList<>();
        
        // List of simulated community cards (adds cards already part of the
        // game's community cards)
        List<Card> simBoard = new ArrayList<>(game.communityCards);
        for (int k = 0; k < d.size() - 1; k++) {
            Card c1 = d.get(k);
            simHand.add(c1);
            for (int l = 1; l < d.size(); l++) {
                Card c2 = d.get(l);
                simHand.add(c2);

                int[] ranks = new int[13], suits = new int[4];
                simHand.forEach((Card c) -> {
                    ranks[c.rank.intrinsicValue-1]++;
                    suits[c.suit.ordinal()]++;
                });
                game.communityCards.forEach((Card c) -> {
                    ranks[c.rank.intrinsicValue-1]++;
                    suits[c.suit.ordinal()]++;
                });
                int simScore = game.getHandScore(simHand, suits, ranks);

                int index;
                if (score > simScore)
                    index = ahead;
                else if (score == simScore)
                    index = tied;
                else
                    index = behind;

                if (game.communityCards.size() < 5) {
                    int limit = d.size();
                    // size < 4 during flop, meaning turn and river are simulated
                    // so decrement the limit to enumerate all pairs
                    if (game.communityCards.size() < 4)
                        limit--;
                    for (int m = 0; m < limit; m++) {
                        if (m == k || m == l)
                            continue;

                        Card river = d.get(m);
                        simBoard.add(river);

                        //DURING THE FLOP
                        if (game.communityCards.size() < 4) {
                            for (int n = m + 1; n < d.size(); n++) {
                                if (n == k || n == l)
                                    continue;
                                totalPotential[index]++;

                                Card turn = d.get(n);
                                simBoard.add(turn);

                                int[] myRanks = new int[13], mySuits = new int[4],
                                      oppRanks = new int[13], oppSuits = new int[4];
                                hand.forEach((Card c) -> {
                                    myRanks[c.rank.intrinsicValue-1]++;
                                    mySuits[c.suit.ordinal()]++;
                                });
                                simHand.forEach((Card c) -> {
                                    oppRanks[c.rank.intrinsicValue-1]++;
                                    oppSuits[c.suit.ordinal()]++;
                                });
                                simBoard.forEach((Card c) -> {
                                    myRanks[c.rank.intrinsicValue-1]++;
                                    mySuits[c.suit.ordinal()]++;
                                    oppRanks[c.rank.intrinsicValue-1]++;
                                    oppSuits[c.suit.ordinal()]++;
                                });
                                int best = game.getHandScore(hand, mySuits, myRanks),
                                    oppBest = game.getHandScore(simHand, oppSuits, oppRanks);

                                if (best > oppBest)
                                    handPotential[index][ahead]++;
                                else if (best == oppBest)
                                    handPotential[index][tied]++;
                                else
                                    handPotential[index][behind]++;

                                simBoard.remove(turn);
                            }
                        }
                        
                        // DURING THE TURN
                        else {
                            totalPotential[index]++;
                            int[] myRanks = new int[13], mySuits = new int[4],
                            oppRanks = new int[13], oppSuits = new int[4];
                            hand.forEach(c -> {
                                myRanks[c.rank.intrinsicValue-1]++;
                                mySuits[c.suit.ordinal()]++;
                            });
                            simHand.forEach(c -> {
                                oppRanks[c.rank.intrinsicValue-1]++;
                                oppSuits[c.suit.ordinal()]++;
                            });
                            simBoard.forEach(c -> {
                                myRanks[c.rank.intrinsicValue-1]++;
                                mySuits[c.suit.ordinal()]++;
                                oppRanks[c.rank.intrinsicValue-1]++;
                                oppSuits[c.suit.ordinal()]++;
                            });
                            int best = game.getHandScore(hand, mySuits, myRanks),
                                oppBest = game.getHandScore(simHand, oppSuits, oppRanks);

                            if (best > oppBest)
                                handPotential[index][ahead]++;
                            else if (best == oppBest)
                                handPotential[index][tied]++;
                            else
                                handPotential[index][behind]++;
                        }
                        simBoard.remove(river);
                    }
                }
                simHand.remove(c2);
            }
            simHand.remove(c1);
        }
        // Positive and negative potential
        double ppot = (handPotential[behind][ahead] + .5 * handPotential[behind][tied] + .5 * handPotential[tied][ahead])
                 / (totalPotential[behind] + .5 * totalPotential[tied]),
              npot = (handPotential[ahead][behind] + .5 * handPotential[tied][behind] + .5 * handPotential[ahead][tied])
                 / (totalPotential[ahead] + .5 * totalPotential[tied]);
        return new double[] {ppot, npot};
    }
}