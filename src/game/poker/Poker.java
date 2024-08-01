package game.poker;

import java.util.ArrayList;
import java.util.List;
import card.*;
import game.poker.event.ActivePlayerEvent;
import game.poker.event.AddChipsEvent;
import game.poker.event.AwardPotEvent;
import game.poker.event.BetAmountUpdateEvent;
import game.poker.event.CommunityCardEvent;
import game.poker.event.DealCardEvent;
import game.poker.event.FoldEvent;
import game.poker.event.HandScoreUpdateEvent;
import game.poker.event.IdentifyMeEvent;
import game.poker.event.ManagePotsEvent;
import game.poker.event.PokerEvent;
import game.poker.event.ResetEvent;
import game.poker.event.RotateBlindsEvent;
import game.poker.event.ShowHandEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.Timer;

/**
 * Class to build and play a game of Texas hold 'em.
 **/
public final class Poker {
    // The list of players in the game.
    final List<PokerPlayer> players;
    
    // The pots in a round.
    private final List<Pot> pots;
    
    // The community cards in a round.
    final List<Card> communityCards;
    
    // The human player.
    private PokerPlayer me;
    
    // The deck of cards used.
    private Deck deck;
    
    // The amount the big blind posts.
    private final int bigBlind;
    
    // A random number generator.
    private Random rand;
    
    // The scanner for collecting player input.
    private final Scanner sc;
    
    // The number of players still in the game.
    private int activePlayers;
    
    // Controls whether to automatically pass through a round.
    private boolean special = false;
    
    // Controls output of debug information to the console.
    private final boolean debug = false;
    
    
    // Flag for whether GUI is used.
    private boolean vis = false;
    
    // Instance of the GUI for the poker game.
    PokerVisualizer visualizer;
    
    // GUI timer for collecting player input.
    private Timer waitTimer;
    
    // Action listener for collecting player input.
    private ActionListener visInputListener;
    
    // The variable the player's input is stored in when using the GUI.
    private String buttonInput;
    
    /**
     * Sets up a game of Texas hold 'em.
     * 
     * @param numPlayers the number of players in the game
     * @param startingMoney the amount of money everyone starts with
     * @param bigBlind the starting blind
     **/
    public Poker(int numPlayers, int startingMoney, int bigBlind) {
        players = new ArrayList<>();
        pots = new ArrayList<>();
        communityCards = new ArrayList<>();
        this.bigBlind = bigBlind;
        sc = new Scanner(System.in);
        addPlayers(numPlayers, startingMoney);
    }
    
    /**
     * Allows for the game to be played via a GUI. Call this function on the
     * constructed {@code Poker} instance to interact via a GUI.
     * 
     * If this method is not called, the game will run via the command line.
     **/
    public void initializeVisualizer() {
        visualizer = new PokerVisualizer(this);
        visualizer.createAndShowGUI();
        vis = true;
        
        // Construct an action listener for the GUI to use; player input will be
        // sent to the buttonInput variable.
        waitTimer = new Timer(100, null);
        visInputListener = ev -> {
            buttonInput = ev.getActionCommand();
            waitTimer.stop();
        };
        
        // Send the listener to the GUI.
        visualizer.registerActionListenerForInput(visInputListener);
    }
    
    /**
     * Adds players to this {@code Poker} game.
     * 
     * @param p the number of players to add
     * @param s the amount of money each player starts with
     **/
    private void addPlayers(int p, int s) {
        if (p < 2)
            throw new IllegalArgumentException("Need at least 2 players for poker");
        if (s < 2)
            throw new IllegalArgumentException("Players' starting amount must be greater than 1");
        for (int k = 1; k <= p; k++)
            players.add(new PokerPlayer(this, "Player " + k, s));
        me = players.get(0);
    }
    
    /**
     * Plays a game of Texas hold 'em.
     **/
    public void play() {
        firePokerEvent(new IdentifyMeEvent(me));
        for (int k = 1; !players.isEmpty(); k++) {
            firePokerEvent(new ActivePlayerEvent(null, false));
            System.out.println("========== Round " + k + " ==========");
            playRound();
            
            if (vis) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
            else if (players.contains(me)) {
                System.out.println();
                System.out.println("Press Enter to go to the next round...");
                sc.nextLine();
                sc.nextLine();
                System.out.println();
            }
            firePokerEvent(new ResetEvent());
        }
    }
    
    /**
     * Sets up and plays a full round of Texas hold 'em, including determining
     * a winner at the end.
     **/
    private void playRound() {
        setupRound();
        for (int k = 1; k <= 4; k++)
            playRound(k);
        findWinner();
    }
    
    /**
     * Sets up a round of Texas hold 'em.
     **/
    private void setupRound() {
        rand = new Random(System.nanoTime() - System.currentTimeMillis());
        
        communityCards.clear();
        
        // Rotate players (for posting blinds)
        PokerPlayer rotated = players.remove(players.size() - 1);
        players.add(0, rotated);
        
        // Remove players that are out of money and reset those still in the game
        for (Iterator<PokerPlayer> it = players.iterator(); it.hasNext();) {
            PokerPlayer player = it.next();
            player.folded = false;
            player.checked = false;
            player.posted = 0;
            player.getHand().clear();
            
            if (player.money == 0)
                it.remove();
        }
        if (players.size() == 1) {
            System.out.println("One player left; exiting");
            System.exit(0);
        }
        activePlayers = players.size();
        
        // Post blinds
        PokerPlayer bb = players.get(activePlayers - 1), sb = players.get(0);
        firePokerEvent(new RotateBlindsEvent(bb, sb));
        int bmoney = Math.min(bb.money, bigBlind);
        bb.money -= bmoney;
        bb.posted += bmoney;
        int smoney = Math.min(sb.money, bigBlind / 2);
        sb.money -= smoney;
        sb.posted += smoney;
        
        AddChipsEvent e1 = new AddChipsEvent(bb, bmoney);
        firePokerEvent(e1);
        AddChipsEvent e2 = new AddChipsEvent(sb, smoney);
        firePokerEvent(e2);
        
        System.out.printf("%s posted a big blind amount of $%d%n", bb.getName(), bmoney);
        System.out.printf("%s posted a small blind amount of $%d%n", sb.getName(), smoney);
        System.out.printf("Pot value: $%d%n%n", bmoney + smoney);
        
        // Create new deck, deal cards
        deck = Deck.newDeck();
        deck.shuffle();
        //adjustDeck();
        for (int k = 0; k < players.size() * 2; k++) {
            PokerPlayer p = players.get((k + 2) % players.size());
            Card c = deck.removeFirst();
            c.visible = (p == me);
            p.getHand().add(c);
            firePokerEvent(new DealCardEvent(p, c));
        }
        
        // Reset pots
        pots.clear();
        Pot startingPot = new Pot();
        pots.add(startingPot);
        
        // Reset special variable for everyone all in situations
        special = false;
    }
    
    /**
     * Plays a round of Texas hold 'em.
     * 
     * @param roundNum the round number: 1 for initial betting, 2 for flop, 3
     *          for turn, 4 for river
     **/
    private void playRound(int roundNum) {
        // If one player is left, do not play
        if (activePlayers == 1)
            return;
        int index = players.size() - 2;
        if (index < 0)
            index += players.size();
        PokerPlayer activePlayer = players.get(index);
        String aName = activePlayer.getName();
        int betAmount = roundNum == 1 ? bigBlind : 0;
        firePokerEvent(new BetAmountUpdateEvent(betAmount));
        // Total amount in the pot for display purposes
        int totalPosted = roundNum == 1 ? players.get(0).posted + players.get(activePlayers - 1).posted : pots.get(pots.size() - 1).value;
        
        if (roundNum == 2)
            dealFlop();
        else if (roundNum == 3)
            dealTurn();
        else if (roundNum == 4)
            dealRiver();
        if (me != null && players.contains(me))
            firePokerEvent(new HandScoreUpdateEvent(getHandName(getHandScore(me))));
        
        int CALL = 1, RAISE = 2, FOLD = 3;
        while (!activePlayer.checked && activePlayers > 1) {
            // If the active player has no money, they check automatically
            if (activePlayer.money == 0)
                activePlayer.checked = true;
            
            if (!activePlayer.folded && activePlayer.money > 0) {
                firePokerEvent(new ActivePlayerEvent(activePlayer, true));
                int choice = getChoice(activePlayer, betAmount, roundNum);

                if (choice == CALL) {
                    // Check if player adds no money to the pot
                    int diff = betAmount - activePlayer.posted;
                    if (diff == 0)
                        System.out.println(aName.concat(" checked."));
                    // Otherwise call if money was added
                    else {
                        int toAdd = Math.min(activePlayer.money, diff);
                        activePlayer.money -= toAdd;
                        totalPosted += toAdd;
                        activePlayer.posted += toAdd;
                        
                        firePokerEvent(new AddChipsEvent(activePlayer, toAdd));
                        System.out.printf("%s called and added $%d to the pot%n",
                                aName, toAdd);
                    }
                    activePlayer.checked = true;
                }
                else if (choice == RAISE) {
                    int entry;
                    if (activePlayer == me) {
                        if (vis)
                            entry = visualizer.getSliderValue();
                        else {
                            if (betAmount - me.posted >= me.money) {
                                System.out.println("You don't have enough money to raise.");
                                continue;
                            }
                            System.out.println("Enter the amount to raise by or 0 to cancel.");
                            System.out.printf("(current bet is $%d) ===>> $", betAmount);
                            entry = sc.nextInt();
                            while (entry < 0 || betAmount - me.posted + entry > me.money) {
                                System.out.print("Invalid amount.\nEnter a valid amount (or 0 to cancel) ===>> $");
                                entry = sc.nextInt();
                            }
                            if (entry == 0) continue;
                        }
                    }
                    else {
                        if (betAmount - activePlayer.posted >= activePlayer.money)
                            continue;
                        int range = rand.nextInt(100);
                        if (range < 65)
                            entry = rand.nextInt(Math.max((activePlayer.money - betAmount + activePlayer.posted) / 5, 1)) + 1;
                        else if (range >= 65 && range < 90)
                            entry = rand.nextInt(Math.max((int) ((activePlayer.money - betAmount + activePlayer.posted) / 1.5), 1)) + 1;
                        else
                            entry = rand.nextInt(activePlayer.money - betAmount + activePlayer.posted) + 1;
                    }
                    betAmount += entry;
                    firePokerEvent(new BetAmountUpdateEvent(betAmount));

                    int toAdd = betAmount - activePlayer.posted;
                    activePlayer.money -= toAdd;
                    totalPosted += toAdd;
                    activePlayer.posted = betAmount;

                    players.forEach(p -> {
                        if (!p.folded)
                            p.checked = false;
                    });
                    activePlayer.checked = true;

                    firePokerEvent(new AddChipsEvent(activePlayer, toAdd));
                    System.out.printf("%s raised the bet $%d to $%d%n",
                            aName, entry, betAmount);
                }
                else if (choice == FOLD) {
                    activePlayer.folded = true;
                    System.out.printf("%s folded.%n", aName);
                    firePokerEvent(new FoldEvent(activePlayer.getHand(), activePlayer == me));
                    
                    // If there are side pots, check if only one person is now
                    // participating in the current pot
                    if (pots.size() > 1) {
                        Pot p = pots.get(pots.size() - 1);
                        // If only one person is participating, everyone who was
                        // participating in the pot adds whatever they posted and
                        // the pot is awarded to the last participant
                        int activeLeft = (int) p.activePlayers
                                .stream()
                                .filter(x -> !x.folded)
                                .count();
                        if (activeLeft == 1) {
                            p.activePlayers.forEach(x -> {
                                p.value += x.posted;
                                x.posted = 0;
                            });

                            PokerPlayer player = p.activePlayers.stream()
                                    .filter(x -> !x.folded)
                                    .findAny()
                                    .orElseThrow();

                            firePokerEvent(new ManagePotsEvent(pots));
                            firePokerEvent(new AwardPotEvent(p, List.of(player), ""));
                            player.money += p.value;
                            System.out.printf("%s wins $%d!%n", player.getName(), p.value);
                            pots.remove(p);
                        }
                    }
                    activePlayers--;
                }
                else if (choice == 4) {
                    System.out.println("Current players:");
                    players.forEach(p -> System.out.printf("%s -- $%d %s%n", p.getName(), p.money, p.folded ? "(folded)" : ""));
                    System.out.println();
                    
                    System.out.printf("Your cards: %s%n", me.getHand());
                    System.out.printf("Community cards: %s%n", communityCards);
                    System.out.printf("Pots: %s%n", pots);
                    System.out.printf("Money in the current pot: $%d%n%n", totalPosted);
                    continue;
                }
                else {
                    System.out.println("Invalid choice given.");
                    continue;
                }
                System.out.printf("Pot value: $%d; Money held: $%d%n%n",
                                totalPosted, activePlayer.money);
            }
            
            // Turn goes to the next player
            index--;
            if (index < 0)
                index = players.size() - 1;
            activePlayer = players.get(index);
            aName = activePlayer.getName();
        }
        
        managePots();
        firePokerEvent(new ManagePotsEvent(pots));
        System.out.println("Pot values:");
        for (Pot p : pots) {
            System.out.printf("$%d between ", p.value);
            StringBuilder sb = new StringBuilder();
            p.activePlayers.forEach(s -> sb.append(s.getName()).append(", "));
            String str = sb.toString();
            System.out.print(str.substring(0, str.length() - 2));
            System.out.println();
        }
        System.out.println("\n");
        players.forEach(p -> {
            p.checked = false;
            p.raised = false;
            p.ehs = -1;
        });
    }
    
    /**
     * Deals the flop (the first three community cards).
     **/
    private void dealFlop() {
        System.out.println("----- Dealing the flop. -----");
        
        Card c1 = deck.pollFirst(), c2 = deck.pollFirst(), c3 = deck.pollFirst();
        System.out.printf("The cards in the flop are: %s, %s, and %s%n",
                c1.toPrettyString(), c2.toPrettyString(), c3.toPrettyString());
        communityCards.add(c1);
        firePokerEvent(new CommunityCardEvent(c1));
        communityCards.add(c2);
        firePokerEvent(new CommunityCardEvent(c2));
        communityCards.add(c3);
        firePokerEvent(new CommunityCardEvent(c3));
    }
    
    /**
     * Deals the turn (the fourth community card).
     **/
    private void dealTurn() {
        System.out.println("----- Dealing the turn. -----");
        
        Card c = deck.pollFirst();
        System.out.printf("The card in the turn is the %s%n", c.toPrettyString());
        communityCards.add(c);
        firePokerEvent(new CommunityCardEvent(c));
    }
    
    /**
     * Deals the river (the fifth community card).
     **/
    private void dealRiver() {
        System.out.println("----- Dealing the river. -----");
        
        Card c = deck.pollFirst();
        System.out.printf("The card in the river is the %s%n", c.toPrettyString());
        communityCards.add(c);
        firePokerEvent(new CommunityCardEvent(c));
    }
    
    /**
     * Collects the money each player posted into one or more pots.
     **/
    private void managePots() {
        // Sort players by money owned, then amount posted. Puts players with no
        // money (all in players) in front, in ascending order of how much they
        // bet
        List<PokerPlayer> sortedPlayers = players.stream()
                .filter(p -> p.posted > 0)
                .sorted(Comparator.comparingInt((PokerPlayer p) -> p.money)
                                  .thenComparingInt(p -> p.posted))
                .collect(Collectors.toList());
        
        for (ListIterator<PokerPlayer> it = sortedPlayers.listIterator(); it.hasNext();) {
            PokerPlayer p = it.next();
            int postAmount = p.posted;
            if (postAmount == 0)
                continue;
            
            // Check for a case where someone who matched a bet last round now
            // has no money (i.e. a player with only $30 matched a bet of
            // exactly $30). In that case a new side pot would not have been
            // created, so make one here
            for (PokerPlayer x : pots.get(pots.size() - 1).activePlayers) {
                if (x.money == 0) {
                    pots.add(new Pot());
                    break;
                }
            }
            
            // If current player p has no money (went all in), may need to add
            // side pots that other players with money can participate in
            if (p.money == 0) {
                Pot currentPot = pots.get(pots.size() - 1);
                
                // For each player remaining who posted money...
                ListIterator<PokerPlayer> it2 = sortedPlayers.listIterator(it.nextIndex() - 1);
                it2.forEachRemaining(x -> {
                    // Add however much the player short of money contributed
                    // Decrease that value from the amount they have yet to add
                    
                    // If x didn't post that much (because they folded, e.g.),
                    // add what they can
                    if (postAmount > x.posted) {
                        currentPot.value += x.posted;
                        currentPot.contrib.put(x, x.posted);
                        x.posted = 0;
                    }
                    else {
                        currentPot.value += postAmount;
                        currentPot.contrib.put(x, postAmount);
                        x.posted -= postAmount;
                    }
                    
                    // Anyone who did not fold now participates in this pot
                    if (!x.folded)
                        currentPot.activePlayers.add(x);
                });
                
                // Now add a new pot for the remaining players
                pots.add(new Pot());
            }
            // Otherwise, in this more straightforward case, everyone adds the
            // money they bet into the current pot
            else {
                Pot currentPot = pots.get(pots.size() - 1);
                ListIterator<PokerPlayer> it2 = sortedPlayers.listIterator(it.nextIndex() - 1);
                it2.forEachRemaining(x -> {
                    currentPot.value += x.posted;
                    currentPot.contrib.put(x, x.posted);
                    x.posted = 0;
                    if (!x.folded)
                        currentPot.activePlayers.add(x);
                });
                
                // Special case where big blind posts less than the small blind
                // but everyone else folds. In this case, current pot has no
                // participating players, meaning small blind gets however much
                // is in the pot (this would be small blind's post minus big
                // blind's post)
                List<PokerPlayer> special = sortedPlayers.stream()
                        .filter(x -> !x.folded)
                        .collect(Collectors.toList());
                
                // Big blind is at the end of the players list, small blind at
                // the start
                if (special.size() == 1 &&
                    special.get(0) == players.get(players.size() - 1) &&
                    pots.size() > 1) {
                    players.get(0).money += currentPot.value;
                    pots.remove(currentPot);
                }
                break;
            }
        }
        
        // There may be duplicate pots (i.e. two pots with exactly the same
        // active players) in cases where one or more players fold, so check and
        // merge any that exist
        for (int i = 0; i < pots.size() - 1; i++) {
            Pot p = pots.get(i), q = pots.get(i + 1);
            if (p.activePlayers.equals(q.activePlayers)) {
                p.value += q.value;
                pots.remove(q);
                i++;
            }
        }
        
        pots.forEach(p -> {
            // Remove players from pots if they folded
            p.activePlayers.removeIf(x -> x.folded);
            
            // Second condition is for the case where everyone except one folds
            // This causes the game to end slightly early and not display a winner
            if (p.activePlayers.size() == 1 && pots.size() > 1) {
                PokerPlayer x = p.activePlayers.toArray(PokerPlayer[]::new)[0];
                x.money += p.value;
                p.value = 0;
            }
        });
        pots.removeIf(p -> p.value == 0);
        
        // Check for only one player with money remaining or no players with
        // money, meaning game can jump through steps automatically with no
        // player input
        int i = (int) players.stream().filter(p -> p.money != 0 && !p.folded).count();
        if (i <= 1)
            special = true;
    }
    
    /**
     * Determines the winner of a round of Texas hold 'em.
     **/
    private void findWinner() {
        firePokerEvent(new ActivePlayerEvent(null, false));
        for (ListIterator<Pot> it = pots.listIterator(pots.size()); it.hasPrevious();) {
            // Starting with the last pot, award pots to winners
            Pot pot = it.previous();
            List<PokerPlayer> remaining = pot.activePlayers.stream()
                    .filter(p -> !p.folded)
                    .collect(Collectors.toList());
            if (remaining.size() == 1) {
                // One player in this pot remaining; award them the pot
                PokerPlayer p = remaining.get(0);
                firePokerEvent(new AwardPotEvent(pot, List.of(p), ""));
                System.out.printf("%s wins $%d!%n%n", p.getName(), pot.value);
                p.money += pot.value;
            }
            else {
                printCommunityCards();
                Map<PokerPlayer, Integer> scoreMap = new HashMap<>();
                
                // Display everyone's cards, populate map with players and their
                // hand scores
                firePokerEvent(new ShowHandEvent(remaining));
                remaining.forEach(p -> {
                    List<Card> hand = p.getHand();
                    Card c1 = hand.get(0), c2 = hand.get(1);
                    System.out.printf("%s's cards: %s, %s ", p.getName(),
                            c1.toPrettyString(), c2.toPrettyString());

                    int score = getHandScore(p);
                    scoreMap.put(p, score);
                    System.out.printf("(%s)%n", getHandName(score));
                });

                // Get the highest score from the map
                int maxScore = scoreMap.entrySet()
                        .stream()
                        .mapToInt(e -> e.getValue())
                        .max()
                        .orElse(-1);
                if (maxScore == -1)
                    System.out.println("MAX SCORE MAP: " + scoreMap);
                String handName = getHandName(maxScore);
                
                // Get the players with the highest scoring hand
                List<PokerPlayer> winners = scoreMap.entrySet()
                        .stream()
                        .filter(e -> e.getValue() == maxScore)
                        .map(e -> e.getKey())
                        .collect(Collectors.toList());
                // If only one winner, give them the whole pot
                if (winners.size() == 1) {
                    PokerPlayer winner = winners.get(0);
                    firePokerEvent(new AwardPotEvent(pot, List.of(winner), handName));
                    System.out.printf("%s wins $%d with %s!%n%n", winner.getName(), pot.value, handName);
                    winner.money += pot.value;
                }
                else {
                    // Resolve tie
                    List<PokerPlayer> newWinners = resolveTie(winners, maxScore);
                    
                    // If only one winner, give them the whole pot
                    if (newWinners.size() == 1) {
                        PokerPlayer winner = newWinners.get(0);
                        firePokerEvent(new AwardPotEvent(pot, List.of(winner), handName));
                        System.out.printf("%s wins $%d with %s!%n%n", winner.getName(), pot.value, handName);
                        winner.money += pot.value;
                    }
                    // Otherwise split the pot among the winners
                    else {
                        firePokerEvent(new AwardPotEvent(pot, newWinners, handName));
                        System.out.printf("There are multiple winners each receiving $%d from %s: ",
                                pot.value / newWinners.size(), handName);
                        StringBuilder sb = new StringBuilder();
                        for (PokerPlayer p : newWinners)
                            sb.append(p.getName()).append(", ");
                        String str = sb.toString();
                        System.out.println(str.substring(0, str.length() - 2).concat("\n\n"));
                        int x = pot.value / newWinners.size();
                        newWinners.forEach(p -> {
                            p.money += x;
                            pot.value -= x;
                        });
                        
                        // Excess money given out one at a time randomly
                        while (pot.value != 0) {
                            pot.value--;
                            newWinners.get(rand.nextInt(newWinners.size())).money++;
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Checks for a flush among the given distribution of card suits.
     * 
     * @param suits the distribution of suits among the cards
     * @return {@code true} if the distribution indicates a flush; {@code false}
     *         otherwise
     **/
    private boolean hasFlush(int[] suits) {
        // Check that at least five cards of any suit exist
        for (int i : suits) {
            if (i >= 5)
                return true;
        }
        return false;
    }
    
    /**
     * Checks for a straight among the given distribution of card suits.
     * 
     * @param ranks the distribution of ranks among the cards
     * @return {@code true} if the distribution indicates a straight; {@code
     *         false} otherwise
     **/
    private boolean hasStraight(int[] ranks) {
        // Check that cards forming a sequence of five ranks exists
        for (int k = 0; k < 10; k++) {
            if (ranks[k] > 0 && ranks[k+1] > 0 && ranks[k+2] > 0 && ranks[k+3] > 0 && ranks[(k+4) % 13] > 0)
                return true;
        }
        return false;
    }
    
    /**
     * Checks for a straight flush among the given distribution of card suits
     * and cards in hand.
     * 
     * @param suits the distribution of suits among the cards
     * @param hand the hole cards
     * @return {@code true} if the distribution indicates a straight flush;
     *         {@code false} otherwise
     **/
    private boolean hasStraightFlush(int[] suits, List<Card> hand) {
        // For each suit...
        for (Suit s : Suit.values()) {
            
            // Check that there are 5 or more cards of this suit
            if (suits[s.ordinal()] >= 5) {
                
                // List of all cards (hole plus community cards)
                List<Card> l = new ArrayList<>(communityCards);
                l.addAll(hand);
                
                // Remove cards that are not of this suit
                l.removeIf(c -> c.suit != s);
                
                // Now check that cards forming a sequence of five ranks remain
                int[] ranks = new int[13];
                l.forEach(c -> ranks[c.rank.intrinsicValue-1]++);
                for (int k = 0; k < 9; k++) {
                    if (ranks[k] == 1 && ranks[k+1] == 1 && ranks[k+2] == 1 && ranks[k+3] == 1 && ranks[k+4] == 1)
                        return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Checks for a royal flush among the given distribution of card suits and
     * cards in hand.
     * 
     * @param suits the distribution of suits among the cards
     * @param hand the hole cards
     * @return {@code true} if the distribution indicates a royal flush; {@code
     *         false} otherwise
     **/
    private boolean hasRoyalFlush(int[] suits, List<Card> hand) {
        // For each suit...
        for (Suit s : Suit.values()) {
            
            // Check that there are 5 or more cards of this suit
            if (suits[s.ordinal()] >= 5) {
                
                // List of all cards (hole plus community cards)
                List<Card> l = new ArrayList<>(communityCards);
                l.addAll(hand);
                
                // Remove cards that are not of this suit
                l.removeIf(c -> c.suit != s);
                
                // Now check that a ten, jack, queen, king, and ace remain
                int[] ranks = new int[13];
                l.forEach(c -> ranks[c.rank.intrinsicValue-1]++);
                return ranks[9] == 1 && ranks[10] == 1 && ranks[11] == 1 && ranks[12] == 1 && ranks[0] == 1;
            }
        }
        return false;
    }
    
    /**
     * Checks for a full house among the given distribution of card ranks.
     * 
     * @param ranks the distribution of ranks among the cards
     * @return {@code true} if the distribution indicates a full house; {@code
     *         false} otherwise
     **/
    private boolean hasFullHouse(int[] ranks) {
        // Sort the distribution of ranks so that the higher numbers appear at
        // the end of the array
        Arrays.sort(ranks);
        
        // Check for the last number being at least 3 (indicates 3 cards of a
        // rank) and the penultimate number being at least 2 (2 cards of a rank)
        //
        // This may return true if there is a four of a kind, but that would
        // still be a full house in a more permissive scenario.
        return ranks[12] >= 3 && ranks[11] >= 2;
    }
    
    /**
     * Checks for four of a kind among the given distribution of card ranks.
     * 
     * @param ranks the distribution of ranks among the cards
     * @return {@code true} if the distribution indicates four of a kind; {@code
     *         false} otherwise
     **/
    private boolean has4ofaKind(int[] ranks) {
        // Sort the distribution of ranks so that the higher numbers appear at
        // the end of the array
        Arrays.sort(ranks);
        
        // By definition the last number must be 4
        return ranks[12] == 4;
    }
    
    /**
     * Checks for three of a kind among the given distribution of card ranks.
     * 
     * @param ranks the distribution of ranks among the cards
     * @return {@code true} if the distribution indicates three of a kind;
     *         {@code false} otherwise
     **/
    private boolean has3ofaKind(int[] ranks) {
        // Sort the distribution of ranks so that the higher numbers appear at
        // the end of the array
        Arrays.sort(ranks);
        
        // The last number should therefore be at least a 3 for this to be true
        return ranks[12] >= 3;
    }
    
    /**
     * Checks for two pairs among the given distribution of card ranks.
     * 
     * @param ranks the distribution of ranks among the cards
     * @return {@code true} if the distribution indicates two pairs; {@code
     *         false} otherwise
     **/
    private boolean has2Pair(int[] ranks) {
        // Sort the distribution of ranks so that the higher numbers appear at
        // the end of the array
        Arrays.sort(ranks);
        
        // The last two numbers should therefore be at least 2 to indicate two
        // of one rank and two of another rank
        //
        // This may still be true in the presence of three or four of a kind,
        // but otherwise still indicates two pair in a more permissive scenario
        return ranks[11] == 2 && ranks[12] == 2;
    }
    
    /**
     * Checks for a pair among the given distribution of card ranks.
     * 
     * @param ranks the distribution of ranks among the cards
     * @return {@code true} if the distribution indicates a pair; {@code false}
     *         otherwise
     **/
    private boolean hasPair(int[] ranks) {
        // Sort the distribution of ranks so that the higher numbers appear at
        // the end of the array
        Arrays.sort(ranks);
        
        // The last number should be at least two to indicate two of any rank
        //
        // May still return true for other hands but otherwise indicates one
        // pair in a more permissive scenario
        return ranks[12] == 2;
    }
    
    /**
     * Resolves ties between players with the same best hand (e.g. two players
     * have a full house, etc.). This will determine which player has the better
     * hand in terms of who has cards of higher rank.
     * 
     * The player found to have cards of higher rank within their best hand will
     * be returned in a {@code List}. If multiple players are found to have the
     * same highest rank cards, those players will be returned within the list
     * and will share the pot.
     * 
     * @param tiedPlayers the players with the same best hand
     * @param score the best hand the tied players have
     * @return a list of players among those with the best hand with highest
     *         rank cards within that hand
     **/
    private List<PokerPlayer> resolveTie(List<PokerPlayer> tiedPlayers, int score) {
        if (debug)
            System.out.println(tiedPlayers);
        
        switch (score) {
            case 9: return tiedPlayers;
            case 8: return resolveStraightFlushTie(tiedPlayers);
            case 7: return resolve4ofaKindTie(tiedPlayers);
            case 6: return resolveFullHouseTie(tiedPlayers);
            case 5: return resolveFlushTie(tiedPlayers);
            case 4: return resolveStraightTie(tiedPlayers);
            case 3: return resolve3ofaKindTie(tiedPlayers);
            case 2: return resolve2PairTie(tiedPlayers);
            case 1: return resolvePairTie(tiedPlayers);
            case 0: return resolveJunkTie(tiedPlayers);
            default: throw new IllegalArgumentException("Unexpected score value: " + score);
        }
    }
    
    /**
     * Resolves a tie between multiple players with a straight flush.
     * 
     * @param tiedPlayers the players who each have a straight flush
     * @return a {@code List} of players with the highest straight flush
     **/
    private List<PokerPlayer> resolveStraightFlushTie(List<PokerPlayer> tiedPlayers) {
        Map<PokerPlayer, Integer> scoreMap = new HashMap<>();
        
        // For each tied player...
        tiedPlayers.forEach(p -> {
            // Find which suit makes the flush
            int[] suits = new int[4];
            communityCards.forEach(c -> suits[c.suit.ordinal()]++);
            p.getHand().forEach(c -> suits[c.suit.ordinal()]++);
            
            for (Suit s : Suit.values()) {
                if (suits[s.ordinal()] >= 5) {
                    // Suit s makes the flush; remove all cards that are not
                    // that suit
                    List<Card> l = new ArrayList<>(communityCards);
                    l.addAll(p.getHand());
                    l = l.stream().filter(c -> c.suit == s).collect(Collectors.toList());

                    // Create rank distribution of all remaining cards.
                    // Create record of the highest value card this player has.
                    int[] ranks = new int[13];
                    l.forEach(c -> ranks[c.rank.intrinsicValue-1]++);
                    for (int k = 12; k >= 4; k--) {
                        if (ranks[k] == 1) {
                            scoreMap.put(p, k);
                            break;
                        }
                    }
                }
            }
        });
        // Return the players with the highest rank card
        return highestPlayerByMapScore(scoreMap);
    }
    
    /**
     * Resolves a tie between multiple players with a four of a kind.
     * 
     * @param tiedPlayers the players who each have a four of a kind
     * @return a {@code List} of players with the highest four of a kind
     **/
    private List<PokerPlayer> resolve4ofaKindTie(List<PokerPlayer> tiedPlayers) {
        Map<PokerPlayer, Integer> scoreMap = new HashMap<>();
        // For each tied player...
        tiedPlayers.forEach(p -> {
            
            // Create distribution of card ranks (adjusted by 2 to account for
            // ace being on top)
            int[] ranks = new int[13];
            Consumer<Card> consumer = c -> {
                int i = c.rank == Rank.ACE ? 14 : c.rank.intrinsicValue;
                ranks[i-2]++;
            };
            communityCards.forEach(consumer);
            p.getHand().forEach(consumer);
            for (int k = 0; k < 13; k++) {
                // If four cards in a rank, put player and highest rank value in
                // map
                if (ranks[k] == 4) {
                    scoreMap.put(p, k);
                    break;
                }
            }
        });
        // Get player with highest rank four of a kind 
        List<PokerPlayer> list = highestPlayerByMapScore(scoreMap);
        int maxVal = scoreMap.get(list.get(0));
        
        if (debug)
            System.out.println("largest 4 kind: " + maxVal);
        
        // If players are still tied, determine winner by who holds the highest
        // rank card out of all the remaining cards
        if (list.size() > 1) {
            // Clear player-score map
            scoreMap.clear();
            // For the remaining players...
            list.forEach(p -> {
                List<Card> kickers = new ArrayList<>(communityCards);
                kickers.addAll(p.getHand());
                
                // Remove four of a kind cards (maxVal + 2 gives actual rank)
                kickers.removeIf(c -> c.rank.intrinsicValue == maxVal + 2 ||
                                      (c.rank == Rank.ACE && maxVal == 12));
                
                // Sort remaining cards by rank
                kickers.sort(Card.ACE_HIGH_COMPARATOR);
                
                // Get the highest rank remaining card and add to map
                Card hiCard = kickers.get(kickers.size() - 1);
                scoreMap.put(p, hiCard.rank == Rank.ACE ?
                                                     14 :
                                                     hiCard.rank.intrinsicValue);
            });
            
            list = highestPlayerByMapScore(scoreMap);
        }
        return list;
    }
    
    /**
     * Resolves a tie between multiple players with a full house.
     * 
     * The rank of the trio of cards is the main determining factor in who wins
     * the full house tie. If multiple players have the highest same rank trio,
     * then the rank of the pair is considered next.
     * 
     * @param tiedPlayers the players who each have a full house
     * @return a {@code List} of players with the best full house
     **/
    private List<PokerPlayer> resolveFullHouseTie(List<PokerPlayer> tiedPlayers) {
        Map<PokerPlayer, Integer> scoreMap = new HashMap<>();
        // For each tied player...
        tiedPlayers.forEach(p -> {
            
            // Create distribution of ranks among cards to see who holds highest
            // rank trio in the full house
            int[] ranks = new int[13];
            Consumer<Card> consumer = c -> {
                int i = c.rank == Rank.ACE ? 14 : c.rank.intrinsicValue;
                ranks[i-2]++;
            };
            communityCards.forEach(consumer);
            p.getHand().forEach(consumer);
            
            // Put rank of trio in map with player
            for (int k = 12; k >= 0; k--) {
                if (ranks[k] == 3) {
                    scoreMap.put(p, k);
                    break;
                }
            }
        });
        
        List<PokerPlayer> winners = highestPlayerByMapScore(scoreMap);
        if (debug)
            System.out.println("largest full house 3: " + scoreMap.get(winners.get(0)));
        
        // If multiple players have highest rank trio, check for highest rank
        // duo next
        if (winners.size() > 1) {
            Map<PokerPlayer, Integer> scoreMap2 = new HashMap<>();
            winners.forEach(p -> {
                // Create distribution of ranks among cards again
                int[] ranks = new int[13];
                Consumer<Card> consumer = c -> {
                    int i = c.rank == Rank.ACE ? 14 : c.rank.intrinsicValue;
                    ranks[i-2]++;
                };
                communityCards.forEach(consumer);
                p.getHand().forEach(consumer);
                
                // Add rank to new map if at least two cards exist and the rank
                // was not recorded in the old map
                for (int k = 12; k >= 0; k--) {
                    if (ranks[k] >= 2 && k != scoreMap.get(p)) {
                        scoreMap2.put(p, k);
                        break;
                    }
                }
            });
            
            winners = highestPlayerByMapScore(scoreMap2);
        }
        return winners;
    }
    
    /**
     * Resolves a tie between multiple players with a flush.
     * 
     * The player with the highest rank card in the flush wins the tie. If
     * multiple players have the same highest rank card, then the second highest
     * rank card is considered, then the third, etc.
     * 
     * @param tiedPlayers the players who each have a straight flush
     * @return a {@code List} of players with the highest straight flush
     **/
    private List<PokerPlayer> resolveFlushTie(List<PokerPlayer> tiedPlayers) {
        Map<PokerPlayer, List<Card>> scoreMap = new HashMap<>();
        
        // For each player...
        tiedPlayers.forEach(p -> {
            // Get suit that makes a flush
            int[] suits = new int[4];
            communityCards.forEach(c -> suits[c.suit.ordinal()]++);
            p.getHand().forEach(c -> suits[c.suit.ordinal()]++);
            Suit s = Arrays.stream(Suit.values())
                    .filter(t -> suits[t.ordinal()] >= 5)
                    .findAny()
                    .orElseThrow();
            
            // Of cards of that specific suit, get the top five in descending
            // order of rank
            List<Card> score = Stream.of(communityCards, p.getHand())
                    .flatMap(List<Card>::stream)
                    .filter(c -> c.suit == s)
                    .sorted(Card.ACE_HIGH_COMPARATOR.reversed())
                    .limit(5)
                    .collect(Collectors.toList());
            scoreMap.put(p, score);
        });
        
        // Sort players by their highest value cards in descending order
        Comparator<PokerPlayer> comp = Comparator.comparing(p -> {
            int x = scoreMap.get(p).get(0).rank.intrinsicValue;
            return x == 1 ? 14 : x;
        });
        for (int k = 1; k <= 4; k++) {
            int k0 = k;
            comp = comp.thenComparing(p -> {
                int x = scoreMap.get(p).get(k0).rank.intrinsicValue;
                return x == 1 ? 14 : x;
            });
        }
        tiedPlayers.sort(comp.reversed());
        if (debug) {
            System.out.println("highest flush ranks");
            tiedPlayers.forEach(p -> 
                    System.out.println(p.getName() + ": " + scoreMap.get(p)));
        }
        
        // Iterate through players with the 5 highest rank cards until we reach
        // a player with a lower rank card in their top 5.
        // That player and anyone else after are no longer in the running to win
        // the pot
        for (int i = 0; i < tiedPlayers.size() - 1; i++) {
            PokerPlayer p = tiedPlayers.get(i), q = tiedPlayers.get(i+1);
            List<Card> pcards = scoreMap.get(p), qcards = scoreMap.get(q);
            
            // Compare each player's highest rank, then second highest, etc.
            // cards
            for (int k = 0; k < 5; k++) {
                if (Card.ACE_HIGH_COMPARATOR
                        .compare(pcards.get(k), qcards.get(k)) > 0) {
                    // Set tiedPlayers to contain only those players before
                    // player q, who has a lower rank card in their top 5
                    tiedPlayers = tiedPlayers.subList(0, i+1);
                    break;
                }
            }
        }
        return tiedPlayers;
    }
    
    /**
     * Resolves a tie between multiple players with a straight.
     * 
     * @param tiedPlayers the players who each have a straight
     * @return a {@code List} of players with the highest straight
     **/
    private List<PokerPlayer> resolveStraightTie(List<PokerPlayer> tiedPlayers) {
        Map<PokerPlayer, Integer> scoreMap = new HashMap<>();
        // For each player...
        tiedPlayers.forEach(p -> {
            // Create distribution of card ranks
            int[] ranks = new int[13];
            communityCards.forEach(c -> ranks[c.rank.intrinsicValue-1]++);
            p.getHand().forEach(c -> ranks[c.rank.intrinsicValue-1]++);
            
            // Record highest index of sequence of 5 ranks
            for (int k = 9; k >= 0; k--) {
                if (ranks[k] >= 1 && ranks[k+1] >= 1 && ranks[k+2] >= 1 &&
                        ranks[k+3] >= 1 && ranks[(k+4)%13] >= 1) {
                    scoreMap.put(p, k);
                    break;
                }
            }
            
            if (debug)
                System.out.println(p.getName() + ": " + scoreMap.get(p));
        });
        return highestPlayerByMapScore(scoreMap);
    }
    
    /**
     * Resolves a tie between multiple players with three of a kind.
     * 
     * @param tiedPlayers the players who each have a three of a kind
     * @return a {@code List} of players with the highest rank three of a kind
     **/
    private List<PokerPlayer> resolve3ofaKindTie(List<PokerPlayer> tiedPlayers) {
        Map<PokerPlayer, Integer> scoreMap = new HashMap<>();
        
        // For each player...
        tiedPlayers.forEach(p -> {
            // Create distribution of card ranks
            int[] ranks = new int[13];
            Consumer<Card> consumer = c -> {
                int i = c.rank == Rank.ACE ? 14 : c.rank.intrinsicValue;
                ranks[i-2]++;
            };
            communityCards.forEach(consumer);
            p.getHand().forEach(consumer);
            
            // Add highest rank trio to the score map
            for (int k = 12; k >= 0; k--) {
                if (ranks[k] == 3) {
                    scoreMap.put(p, k);
                    break;
                }
            }
        });
        
        List<PokerPlayer> winners = highestPlayerByMapScore(scoreMap);
        if (debug)
            System.out.println("highest rank 3: " +
                    scoreMap.get(winners.get(0)));
        
        // If multiple players share the highest rank trio, look at the other
        // cards for who has the highest rank card
        if (winners.size() > 1) {
            Map<PokerPlayer, List<Card>> scoreMap2 = new HashMap<>();
            winners.forEach(p -> {
                // Of the cards in hand and community cards, filter out the trio
                // cards, sort by rank in descending order, then take the two
                // highest
                List<Card> cards = Stream.of(communityCards, p.getHand())
                        .flatMap(List<Card>::stream)
                        .filter(c -> {
                            int x = (c.rank == Rank.ACE ?
                                                     14 :
                                                     c.rank.intrinsicValue) - 2;
                            return x != scoreMap.get(p);
                        })
                        .sorted(Card.ACE_HIGH_COMPARATOR.reversed())
                        .limit(2)
                        .collect(Collectors.toList());
                scoreMap2.put(p, cards);
            });
            
            // Sort players by their highest value cards in descending order
            winners.sort(Comparator.comparing((PokerPlayer p) -> {
                int x = scoreMap2.get(p).get(0).rank.intrinsicValue;
                return x == 1 ? 14 : x;
            })
            .thenComparing(p -> {
                    int x = scoreMap2.get(p).get(1).rank.intrinsicValue;
                    return x == 1 ? 14 : x;
            })
            .reversed());
            
            if (debug)
                winners.forEach(p -> System.out.println(
                        p.getName() + ": " + scoreMap2.get(p)));
            
            // Iterate through players with the 2 highest rank cards until we
            // reach a player with a lower rank card in their top 2.
            // That player and anyone else after are no longer in the running to
            // win the pot
            for (int i = 0; i < winners.size() - 1; i++) {
                PokerPlayer p = winners.get(i), q = winners.get(i + 1);
                List<Card> pcards = scoreMap2.get(p), qcards = scoreMap2.get(q);

                // Compare each player's highest rank, then second highest,
                // cards
                for (int k = 0; k < 2; k++) {
                    if (Card.ACE_HIGH_COMPARATOR
                            .compare(pcards.get(k), qcards.get(k)) > 0) {
                        // Set winners list to contain only those players before
                        // player q, who has a lower rank card in their top 2
                        winners = winners.subList(0, i + 1);
                        break;
                    }
                }
            }
        }
        return winners;
    }
    
    /**
     * Resolves a tie between multiple players with two pairs.
     * 
     * The rank of the highest pair is the main determining factor in who wins
     * the tie. If multiple players have the highest rank pair, then the rank of
     * the second highest pair is considered.
     * 
     * @param tiedPlayers the players who each have two pairs
     * @return a {@code List} of players with the highest two pairs
     **/
    private List<PokerPlayer> resolve2PairTie(List<PokerPlayer> tiedPlayers) {
        Map<PokerPlayer, Integer> scoreMap = new HashMap<>();
        // For each player...
        tiedPlayers.forEach(p -> {
            // Create distribution of card ranks
            int[] ranks = new int[13];
            Consumer<Card> consumer = c -> {
                int i = c.rank == Rank.ACE ? 14 : c.rank.intrinsicValue;
                ranks[i-2]++;
            };
            communityCards.forEach(consumer);
            p.getHand().forEach(consumer);
            
            // Add highest rank pair to the score map
            for (int k = 12; k >= 0; k--) {
                if (ranks[k] == 2) {
                    scoreMap.put(p, k);
                    break;
                }
            }
        });
        
        List<PokerPlayer> winners = highestPlayerByMapScore(scoreMap);
        if (debug)
            System.out.println("highest rank 2: " +
                    scoreMap.get(winners.get(0)));
        
        // If multiple players share the highest rank pair, look at the lower
        // rank pair
        if (winners.size() > 1) {
            Map<PokerPlayer, Integer> scoreMap2 = new HashMap<>();
            winners.forEach(p -> {
                // Create distribution of card ranks
                int[] ranks = new int[13];
                Consumer<Card> consumer = c -> {
                    int i = c.rank == Rank.ACE ? 14 : c.rank.intrinsicValue;
                    ranks[i-2]++;
                };
                communityCards.forEach(consumer);
                p.getHand().forEach(consumer);
                
                // Add highest rank pair to score map that wasn't added
                // previously
                for (int k = 12; k >= 0; k--) {
                    if (ranks[k] == 2 && k != scoreMap.get(p)) {
                        scoreMap2.put(p, k);
                        break;
                    }
                }
            });
            
            winners = highestPlayerByMapScore(scoreMap2);
            if (debug)
                System.out.println("2nd highest rank 2: " +
                        scoreMap2.get(winners.get(0)));
            
            // If multiple players share the second highest rank pair, look at
            // the other cards for who has the highest rank card
            if (winners.size() > 1) {
                Map<PokerPlayer, Integer> scoreMap3 = new HashMap<>();
                // Of the cards in hand and community cards, filter out the 2
                // pair cards and get the highest rank card of those remaining
                winners.forEach(p -> {
                    int score = Stream.of(communityCards, p.getHand())
                            .flatMap(List<Card>::stream)
                            .filter(c -> {
                                // Take out the 2 pair cards
                                int x = (c.rank == Rank.ACE ?
                                                         14 :
                                                         c.rank.intrinsicValue)
                                        - 2;
                                return x != scoreMap.get(p) &&
                                       x != scoreMap2.get(p);
                            })
                            .max(Card.ACE_HIGH_COMPARATOR)
                            .orElseThrow()
                            .rank.intrinsicValue;
                    // Record rank in score map
                    scoreMap3.put(p, score == 1 ? 14 : score);
                });
                winners = highestPlayerByMapScore(scoreMap3);
                if (debug)
                    System.out.println("high card: " +
                            scoreMap3.get(winners.get(0)));
            }
        }
        return winners;
    }
    
    /**
     * Resolves a tie between multiple players with a pair.
     * 
     * @param tiedPlayers the players who each have a pair
     * @return a {@code List} of players with the highest rank pair
     **/
    private List<PokerPlayer> resolvePairTie(List<PokerPlayer> tiedPlayers) {
        Map<PokerPlayer, Integer> scoreMap = new HashMap<>();
        // For each player...
        tiedPlayers.forEach(p -> {
            // Create distribution of card ranks
            int[] ranks = new int[13];
            Consumer<Card> consumer = c -> {
                int i = c.rank == Rank.ACE ? 14 : c.rank.intrinsicValue;
                ranks[i-2]++;
            };
            communityCards.forEach(consumer);
            p.getHand().forEach(consumer);
            
            // Add highest rank pair to score map
            for (int k = 12; k >= 0; k--) {
                if (ranks[k] == 2) {
                    scoreMap.put(p, k);
                    break;
                }
            }
        });
        
        List<PokerPlayer> winners = highestPlayerByMapScore(scoreMap);
        if (debug)
            System.out.println("highest rank 2: " +
                    scoreMap.get(winners.get(0)));
        
        // If multiple players still share the highest rank pair, look at the
        // other cards for who has the higher rank card
        if (winners.size() > 1) {
            Map<PokerPlayer, List<Card>> scoreMap2 = new HashMap<>();
            winners.forEach(p -> {
                // Of the cards in hand and community cards, filter out the pair
                // cards, sort by rank in descending order, then take the three
                // highest
                List<Card> cards = Stream.of(communityCards, p.getHand())
                        .flatMap(List<Card>::stream)
                        .filter(c -> {
                            int x = (c.rank == Rank.ACE ?
                                                     14 :
                                                     c.rank.intrinsicValue) - 2;
                            return x != scoreMap.get(p);
                        })
                        .sorted(Card.ACE_HIGH_COMPARATOR.reversed())
                        .limit(3)
                        .collect(Collectors.toList());
                scoreMap2.put(p, cards);
            });
            
            // Sort players by their highest value cards in descending order
            Comparator<PokerPlayer> comp = Comparator.comparing(p -> {
                int x = scoreMap2.get(p).get(0).rank.intrinsicValue;
                return x == 1 ? 14 : x;
            });
            for (int k = 1; k <= 2; k++) {
                int k0 = k;
                comp = comp.thenComparing(p -> {
                    int x = scoreMap2.get(p).get(k0).rank.intrinsicValue;
                    return x == 1 ? 14 : x;
                });
            }
            winners.sort(comp.reversed());
            
            if (debug)
                winners.forEach(p -> System.out.println(
                        p.getName() + ": " + scoreMap2.get(p)));
            
            // Iterate through players with the 3 highest rank cards until we
            // reach a player with a lower rank card in their top 3.
            // That player and anyone else after are no longer in the running to
            // win the pot
            for (int i = 0; i < winners.size() - 1; i++) {
                PokerPlayer p = winners.get(i), q = winners.get(i + 1);
                List<Card> pcards = scoreMap2.get(p), qcards = scoreMap2.get(q);

                // Compare each player's highest rank, then second highest, etc.
                // cards
                for (int k = 0; k < 3; k++) {
                    if (Card.ACE_HIGH_COMPARATOR
                            .compare(pcards.get(k), qcards.get(k)) > 0) {
                        // Set winners list to contain only those players before
                        // player q, who has a lower rank card in their top 3
                        winners = winners.subList(0, i + 1);
                        break;
                    }
                }
            }
        }
        return winners;
    }
    
    /**
     * Resolves a tie between multiple players with junk.
     * 
     * The player with the highest rank card among all the cards wins the tie.
     * If multiple players have the same highest rank card, then the second
     * highest rank card is considered, then the third, etc.
     * 
     * @param tiedPlayers the players who each have a straight flush
     * @return a {@code List} of players with the highest straight flush
     **/
    private List<PokerPlayer> resolveJunkTie(List<PokerPlayer> tiedPlayers) {
        Map<PokerPlayer, List<Card>> scoreMap = new HashMap<>();
        // For each player...
        tiedPlayers.forEach(p -> {
            // Get their 5 highest cards by rank
            List<Card> cards = Stream.of(communityCards, p.getHand())
                    .flatMap(List<Card>::stream)
                    .sorted(Card.ACE_HIGH_COMPARATOR.reversed())
                    .limit(5)
                    .collect(Collectors.toList());
            scoreMap.put(p, cards);
        });
        
        // Sort players by their highest value cards in descending order
        Comparator<PokerPlayer> comp = Comparator.comparing(p -> {
            int x = scoreMap.get(p).get(0).rank.intrinsicValue;
            return x == 1 ? 14 : x;
        });
        for (int k = 1; k <= 4; k++) {
            int k0 = k;
            comp = comp.thenComparing(p -> {
                int x = scoreMap.get(p).get(k0).rank.intrinsicValue;
                return x == 1 ? 14 : x;
            });
        }
        tiedPlayers.sort(comp.reversed());
        
        if (debug)
            tiedPlayers.forEach(p -> System.out.println(
                    p.getName() + ": " + scoreMap.get(p)));
        
        // Iterate through players with the 5 highest rank cards until we reach
        // a player with a lower rank card in their top 5.
        // That player and anyone else after are no longer in the running to win
        // the pot
        for (int i = 0; i < tiedPlayers.size() - 1; i++) {
            PokerPlayer p = tiedPlayers.get(i), q = tiedPlayers.get(i+1);
            List<Card> pcards = scoreMap.get(p), qcards = scoreMap.get(q);
            
            // Compare each player's highest rank, then second highest, etc.
            // cards
            for (int k = 0; k < 5; k++) {
                if (Card.ACE_HIGH_COMPARATOR
                        .compare(pcards.get(k), qcards.get(k)) > 0) {
                    // Set tiedPlayers to contain only those players before
                    // player q, who has a lower rank card in their top 5
                    tiedPlayers = tiedPlayers.subList(0, i+1);
                    break;
                }
            }
        }
        return tiedPlayers;
    }
    
    /**
     * Returns the player with the highest score value in the given map.
     * 
     * @param map a map of players' hand scores
     * @return the player with the highest score
     **/
    private List<PokerPlayer> highestPlayerByMapScore(Map<PokerPlayer, Integer> map) {
        int max = map.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .orElseThrow()
                .getValue();
        return map.entrySet()
                .stream()
                .filter(e -> e.getValue() == max)
                .map(e -> e.getKey())
                .collect(Collectors.toList());
    }
    
    /**
     * Scores a hand based on the cards in the given player's hand and
     * community cards.
     * 
     * See {@link Poker#getHandScore(java.util.List, int[], int[])} for a
     * description of possible return values.
     * 
     * @param p the player whose hand to score
     * @return an {@code int} representing the score of the hand
     */
    int getHandScore(PokerPlayer p) {
        List<Card> hand = p.getHand();
        int[] ranks = new int[13];
        communityCards.forEach(c -> ranks[c.rank.intrinsicValue-1]++);
        hand.forEach(c -> ranks[c.rank.intrinsicValue-1]++);

        int[] suits = new int[4];
        communityCards.forEach(c -> suits[c.suit.ordinal()]++);
        hand.forEach(c -> suits[c.suit.ordinal()]++);
        
        return getHandScore(hand, suits, ranks);
    }
    
    /**
     * Scores a hand based on cards in a hand and community cards.
     * 
     * The possible return values are based on the highest hand that can be
     * made and are as follows:
     * 
     * <ul>
     *   <li>9 - royal flush</li>
     *   <li>8 - straight flush</li>
     *   <li>7 - four of a kind</li>
     *   <li>6 - full house</li>
     *   <li>5 - flush</li>
     *   <li>4 - straight</li>
     *   <li>3 - three of a kind</li>
     *   <li>2 - two pairs</li>
     *   <li>1 - one pair</li>
     *   <li>0 - junk (i.e. none of the above)</li>
     * </ul>
     * 
     * @param l list of cards in hand
     * @param suits a distribution of suits among cards in hand and community
     *              cards
     * @param ranks a distribution of ranks among cards in hand and community
     *              cards
     * @return an {@code int} representing the score of the hand
     **/
    int getHandScore(List<Card> l, int[] suits, int[] ranks) {
        if (hasRoyalFlush(suits, l))
            return 9;
        else if (hasStraightFlush(suits, l))
            return 8;
        else if (hasFlush(suits))
            return 5;
        else if (hasStraight(ranks))
            return 4;
        else if (has4ofaKind(ranks))
            return 7;
        else if (hasFullHouse(ranks))
            return 6;
        else if (has3ofaKind(ranks))
            return 3;
        else if (has2Pair(ranks))
            return 2;
        else if (hasPair(ranks))
            return 1;
        else return 0;
    }
    
    /**
     * Captures and returns the input from each player.
     * 
     * @param player the player whose turn it is
     * @param betAmount the current amount to call
     * @param roundNum the round number (
     * @return the player's action (1 for call, 2 for raise, 3 for fold)
     **/
    private int getChoice(PokerPlayer player, int betAmount, int roundNum) {
        // Automatic check for any player with no money or during an everyone
        // all in situation
        if (player.money == 0 || special)
            return 1;
        int choice = 0;
        
        // For the human player, gather their choice
        if (player == me) {
            // If using GUI, get input from GUI
            if (vis)
                choice = collectVisualizerInput();
            // Otherwise display info on and get input from command line
            else {
                boolean valid = false;
                while (!valid) {
                    if (roundNum >= 2)
                        printCommunityCards();
                    int score = getHandScore(me);
                    System.out.printf("Your cards: %s, %s %s%nYour money: $%d%n",
                            me.getHand().get(0).toPrettyString(), me.getHand().get(1).toPrettyString(),
                            score != 0 ? " (" + getHandName(score) + ")" : "", me.money);
                    if (betAmount != me.posted)
                        System.out.printf("Enter 1 to call $%d, 2 to raise, 3 to fold, or 4 for more info ===>> ",
                            betAmount - me.posted);
                    else
                        System.out.print("Enter 1 to check, 2 to raise, 3 to fold, or 4 for more info ===>> ");

                    if (sc.hasNextInt()) {
                        choice = sc.nextInt();
                        valid = true;
                    }
                    else {
                        System.out.println("You did not provide a valid input.");
                        sc.next();
                    }
                }
            }
            return choice;
        }
        // For computer players, calculate their input
        else {
            double ehs = player.effectiveHandStrength();
            if (debug)
                System.out.printf("%s's hand strength: %.6f%n", player.getName(), ehs);
            if (roundNum == 1) {
                int hs = player.holeCardScore();
                if (debug)
                    System.out.printf("%s's hand score: %d%n", player.getName(), hs);
                double foldLine = 0, raiseLine = .92;
                if (hs >= 10)
                    raiseLine -= (hs - 5) * .05;
                else if (hs <= 8)
                    foldLine = -.1 * hs + .9;
               
                double ratio = (betAmount - player.posted) / (double) player.money;
                if (ratio < 1E-6)
                    foldLine = 0;
                else if (ratio > .5)
                    foldLine += .5;
                else if (ratio > .3)
                    foldLine += .4;
                else if (ratio > .2)
                    foldLine += .3;
                else if (ratio > .1)
                    foldLine += .2;
                else if (ratio > .05)
                    foldLine += .1;
                
                double rand = Math.random();
                if (rand < foldLine)
                    choice = 3;
                else if (rand > raiseLine)
                    choice = 2;
                else
                    choice = 1;
            }
            else if (roundNum < 1) {
                double foldLimit = .66;
                double raiseLine = .95;
                double ppot = player.handPotential()[0];
                double foldLine = (betAmount - player.posted) / player.money * foldLimit;
                if (foldLine > foldLimit)
                    foldLine = foldLimit;
                
                double adjFoldLine;
                if (ehs < .6)
                    adjFoldLine = ehs - ppot;
                else {//if (ehs > .6 && ehs < .9) {
                    if (ppot > .5)
                        ppot = .5;
                    raiseLine -= (ppot * .75);
                    if (player.raised)
                        raiseLine += (1 - raiseLine) * 2 / 3;
                    adjFoldLine = .5 - ppot;
                }
                raiseLine = foldLine + (1 - foldLine) * raiseLine;
                foldLine = foldLine + (1 - foldLine) * adjFoldLine;
                double rand = Math.random();
                if (rand < foldLine)
                    choice = 3;
                else if (rand > raiseLine) {
                    player.raised = true;
                    choice = (betAmount - player.posted) < player.money ? 2 : 1;
                }
                else
                    choice = 1;
            }
            else {
                choice = rand.nextInt(4) + 1;
                if (choice == 4 && player.posted == betAmount)
                    choice = rand.nextInt(3) + 1;
                choice = choice == 1 ? 1 : choice - 1;
            }
            return choice;
        }
    }
    
    /**
     * Gathers the input from the GUI, if one is registered.
     * 
     * @return the player input from the GUI
     **/
    private int collectVisualizerInput() {
        waitTimer.start();
        while (waitTimer.isRunning());
        switch (buttonInput) {
            case "Call":  return 1;
            case "Raise": return 2;
            case "Fold":  return 3;
            default: throw new RuntimeException("Unexpected input: " + buttonInput);
        }
    }
    
    /**
     * Gets the name of a hand based on the given integer code.
     * 
     * @param id the number representing the hand
     * @return the corresponding name of the hand
     **/
    private String getHandName(int id) {
        switch (id) {
            case 0: return "junk";
            case 1: return "one pair";
            case 2: return "two pairs";
            case 3: return "three of a kind";
            case 4: return "a straight";
            case 5: return "a flush";
            case 6: return "a full house";
            case 7: return "four of a kind";
            case 8: return "a straight flush";
            case 9: return "a royal flush!";
            default: throw new IllegalArgumentException("Unrecognized id: " + id);
        }
    }
    
    /**
     * Prints the community cards onto the command line.
     **/
    private void printCommunityCards() {
        System.out.print("Community cards: ");
        int k;
        // Print the first k-1 community cards separated by a space
        for (k = 0; k < communityCards.size() - 1; k++)
            System.out.print(communityCards.get(k).toPrettyString() + ", ");
        
        // Print the last community card followed by a newline
        if (!communityCards.isEmpty())
            System.out.print(communityCards.get(k).toPrettyString());
        System.out.println();
    }
    
    /**
     * If utilizing the GUI, sends an event to the GUI to control its display
     * and animations.
     * 
     * @param ev the {@code PokerEvent} to send to the GUI
     **/
    private void firePokerEvent(PokerEvent ev) {
        if (vis)
            visualizer.firePokerEvent(ev);
    }
}
