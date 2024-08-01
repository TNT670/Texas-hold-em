package game.poker.event;

public class BetAmountUpdateEvent extends PokerEvent {
    public BetAmountUpdateEvent(int newAmount) {
        super(newAmount);
    }
}
