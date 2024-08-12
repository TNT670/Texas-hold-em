package game.poker;

import card.Card;
import game.poker.event.*;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class PokerVisualizer {
    // The Poker game this visualizer provides a GUI for.
    private final Poker game;
    
    // The players to draw.
    private List<PlayerBox> players;
    
    // The panel which displays most of the graphics.
    private GamePanel gamePanel;
    
    // Panels for displaying the buttons.
    private JPanel lowerPanel, card1, card2;
    
    // The main listener which listens for and responds to events.
    private PokerEventListener listener;
    
    // The timer which controls the repaint frequency of the panel.
    private Timer mainTimer;
    
    // References to the active player (whose turn it is), the human player, the
    // big blind, and the small blind.
    private PokerPlayer activePlayer, me, bb, sb;
    
    // The name of the hand to display.
    private String handName = "";
    
    // The buttons for calling, raising, and folding.
    private JButton callButton, raiseButton, confirmRaiseButton, foldButton;
    
    // The slider for setting an amount to raise.
    private JSlider valueSlider;
    
    // The color green to use for the background.
    private static final Color GREEN = new Color(0, 192, 0);
    
    // The current bet amount.
    private int betAmount;
    
    /**
     * Constructs a new {@code PokerVisualizer} for visual following of the
     * given {@code Poker} game.
     * 
     * @param game the game to provide a GUI for
     **/
    public PokerVisualizer(Poker game) {
        this.game = game;
        setup();
    }
    
    /**
     * Handles initialization of various aspects of the game.
     **/
    private void setup() {
        initGUIElements();
        initPlayerBoxes();
        initEvents();
        
        mainTimer = new Timer(16, ev -> gamePanel.repaint());
        mainTimer.start();
    }
    
    /**
     * Creates a "player box" for each player that displays name and money held.
     **/
    private void initPlayerBoxes() {
        players = game.players.stream().map(PlayerBox::new).collect(Collectors.toList());
        
        PlayerBox p1Box = players.get(0);
        int width = p1Box.width;
        if (players.size() % 2 == 0) {
            p1Box.x = gamePanel.getPreferredSize().width / 2 - 4 - p1Box.width;
            
            int i;
            for (i = 1; i <= players.size() / 2; i++) {
                players.get(i).x = gamePanel.getPreferredSize().width / 2 + 4 + (i - 1) * (8 + p1Box.width);
            }
            for (; i < players.size(); i++) {
                players.get(i).x = gamePanel.getPreferredSize().width / 2 + 4 - (8 + width) * (players.size() - i + 1);
            }
        }
        else {
            p1Box.x = (gamePanel.getPreferredSize().width - p1Box.width) / 2;
            
            int i;
            for (i = 1; i <= players.size() / 2; i++) {
                players.get(i).x = (gamePanel.getPreferredSize().width - width) / 2 + i * (8 + width);
            }
            for (; i < players.size(); i++) {
                players.get(i).x = (gamePanel.getPreferredSize().width - width) / 2 - (players.size() - i) * (8 + width);
            }
        }
        int minX = players.stream().reduce(Integer.MAX_VALUE, (u, p) -> Math.min(u, p.x), Math::min);
        if (minX < 8)
            players.forEach(p -> p.x -= minX - 8);
        gamePanel.drawables.addAll(players);
    }
    
    /**
     * Initializes the event listener.
     **/
    private void initEvents() {
        listener = new PokerEventManager();
    }
    
    /**
     * Initializes the GUI panels that display the graphics.
     **/
    private void initGUIElements() {
        // Main game panel
        gamePanel = new GamePanel();
        gamePanel.setLayout(new BorderLayout());
        
        // Lower panel with card layout; panel flips through cards depending on
        // whether player is deciding on a choice or deciding an amount to raise
        CardLayout layout = new CardLayout();
        lowerPanel = new JPanel(layout);
        lowerPanel.setPreferredSize(new Dimension(gamePanel.getPreferredSize().width, 40));
        lowerPanel.setOpaque(false);
        gamePanel.add(lowerPanel, BorderLayout.SOUTH);
        
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 6, 0, 6);
        c.anchor = GridBagConstraints.NORTHWEST;
        
        ActionListener disableButtonsListener = ev -> disableButtons();
        card1 = new JPanel(new GridBagLayout());
        card1.setOpaque(false);
        callButton = new JButton("Call");
        callButton.setActionCommand("Call");
        callButton.setFocusable(false);
        callButton.addActionListener(disableButtonsListener);
        raiseButton = new JButton("Raise");
        raiseButton.setFocusable(false);
        foldButton = new JButton("Fold");
        foldButton.setFocusable(false);
        foldButton.addActionListener(disableButtonsListener);
        disableButtons();
        
        c.gridwidth = 1;
        c.weightx = 1;
        card1.add(callButton, c);
        card1.add(raiseButton, c);
        c.gridwidth = GridBagConstraints.REMAINDER;
        card1.add(foldButton, c);
        
        card2 = new JPanel(new GridBagLayout());
        card2.setOpaque(false);
        
        JButton backButton = new JButton("Back");
        backButton.setFocusable(false);
        c.gridwidth = 1;
        c.weightx = 0;
        card2.add(backButton, c);
        
        valueSlider = new JSlider();
        valueSlider.setOpaque(false);
        c.weightx = 1;
        card2.add(valueSlider, c);
        
        JTextField valueField = new JTextField();
        valueField.setColumns(4);
        valueField.setHorizontalAlignment(JTextField.RIGHT);
        c.weightx = 0;
        card2.add(valueField, c);
        
        confirmRaiseButton = new JButton("Raise");
        confirmRaiseButton.setFocusable(false);
        c.gridwidth = GridBagConstraints.REMAINDER;
        card2.add(confirmRaiseButton, c);
        
        valueSlider.setMinimum(1);
        valueSlider.addChangeListener(ev -> {
            if (valueSlider.hasFocus())
                valueField.setText(String.valueOf(valueSlider.getValue()));
        });
        valueField.setText(String.valueOf(valueSlider.getValue()));
        
        // Track changes and update slider dynamically based on value in text
        // box
        valueField.getDocument().addDocumentListener(new DocumentListener() {
            void update() {
                String input = valueField.getText();
                if (input.matches("\\d+")) {
                    int num = Integer.parseInt(input);
                    if (num <= valueSlider.getMaximum())
                        valueSlider.setValue(num);
                }
            }
            @Override public void changedUpdate(DocumentEvent ev) { update(); }
            @Override public void removeUpdate(DocumentEvent ev)  { update(); }
            @Override public void insertUpdate(DocumentEvent ev)  { update(); }
        });
        raiseButton.addActionListener(ev -> layout.next(lowerPanel));
        ActionListener switchCardListener = ev -> layout.previous(lowerPanel);
        backButton.addActionListener(switchCardListener);
        confirmRaiseButton.addActionListener(switchCardListener);
        confirmRaiseButton.addActionListener(disableButtonsListener);
        
        layout.addLayoutComponent(card1, "main");
        layout.addLayoutComponent(card2, "raise");
        lowerPanel.add(card1);
        lowerPanel.add(card2);
    }
    
    /**
     * Disables the buttons for calling, raising, and folding (especially when
     * not the human player's turn).
     **/
    private void disableButtons() {
        callButton.setEnabled(false);
        raiseButton.setEnabled(false);
        foldButton.setEnabled(false);
    }
    
    /**
     * Updates GUI state.
     **/
    private void propertyUpdate() {
        // Enable buttons when it's the human player's turn
        boolean myTurn = activePlayer == me;
        callButton.setEnabled(myTurn);
        foldButton.setEnabled(myTurn);
        
        // Update call button based on the new bet amount and our status
        if (me != null && betAmount != me.posted)
            callButton.setText("Call $" + (betAmount - me.posted));
        else
            callButton.setText("Check");
        
        // Update raise button now if we can raise
        if (me != null && (betAmount - me.posted < me.money && myTurn)) {
            raiseButton.setEnabled(true);
            valueSlider.setMaximum(me.money + me.posted - betAmount);
        }
        else
            raiseButton.setEnabled(false);
    }
    
    /**
     * Finds the {@code PlayerBox} represented by the given {@code PokerPlayer}.
     * Usually used for calculating the position of certain drawings.
     * 
     * @param p the player's box to find
     * @return the corresponding {@code PlayerBox}
     **/
    private PlayerBox findPlayerBox(PokerPlayer p) {
        return players.stream().filter(box -> box.player == p).findAny()
                .orElseThrow(() -> new RuntimeException("No matching player found."));
    }
    
    /**
     * Processes a {@code PokerEvent}.
     * 
     * @param ev the event to process
     **/
    void firePokerEvent(PokerEvent ev) {
        listener.processEvent(ev);
    }
    
    /**
     * Adds the given {@code ActionListener} to the call, raise, and fold
     * buttons to track GUI updates.
     * 
     * @param al the {@code ActionListener} to track GUI updates with
     **/
    void registerActionListenerForInput(ActionListener al) {
        callButton.addActionListener(al);
        confirmRaiseButton.addActionListener(al);
        foldButton.addActionListener(al);
    }
    
    /**
     * Gets the value of the slider displayed when raising.
     * 
     * @return the slider value
     **/
    int getSliderValue() {
        return valueSlider.getValue();
    }
    
    /**
     * Creates and displays the window containing the graphics.
     **/
    public void createAndShowGUI() {
        JFrame frame = new JFrame();
        frame.add(gamePanel);
        
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setBackground(new Color(0, 192, 0));
        frame.setVisible(true);
    }
    
    /**
     * Interface to track which objects can be drawn onto the screen.
     **/
    interface Drawable {
        void draw(Graphics2D g);
    }
    
    /**
     * Class that draws the graphics and responds to events.
     **/
    private class GamePanel extends JPanel {
        
        // List of drawables -- objects that can be drawn to the screen.
        final List<Drawable> drawables = new CopyOnWriteArrayList<>();
        
        /**
         * Paints all drawable objects to the screen.
         **/
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            for (int k = drawables.size() - 1; k >= 0; k--) {
                try {
                    Drawable d = drawables.get(k);
                    d.draw(g2);
                } catch (IndexOutOfBoundsException ignore) {}
            }
            g2.dispose();
        }
        
        /**
         * Controls the animation for adding money to the pot.
         * 
         * @param p the player adding chips
         * @param amount the amount of money this player is adding 
         **/
        void drawAddChips(PokerPlayer p, int amount) {
            ChipPile pile = new ChipPile(p, amount);
            PlayerBox box = findPlayerBox(p);
            pile.x = box.x + 15;
            pile.y = box.y + box.height + 20;
            drawables.add(pile);
            
            Timer t = new Timer(8, null);
            t.addActionListener(new ActionListener() {
                int ticks = 0;
                @Override
                public void actionPerformed(ActionEvent ev) {
                    if (ticks == 31) {
                        t.stop();
                        ChipPile cp = (ChipPile) drawables
                                .stream()
                                .filter(x -> x instanceof ChipPile && x != pile)
                                .map(x -> (ChipPile) x)
                                .filter(x -> x.owner == p)
                                .findAny().orElse(null);
                        if (cp != null) {
                            cp.amount += pile.amount;
                            drawables.remove(pile);
                        }
                    }
                    else {
                        if (ticks < 5)
                            pile.y += 5;
                        else if (ticks < 10)
                            pile.y += 4;
                        else if (ticks < 17)
                            pile.y += 3;
                        else if (ticks < 27)
                            pile.y += 2;
                        else
                            pile.y += 1;
                        ticks++;
                    }
                }
            });
            t.start();
            
            // Allow the animation to finish before moving on
            while (t.isRunning());
        }
        
        /**
         * Controls the animation for a card being dealt to the given player.
         * 
         * @param p the player the given card is being distributed to
         * @param c the card being dealt
         **/
        void drawDealCard(PokerPlayer p, Card c) {
            DrawableCard card = new DrawableCard(c);
            card.img = card.img.getScaledInstance((int) (card.width * .058), (int) (card.height * .058), Image.SCALE_AREA_AVERAGING);
            card.x = getPreferredSize().width / 2 - card.img.getWidth(gamePanel) / 2;
            card.y = getPreferredSize().height + 25;
            PlayerBox box = findPlayerBox(p);
            gamePanel.drawables.add(card);
            
            Timer t = new Timer(8, null);
            t.addActionListener(new ActionListener() {
                int totalTicks = 30;
                int ticks = 0;
                double dx = (double) (card.x - box.x - (p.handSize() == 1 ? 0 : box.width / 2)) / totalTicks,
                       dy = (double) (card.y - box.y - box.height - 5) / totalTicks,
                       x = card.x,
                       y = card.y;
                @Override
                public void actionPerformed(ActionEvent ev) {
                    if (ticks == totalTicks) {
                        t.stop();
                    }
                    else {
                        x -= dx;
                        card.x = (int) x;
                        y -= dy;
                        card.y = (int) y;
                    }
                    ticks++;
                }
            });
            t.start();
        }
        
        /**
         * Controls the animation for posted money being collected into one or
         * more pots.
         * 
         * @param pots the pots that the posted money is being added to
         **/
        void drawManagePots(List<Pot> pots) {
            List<ChipPile> piles = drawables.stream()
                    .filter(x -> x instanceof ChipPile)
                    .map(x -> (ChipPile) x)
                    .collect(Collectors.toList());
            
            // Concept: Each pot has a set of players contributing. For each
            // pot, divide its value by number of contributors to get the amount
            // everyone contributes
            for (int i = 0; i < pots.size(); i++) {
                Pot p = pots.get(i);
                if (p.contrib.isEmpty())
                    continue;
                
                List<ChipPile> newPiles = new ArrayList<>();
                
                for (java.util.Map.Entry<PokerPlayer, Integer> e : p.contrib.entrySet()) {
                    PokerPlayer x = e.getKey();
                    int contrib = e.getValue();
                    ChipPile old = piles.stream()
                            .filter(c -> c.owner == x)
                            .findFirst()
                            .orElseThrow();
                    ChipPile newPile = new ChipPile(x, contrib);
                    newPile.x = old.x;
                    newPile.y = old.y;
                    newPiles.add(newPile);
                    drawables.add(newPile);
                    
                    old.amount -= contrib;
                    if (old.amount == 0)
                        drawables.remove(old);
                }
                
                int drawInterval = i;
                Timer t = new Timer(8, null);
                t.addActionListener(new ActionListener() {
                    int ticks = 0;

                    @Override
                    public void actionPerformed(ActionEvent ev) {
                        if (ticks == 30) {
                            t.stop();
                            for (ChipPile pile : newPiles) {
                                drawables.remove(pile);
                            }
                            drawables.removeIf(x -> x instanceof DrawablePot &&
                                    ((DrawablePot) x).pot == p);
                            DrawablePot pot = new DrawablePot(p);
                            pot.x = 480 + 40 * drawInterval;
                            pot.y = 300;
                            drawables.add(pot);
                        } else {
                            for (ChipPile pile : newPiles) {
                                pile.x += (480 + 40 * drawInterval - pile.x) / 9;
                                pile.y += (300 - pile.y) / 9;
                            }
                            ticks++;
                        }
                    }
                });
                t.start();
                
                // Allow the animation to finish before moving on
                while (t.isRunning());
            }
            
            pots.forEach(p -> p.contrib.clear());
            drawables.removeIf(d -> d instanceof ChipPile);
        }
        
        /**
         * Controls the animation for community cards being dealt on the table.
         * 
         * @param c the card being dealt to the table
         **/
        void drawCommunityCard(Card c) {
            c.visible = true;
            double scale = .152;
            DrawableCard card = new DrawableCard(c);
            card.img = card.img.getScaledInstance((int) (card.width * scale), (int) (card.height * scale), Image.SCALE_AREA_AVERAGING);
            card.x = getPreferredSize().width / 2 - card.img.getWidth(gamePanel) / 2;
            card.y = getPreferredSize().height + 25;
            gamePanel.drawables.add(card);
            Timer t = new Timer(8, null);
            t.addActionListener(new ActionListener() {
                int totalTicks = 30;
                int ticks = 0;
                double dx = (double) (card.x - 50 - (12 + card.width * scale) * (game.communityCards.size() - 1)) / totalTicks,
                       dy = (double) (card.y - 330) / totalTicks,
                       x = card.x,
                       y = card.y;
                @Override
                public void actionPerformed(ActionEvent ev) {
                    if (ticks == totalTicks) {
                        t.stop();
                    }
                    else {
                        x -= dx;
                        card.x = (int) x;
                        y -= dy;
                        card.y = (int) y;
                    }
                    ticks++;
                }
            });
            t.start();
            
            // Allow the animation to finish before moving on
            while (t.isRunning());
        }
        
        /**
         * Controls the animation for a player folding their hand (hole cards).
         * 
         * @param hand the cards being folded
         **/
        void drawFold(List<Card> hand) {
            List<DrawableCard> cards = drawables
                    .stream()
                    .filter(x -> x instanceof DrawableCard)
                    .map(x -> (DrawableCard) x)
                    .filter(x -> hand.contains(x.card))
                    .collect(Collectors.toList());
            Timer t = new Timer(8, null);
            t.addActionListener(new ActionListener() {
                int totalTicks = 60;
                int ticks = 0;
                @Override
                public void actionPerformed(ActionEvent ev) {
                    if (ticks == totalTicks) {
                        t.stop();
                        for (DrawableCard card : cards)
                            drawables.remove(card);
                    }
                    else {
                        for (DrawableCard card : cards) {
                            card.alpha = (float) (totalTicks - ticks) / (float) totalTicks;
                            card.y += 2;
                        }
                        ticks++;
                    }
                }
            });
            t.start();
            
            // Allow the animation to finish before moving on
            while (t.isRunning());
        }
        
        /**
         * Controls the animation for cards being revealed to players.
         * 
         * @param players a list of players showing their hand
         **/
        void drawShowHand(List<PokerPlayer> players) {
            List<Card> cards = players
                    .stream()
                    .flatMap(p -> p.getHand().stream())
                    .collect(Collectors.toList());
            for (int k = 0; k < drawables.size(); k++) {
                Drawable d = drawables.get(k);
                if (!(d instanceof DrawableCard)) continue;
                DrawableCard c = (DrawableCard) d;
                if (!cards.contains(c.card)) continue;
                c.card.visible = true;
                c.replaceImage();
                c.img = c.img.getScaledInstance((int) (c.width * .058), (int) (c.height * .058), Image.SCALE_AREA_AVERAGING);
            }
        }
        
        /**
         * Controls the animation for a pot being awarded to one or more
         * players.
         * 
         * @param pot the pot being awarded
         * @param list the list of players winning the pot
         * @param handName the name of the hand the players won the pot with
         **/
        void drawAwardPot(Pot pot, List<PokerPlayer> list, String handName) {
            DrawablePot p = drawables
                    .stream()
                    .filter(x -> x instanceof DrawablePot)
                    .map(x -> (DrawablePot) x)
                    .filter(d -> d.pot == pot)
                    .findAny()
                    .orElseThrow(() -> new RuntimeException("No matching drawable pot found"));
            drawables.remove(p);
            List<ChipPile> awards = new ArrayList<>();
            for (PokerPlayer player : list) {
                ChipPile c = new ChipPile(player, pot.value / list.size());
                c.x = p.x;
                c.y = p.y;
                awards.add(c);
                drawables.add(c);
                
                PlayerBox box = findPlayerBox(player);
                Text t = new Text(handName);
                t.x = box.x + 4;
                t.y = box.y + box.height + 80;
                drawables.add(t);
            }
            
            Timer t = new Timer(8, null);
            t.addActionListener(new ActionListener() {
                int ticks = 0;                
                @Override
                public void actionPerformed(ActionEvent ev) {
                    if (ticks == 30) {
                        t.stop();
                        for (ChipPile c : awards)
                            drawables.remove(c);
                    }
                    else {
                        for (ChipPile c : awards) {
                            PlayerBox box = findPlayerBox(c.owner);
                            c.x -= (c.x - box.x - 5) / 9;
                            c.y -= (c.y - box.y - box.height) / 9;
                        }
                        ticks++;
                    }
                }
            });
            t.start();
            while (t.isRunning());
        }
        
        /**
         * Returns the double buffering state of this {@code JPanel}.
         * 
         * @return whether this {@code JPanel} is double buffered; {@code true}
         *         by default
         **/
        @Override
        public boolean isDoubleBuffered() {
            return true;
        }
        
        /**
         * Returns the preferred size of this {@code JPanel}.
         * 
         * @return this {@code JPanel}'s preferred size
         **/
        @Override
        public Dimension getPreferredSize() {
            return new Dimension(1000, 600);
        }
        
        /**
         * Returns the background color of this {@code JPanel}.
         * 
         * @return this {@code JPanel}'s background color
         **/
        @Override
        public Color getBackground() {
            return GREEN;
        }
    }
    
    /**
     * Class that displays a pile of chips (currently a text box with a value
     * that indicates the amount of money in the pile).
     **/
    class ChipPile implements Drawable {
        PokerPlayer owner;
        int amount, x, y;

        /**
         * Constructs a new {@code ChipPile}.
         * 
         * @param p the player this {@code ChipPile} belongs to
         * @param a the amount of money in the pile
         **/
        ChipPile(PokerPlayer p, int a) {
            owner = p;
            amount = a;
        }

        /**
         * Draws this {@code ChipPile} to the screen.
         * 
         * @param g a {@code Graphics2D} object from a {@code JPanel} or other
         *          {@code Component}
         **/
        @Override
        public void draw(Graphics2D g) {
            g.drawString("$" + amount, x, y);
        }

        /**
         * Returns a {@code String} representation of this {@code ChipPile}.
         * 
         * @return this {@code ChipPile} represented as a {@code String}
         **/
        @Override
        public String toString() {
            return "ChipPile[amount=" + amount + "]";
        }
    }
    
    /**
     * Class that displays a playing card on the table.
     * 
     * Face-down cards are shown as a generic card back, while face-up cards are
     * shown as a card front with its display based on the {@code Card} given in
     * the constructor.
     **/
    class DrawableCard implements Drawable {
        // The card to display
        Card card;

        // The position and dimensions of the card on the screen
        int x, y, width, height;

        // The alpha value used when displaying the card
        float alpha = 1f;

        // The image to display, pulled from the file system
        Image img;

        /**
         * Constructs a new {@code DrawableCard}.
         * 
         * @param c the card this {@code DrawableCard} represents
         **/
        DrawableCard(Card c) {
            card = c;
            replaceImage();
        }

        /**
         * Changes this {@code DrawableCard}'s display based on whether it is
         * face up or face down.
         **/
        final void replaceImage() {
            try {
                String path;
                if (card.visible)
                    path = "cardimages/PNG/" + card.toShortString() + ".png";
                else
                    path = "cardimages/PNG/blue_back.png";
                BufferedImage image = ImageIO.read(new File(path));
                width = image.getWidth();
                height = image.getHeight();
                img = image;
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        /**
         * Draws this {@code DrawableCard} to the screen.
         * 
         * @param g a {@code Graphics2D} object from a {@code JPanel} or other
         *          {@code Component}
         **/
        @Override
        public void draw(Graphics2D g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.SrcOver.derive(alpha));
            g2.drawImage(img, x, y, gamePanel);
            g2.dispose();
        }
    }
    
    /**
     * Class that displays a pot of money (currently a text box with a value
     * that indicates the amount of money in the pile).
     **/
    class DrawablePot implements Drawable {
        // The location of this DrawablePot on the screen
        int x, y;

        // The pot to display
        Pot pot;

        /**
         * Constructs a new {@code DrawablePot}.
         * 
         * @param p the {@code Pot} this {@code DrawablePot} will represent
         **/
        DrawablePot(Pot p) {
            pot = p;
        }

        /**
         * Draws this {@code DrawablePot} to the screen.
         * 
         * @param g a {@code Graphics2D} object from a {@code JPanel} or other
         *          {@code Component}
         **/
        @Override
        public void draw(Graphics2D g) {
            g.drawString("$" + pot.value, x, y);
        }

        /**
         * Returns a {@code String} representation of this {@code DrawablePot}.
         * 
         * @return this {@code DrawablePot} represented as a {@code String}
         **/
        @Override
        public String toString() {
            return "DrawablePot[pot=" + pot.value + "]";
        }
    }
    
    /**
     * Class that displays a string of text.
     **/
    class Text implements Drawable {
        // The text string to draw
        String text;

        // The location of the text string on the screen
        int x, y;

        /**
         * Constructs a new {@code Text}.
         * 
         * @param t the string of text to display
         **/
        Text(String t) {
            text = t;
        }

        /**
         * Draws this {@code Text} to the screen.
         * 
         * @param g a {@code Graphics2D} object from a {@code JPanel} or other
         *          {@code Component}
         **/
        @Override
        public void draw(Graphics2D g) {
            g.drawString(text, x, y);
        }
    }
    
    /**
     * Class that displays a player as a box with information.
     **/
    class PlayerBox extends Rectangle implements Drawable {
        PokerPlayer player;
        
        /**
         * Constructs a new {@code PlayerBox}.
         * 
         * @param p the player to represent
         **/
        PlayerBox(PokerPlayer p) {
            super(0, 24, 80, 100);
            player = p;
        }
        
        /**
         * Draws this {@code PlayerBox} to the screen.
         * 
         * @param g a {@code Graphics2D} object from a {@code JPanel} or other
         *          {@code Component}
         **/
        @Override
        public void draw(Graphics2D g) {
            // Draw outlines indicating whether this player is the active
            // player, the big blind, or the small blind
            if (player == activePlayer || player == bb || player == sb) {
                Stroke soriginal = g.getStroke();
                g.setStroke(new BasicStroke(4.0f));
                g.setColor(player == activePlayer ? Color.orange :
                           player == bb ? Color.red : Color.blue);
                g.draw(this);
                g.setStroke(soriginal);
                g.fillRect(x + 20, y - 8, width - 40, 16);
                g.setColor(Color.white);
                Font foriginal = g.getFont();
                g.setFont(new Font(foriginal.getFontName(), Font.BOLD, 12));
                if (player == activePlayer)
                    g.drawString("Turn", x + 27, y + 4);
                else
                    g.drawString(player == bb ? "BB" : "SB", x + 32, y + 4);
                g.setFont(foriginal);
            }
            else
                g.draw(this);
            
            // Display player's name and money held
            g.setColor(Color.black);
            g.drawString(player.getName(), x + 5, y + 19);
            g.drawString("$" + player.money, x + 5, y + height - 5);
            
            // Draw this player's best hand below if available
            if (player == me && !me.folded && !handName.equals("junk"))
                g.drawString(handName, x + 4, y + height + 80);
        }
    }
    
    /**
     * Class that manages {@code PokerEvent}s and updates the GUI accordingly.
     **/
    private class PokerEventManager implements PokerEventListener {
        @SuppressWarnings("unchecked")
        @Override
        public void processEvent(PokerEvent ev) {
            Class<?> cl = ev.getClass();
            
            // Update the human player
            if (cl == IdentifyMeEvent.class) {
                if (ev.getSource().getClass() == PokerPlayer.class)
                    me = (PokerPlayer) ev.getSource();
            }
            
            // Update the players posting blinds
            else if (cl == RotateBlindsEvent.class) {
                Object[] arr = (Object[]) ev.getSource();
                bb = (PokerPlayer) arr[0];
                sb = (PokerPlayer) arr[1];
            }
            
            // Inform the JPanel to add and draw a pile of chips
            else if (cl == AddChipsEvent.class) {
                Object[] arr = (Object[]) ev.getSource();
                gamePanel.drawAddChips((PokerPlayer) arr[0], (Integer) arr[1]);
            }
            
            // Inform the JPanel to display a player's cards
            else if (cl == DealCardEvent.class) {
                Object[] arr = (Object[]) ev.getSource();
                gamePanel.drawDealCard((PokerPlayer) arr[0], (Card) arr[1]);
            }
            
            // Update the name of the hand
            else if (cl == HandScoreUpdateEvent.class) {
                handName = (String) ev.getSource();
            }
            
            // Update the amount to bet for raising purposes
            else if (cl == BetAmountUpdateEvent.class) {
                betAmount = (Integer) ev.getSource();
            }
            
            // Inform the JPanel to display the collection of posted money into
            // one or more pots
            else if (cl == ManagePotsEvent.class) {
                gamePanel.drawManagePots((List<Pot>) ev.getSource());
            }
            
            // Inform the JPanel to display community cards
            else if (cl == CommunityCardEvent.class) {
                gamePanel.drawCommunityCard((Card) ev.getSource());
            }
            
            // Inform the JPanel to display a player folding cards
            else if (cl == FoldEvent.class) {
                Object[] arr = (Object[]) ev.getSource();
                
                // Whether the human player folded; if so, do not display a
                // best hand for the rest of the round
                if (((Boolean) arr[1]).equals(Boolean.TRUE))
                    handName = "";
                gamePanel.drawFold((List<Card>) arr[0]);
            }
            
            // Inform the JPanel to draw players' cards face up
            else if (cl == ShowHandEvent.class) {
                handName = "";
                gamePanel.drawShowHand((List<PokerPlayer>) ev.getSource());
            }
            
            // Inform the JPanel to draw pots being awarded to players
            else if (cl == AwardPotEvent.class) {
                Object[] arr = (Object[]) ev.getSource();
                gamePanel.drawAwardPot((Pot) arr[0], (List<PokerPlayer>) arr[1], (String) arr[2]);
            }
            
            // Reset the state of the panel and prevent players with no more
            // money from being drawn anymore
            else if (cl == ResetEvent.class) {
                mainTimer.stop();
                handName = "";
                for (int k = gamePanel.drawables.size() - 1; k >= 0; k--) {
                    Drawable d = gamePanel.drawables.get(k);
                    if (!(d instanceof PlayerBox))
                        gamePanel.drawables.remove(k);
                }
                Timer t = new Timer(100, e -> {
                    mainTimer.start();
                });
                t.setRepeats(false);
                t.start();
                
                for (PlayerBox p : players) {
                    if (p.player.money == 0)
                        gamePanel.drawables.remove(p);
                }
            }
            
            // Update the active player
            else if (cl == ActivePlayerEvent.class) {
                Object[] arr = (Object[]) ev.getSource();
                if (arr[1].equals(Boolean.FALSE))
                    activePlayer = null;
                else
                    activePlayer = (PokerPlayer) arr[0];
                propertyUpdate();
            }
        }
    }
}
