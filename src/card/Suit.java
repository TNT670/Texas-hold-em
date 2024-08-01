package card;

/**
 * An enum representing a standing playing card's suit.
 **/
public enum Suit {
    SPADES("S"), HEARTS("H"), CLUBS("C"), DIAMONDS("D");
    
    /**
     * The one character symbol of a suit. {@code S} represents spades, {@code
     * H} is for hearts, {@code C} for clubs, and {@code D} for diamonds.
     */
    public final String symbol;
    
    Suit(String s) {
        symbol = s;
    }
}
