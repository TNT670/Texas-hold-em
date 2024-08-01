package card;

import java.util.Comparator;
import java.util.Objects;

/**
 * A representation of a playing card in a standard 52 card deck.
 **/
public class Card {
    
    /**
     * This {@code Card}'s rank.
     **/
    public final Rank rank;
    
    /**
     * This {@code Card}'s suit.
     **/
    public final Suit suit;
    
    /**
     * Whether this card can be seen by other players. Some applications may not
     * need to use this variable.
     **/
    public boolean visible;
    
    /**
     * Compares cards by rank using ace as the highest rank.
     **/
    public static final Comparator<Card> ACE_HIGH_COMPARATOR;
    
    static {
        ACE_HIGH_COMPARATOR = (c1, c2) -> {
            int c1Val = (c1.rank == Rank.ACE ? 14 : c1.rank.intrinsicValue);
            int c2Val = (c2.rank == Rank.ACE ? 14 : c2.rank.intrinsicValue);
            return c1Val - c2Val;
        };
    }
    
    /**
     * Creates a {@code Card} with the given rank and suit.
     * 
     * @param rank the card's rank
     * @param suit the card's suit
     **/
    public Card(Rank rank, Suit suit) {
        Objects.requireNonNull(rank);
        Objects.requireNonNull(suit);
        this.rank = rank;
        this.suit = suit;
    }
    
    /**
     * Creates a {@code Card} using a string representation of the rank and
     * suit.
     * 
     * The rank may be one of these values:
     * 
     * {@code "ACE"}<br>
     * {@code "TWO"}<br>
     * {@code "THREE"}<br>
     * {@code "FOUR"}<br>
     * {@code "FIVE"}<br>
     * {@code "SIX"}<br>
     * {@code "SEVEN"}<br>
     * {@code "EIGHT"}<br>
     * {@code "NINE"}<br>
     * {@code "TEN"}<br>
     * {@code "JACK"}<br>
     * {@code "QUEEN"}<br>
     * {@code "KING"}
     * 
     * The suit may be one of these values:
     * 
     * {@code "SPADES"}<br>
     * {@code "HEARTS"}<br>
     * {@code "CLUBS"}<br>
     * {@code "DIAMONDS"}
     * 
     * The strings are not case sensitive.
     * 
     * @param rank the card's rank
     * @param suit the card's suit
     **/
    public Card(String rank, String suit) {
        this(Rank.valueOf(rank.toUpperCase()),
             Suit.valueOf(suit.toUpperCase()));
    }
    
    /**
     * Creates a {@code Card} using a string representation of the rank and
     * suit.
     * 
     * The rank may be an integer value representing the card's rank, where 1 is
     * ace, 2-10 are the respective values, 11 is jack, 12 is queen, and 13 is
     * king.
     * 
     * The suit may be one of these values:
     * 
     * {@code "SPADES"}<br>
     * {@code "HEARTS"}<br>
     * {@code "CLUBS"}<br>
     * {@code "DIAMONDS"}
     * 
     * The strings are not case sensitive.
     * 
     * @param rank the card's rank
     * @param suit the card's suit
     **/
    public Card(int rank, String suit) {
        this(Card.toName(rank), suit);
    }
    
    /**
     * Maps an integer representation of card rank to a string representation.
     * 
     * @param i an integer representing a card's rank
     * @return a {@code String} representing the card's rank
     **/
    private static String toName(int i) {
        switch (i) {
            case 1:  return "ACE";
            case 2:  return "TWO";
            case 3:  return "THREE";
            case 4:  return "FOUR";
            case 5:  return "FIVE";
            case 6:  return "SIX";
            case 7:  return "SEVEN";
            case 8:  return "EIGHT";
            case 9:  return "NINE";
            case 10: return "TEN";
            case 11: return "JACK";
            case 12: return "QUEEN";
            case 13: return "KING";
            default: throw new IllegalArgumentException("Illegal number: " + i);
        }
    }
    
    /**
     * Returns a {@code String} representation of this {@code Card}.
     * 
     * @return a {@code String} representing this card
     **/
    @Override
    public String toString() {
        return "Card[rank=" + rank.toString() + ";suit=" + suit.toString() + "]";
    }
    
    /**
     * Returns an easily readable string representation of this {@code Card}.
     * 
     * For example, if this card is a jack of hearts, this method will return
     * the string "JACK of HEARTS".
     * 
     * @return a {@code String} representing this card in a more readable format
     **/
    public String toPrettyString() {
        return rank.toString().concat(" of ").concat(suit.toString());
    }
    
    /**
     * Returns a two-character {@code String} representing this card.
     * 
     * The first character represents this card's rank. {@code A} stands for ace,
     * while the values {@code 2}-{@code 9} are the respective values. {@code T}
     * is ten, {@code J} is jack, {@code Q} is queen, and {@code K} is king.
     * 
     * The second character is this card's suit. {@code S} represents spades,
     * {@code H} is for hearts, {@code C} for clubs, and {@code D} for diamonds.
     * 
     * @return a short two-character representation of this card
     **/
    public String toShortString() {
        return rank.symbol.concat(suit.symbol);
    }

    /**
     * Returns this card's hash code.
     * 
     * @return this card's hash code
     **/
    @Override
    public int hashCode() {
        return Objects.hash(rank, suit);
    }

    /**
     * Compares this {@code Card} with another {@code Object} and tests for
     * equality.
     * 
     * This method returns true if the given object is a {@code Card} instance
     * and has the same {@code Rank} and {@code Suit} as this {@code Card}. It
     * will also return true if the given object references the same object in
     * memory.
     * 
     * @param obj the object to compare
     * @return {@code true} if the two objects are equal; {@code false}
     *         otherwise
     **/
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        
        final Card other = (Card) obj;
        if (this.rank != other.rank)
            return false;
        return this.suit == other.suit;
    }
}
