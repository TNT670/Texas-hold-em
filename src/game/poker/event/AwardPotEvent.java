package game.poker.event;

import game.poker.PokerPlayer;
import game.poker.Pot;
import java.util.List;

public class AwardPotEvent extends PokerEvent {
    public AwardPotEvent(Pot pot, List<PokerPlayer> players, String handName) {
        super(new Object[] {pot, players, handName});
    }
}
