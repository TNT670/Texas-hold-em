package game.poker.event;

public class HandScoreUpdateEvent extends PokerEvent {
    public HandScoreUpdateEvent(String score) {
        super(score);
    }
}
