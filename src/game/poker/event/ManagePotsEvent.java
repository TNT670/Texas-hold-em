package game.poker.event;

import game.poker.Pot;
import java.util.List;

public class ManagePotsEvent extends PokerEvent {
    public ManagePotsEvent(List<Pot> pots) {
        super(pots);
    }
}
