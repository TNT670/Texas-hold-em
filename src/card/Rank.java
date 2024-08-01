package card;

/**
 * An enum representing a standard playing card's rank or numerical value.
 * 
 * Ace is represented by 1, 2-10 by their respective values, jack by 11, queen
 * by 12, and king by 13.
 **/
public enum Rank {
    ACE(1, "A"), TWO(2, "2"), THREE(3, "3"), FOUR(4, "4"), FIVE(5, "5"), SIX(6, "6"),
    SEVEN(7, "7"), EIGHT(8, "8"), NINE(9, "9"), TEN(10, "T"), JACK(11, "J"),
    QUEEN(12, "Q"), KING(13, "K");
    
    /**
     * The numerical value of a rank as described above.
     **/
    public final int intrinsicValue;
    
    /**
     * The one character symbol of a rank. {@code A} stands for ace, while the
     * values {@code 2}-{@code 9} are the respective values. {@code T} is ten,
     * {@code J} is jack, {@code Q} is queen, and {@code K} is king.
     **/
    public final String symbol;
    
    Rank(int i, String s) {
        intrinsicValue = i;
        symbol = s;
    }
}