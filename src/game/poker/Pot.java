package game.poker;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Pot {
    int value;
    Set<PokerPlayer> activePlayers;
    Map<PokerPlayer, Integer> contrib;

    Pot() {
        activePlayers = new HashSet<>();
        value = 0;
        contrib = new HashMap<>();
    }
    
    String prettyString() {
        return "$" + value + " between " + activePlayers;
    }

    @Override
    public String toString() {
        return "value=$".concat(String.valueOf(value)) + ";players=" + activePlayers;
    }
}