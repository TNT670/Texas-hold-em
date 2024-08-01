package game.poker.event;
import game.poker.PokerPlayer;

public class AddChipsEvent extends PokerEvent {
    public AddChipsEvent(PokerPlayer p, int amount) {
        super(new Object[] {p, amount});
    }
}
