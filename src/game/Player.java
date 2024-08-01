package game;

import card.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Base card player class. Contains a few base functions for managing players'
 * hands (cards held).
 **/
public class Player {
    
    /**
     * The cards this Player is holding.
     **/
    protected final List<Card> hand;
    
    /**
     * The name of this Player.
     **/
    protected final String name;
    
    /**
     * Creates a new Player with the given name and no cards held in hand.
     * 
     * @param name this Player's name
     **/
    public Player(String name) {
        this.name = name;
        hand = new ArrayList<>();
    }
    
    /**
     * Returns the number of cards this Player is holding.
     * 
     * @return an {@code int} representing the number of cards held
     **/
    public int handSize() {
        return hand.size();
    }
    
    /**
     * Adds the given {@code Card}s to this Player's hand.
     * 
     * @param c a sequence or array of cards to add to hand
     **/
    public void addCards(Card... c) {
        hand.addAll(Arrays.asList(c));
    }
    
    /**
     * Adds the given {@code Card}s to this Player's hand.
     * 
     * @param c a collection of cards to add to hand
     **/
    public void addCards(Collection<Card> c) {
        hand.addAll(c);
    }
    
    /**
     * Returns the first {@code Card} in this Player's hand, or {@code null} if
     * this player has no cards in hand.
     * 
     * @return the first card held, or {@code null} if no cards are held
     **/
    public Card topCard() {
        if (hand.isEmpty())
            return null;
        return hand.get(0);
    }
    
    /**
     * Removes all {@code Card}s from this Player's hand.
     **/
    public void clearHand() {
        hand.clear();
    }
    
    /**
     * Returns the name of this Player.
     * 
     * @return this Player's name
     **/
    public String getName() {
        return name;
    }
    
    /**
     * Returns the cards this Player is holding.
     * 
     * @return a list of cards in this Player's hand
     **/
    public List<Card> getHand() {
        return hand;
    }
    
    /**
     * Returns a string representation of this Player.
     * 
     * @return a string containing information about this Player.
     **/
    @Override
    public String toString() {
        return "Player[name=" + name + ",hand=" + hand.toString() + "]";
    }
}
