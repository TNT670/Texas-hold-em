package game.poker.event;

import card.Card;
import game.poker.PokerPlayer;

public class DealCardEvent extends PokerEvent {
    public DealCardEvent(PokerPlayer player, Card card) {
        super(new Object[] {player, card});
    }
}
