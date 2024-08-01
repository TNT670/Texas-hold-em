package game.poker.event;

import game.poker.PokerPlayer;

public class IdentifyMeEvent extends PokerEvent {
    public IdentifyMeEvent(PokerPlayer me) {
        super(me);
    }
}
