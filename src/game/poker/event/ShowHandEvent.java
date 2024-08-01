package game.poker.event;

import game.poker.PokerPlayer;
import java.util.List;

public class ShowHandEvent extends PokerEvent {
    public ShowHandEvent(List<PokerPlayer> players) {
        super(players);
    }
}
