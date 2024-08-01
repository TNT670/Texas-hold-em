package game.poker.event;

import card.Card;

public class CommunityCardEvent extends PokerEvent {
    public CommunityCardEvent(Card c) {
        super(c);
    }
}
