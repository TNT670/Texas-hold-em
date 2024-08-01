package game.poker.event;

import java.util.EventListener;

public interface PokerEventListener extends EventListener {
    public void processEvent(PokerEvent ev);
}