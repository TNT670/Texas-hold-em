package game.poker.event;

import game.poker.PokerPlayer;

public class RotateBlindsEvent extends PokerEvent {
    public RotateBlindsEvent(PokerPlayer bb, PokerPlayer sb) {
        super(new Object[] {bb, sb});
    }
}
