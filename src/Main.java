import game.poker.*;

/**
 * Main class to run the poker game.
 **/
public class Main {
    public static void main(String[] args) {
        // First arg: number of players
        // Second arg: money each player starts with
        // Third arg: amount of money big blind posts (small blind posts half
        // rounded down)
        Poker p = new Poker(10, 100, 10);
        
        // Use a GUI; remove or comment this statement to use the console
        p.initializeVisualizer();
        
        // Start the game
        p.play();
    }
}
