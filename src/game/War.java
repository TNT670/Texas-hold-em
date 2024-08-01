package game;

import card.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class War {
    Player player1, player2;
    
    public War() {
        player1 = new Player("Player 1");
        player2 = new Player("Player 2");
        setup();
    }
    
    private void setup() {
        Deck d = Deck.newDeck();
        d.shuffle();
        d.dealCards(26, player1, player2);
        System.out.printf("Player 1's cards: %s%n", player1);
        System.out.printf("Player 2's cards: %s%n", player2);
        System.out.println();
    }
    
    public void play() {
        System.out.println("Starting the game.");
        Comparator<Card> comp = Card.ACE_HIGH_COMPARATOR;
        while (player1.handSize() != 0 && player2.handSize() != 0) {
            assert player1.handSize() + player2.handSize() == 52;
            Card c1 = player1.topCard(), c2 = player2.topCard();
            System.out.printf("Player 1 plays a(n) %s.%n", c1);
            System.out.printf("Player 2 plays a(n) %s.%n", c2);
            
            List<Card> extras = new ArrayList<>();
            int res = comp.compare(c1, c2);
            if (res == 0) {
                Card e1 = null, e2 = null;
                while (res == 0 && player1.handSize() != 0 && player2.handSize() != 0) {
                    System.out.println("The cards are tied! Each player sets aside three cards.");
                    for (int k = 0; k < 3 && player1.handSize() != 1; k++) {
                        Card x = player1.topCard();
                        System.out.printf("Player 1 sets aside a(n) %s.%n", x);
                        extras.add(x);
                    }
                    for (int k = 0; k < 3 && player2.handSize() != 1; k++) {
                        Card x = player2.topCard();
                        System.out.printf("Player 2 sets aside a(n) %s.%n", x);
                        extras.add(x);
                    }
                    
                    e1 = player1.topCard();
                    e2 = player2.topCard();
                    System.out.printf("Player 1 plays a(n) %s.%n", e1);
                    System.out.printf("Player 2 plays a(n) %s.%n", e2);
                    extras.add(e1);
                    extras.add(e2);
                    res = comp.compare(e1, e2);
                }
                if (res > 0) {
                    System.out.printf("The %s beats the %s. Player 1 takes the cards!%n", e1, e2);
                    player1.addCards(c1, c2);
                    player1.addCards(extras.toArray(Card[]::new));
                }
                if (res < 0) {
                    System.out.printf("The %s beats the %s. Player 2 takes the cards!%n", e2, e1);
                    player2.addCards(c1, c2);
                    player2.addCards(extras.toArray(Card[]::new));
                }
            }
            else {
                if (res > 0) {
                    System.out.printf("The %s beats the %s. Player 1 takes the cards!%n", c1, c2);
                    player1.addCards(c1, c2);
                    player1.addCards(extras.toArray(Card[]::new));
                }
                else {
                    System.out.printf("The %s beats the %s. Player 2 takes the cards!%n", c2, c1);
                    player2.addCards(c1, c2);
                    player2.addCards(extras.toArray(Card[]::new));
                }
            }
            System.out.printf("Player 1 %d - %d Player 2%n", player1.handSize(), player2.handSize());
            System.out.println();
        }
        if (player1.handSize() == 0)
            System.out.println("Player 2 wins!");
        else
            System.out.println("Player 1 wins!");
    }
}
