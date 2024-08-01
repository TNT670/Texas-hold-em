package card;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.List;

import game.Player;
import java.util.ArrayList;

/**
 * A class representing a deck of cards. This class extends {@code ArrayDeque}
 * to allow for access to both sides (top and bottom) of the deck when needed.
 **/
public class Deck extends ArrayDeque<Card> {
    private Deck() {}
    
    /**
     * Constructs a deck of 52 standard playing cards.
     * 
     * The {@code Card}s in this deck are always in the same order. They may be
     * shuffled using {@link Deck#shuffle()}.
     * 
     * @return a deck of 52 cards
     **/
    public static Deck newDeck() {
        Deck d = new Deck();
        for (Suit s : Suit.values()) {
            for (Rank r : Rank.values()) {
                Card c = new Card(r, s);
                d.add(c);
            }
        }
        return d;
    }
    
    /**
     * Randomly shuffles the cards in this deck.
     **/
    public void shuffle() {
        List<Card> l = new ArrayList<>(this);
        clear();
        Collections.shuffle(l);
        addAll(l);
    }
    
    /**
     * Distributes cards to one or more players.
     * 
     * This method will distribute cards until each player receives the given
     * number of cards or the deck becomes empty, in which some players may
     * receive less than the given amount.
     * 
     * @param numCards the number of cards to deal to each player
     * @param players the players receiving cards
     **/
    public void dealCards(int numCards, Player... players) {
        int size = this.size();
        for (int k = 0; k < Math.min(numCards, size / players.length + 1); k++) {
            for (Player p : players) {
                if (!isEmpty())
                    p.getHand().add(this.pollFirst());
            }
        }
    }
    
    /**
     * Distributes cards to one or more players.
     * 
     * This method will distribute cards until each player receives the given
     * number of cards or the deck becomes empty, in which some players may
     * receive less than the given amount.
     * 
     * @param numCards the number of cards to deal to each player
     * @param players a list of players receiving cards
     **/
    public void dealCards(int numCards, List<? extends Player> players) {
        int size = this.size();
        for (int k = 0; k < Math.min(numCards, size / players.size() + 1); k++) {
            players.forEach(p -> {
                if (!isEmpty())
                    p.getHand().add(this.pollFirst());
            });
        }
    }
}
