package game.poker.event;

import game.poker.PokerPlayer;

public class ActivePlayerEvent extends PokerEvent {
    public ActivePlayerEvent(PokerPlayer p, boolean inRound) {
        super(new Object[] {p, inRound});
    }
}
