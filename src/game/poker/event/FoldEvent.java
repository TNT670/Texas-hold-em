package game.poker.event;

import card.Card;
import java.util.List;

public class FoldEvent extends PokerEvent {
    public FoldEvent(List<Card> hand, boolean me) {
        super(new Object[] {hand, me});
    }
}
