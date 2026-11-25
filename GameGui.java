import javax.swing.*;
import java.awt.*;
import java.io.File;

public class GameGui {
    private Game game;
    private JButton[][] buttons;
    private JFrame frame;
    private JLabel timerLabel;
    private Timer swingTimer;
    private int secondsLeft;
    private JToggleButton megaToggle;
    private JLabel statusLabel;
    private JProgressBar[] healthBars = new JProgressBar[2];
    private JLabel[] scoreLabels = new JLabel[2];

    public GameGui(Game g) { this.game = g; }

    public void createAndShow() {
        frame = new JFrame("Sea Creatures - Find the creatures");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        int rows = game.getRows(), cols = game.getCols();
        JPanel gridPanel = new JPanel(new GridLayout(rows, cols));
        buttons = new JButton[rows][cols];

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                JButton b = new JButton();
                b.setMargin(new Insets(0,0,0,0));
                b.setFont(b.getFont().deriveFont(10f));
                final int rr = r, cc = c;
                b.addActionListener(e -> onCellClicked(rr, cc));
                buttons[r][c] = b;
                gridPanel.add(b);
            }
        }

        frame.add(gridPanel, BorderLayout.CENTER);

        // Right panel: player info and controls
        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        for (int i = 0; i < 2; i++) {
            JPanel p = new JPanel(new GridLayout(3,1));
            p.setBorder(BorderFactory.createTitledBorder("Player " + (i+1) + ": " + game.getPlayers().get(i).getName()));
            healthBars[i] = new JProgressBar(0, 100);
            healthBars[i].setValue(game.getPlayers().get(i).getHealth());
            scoreLabels[i] = new JLabel("Score: " + game.getPlayers().get(i).getScore());
            p.add(healthBars[i]);
            p.add(scoreLabels[i]);
            right.add(p);
        }

        JPanel control = new JPanel(new FlowLayout());
        megaToggle = new JToggleButton("Use Mega Guess");
        control.add(megaToggle);
        timerLabel = new JLabel("Time: " + game.getTurnTimeSeconds());
        control.add(timerLabel);
        JButton saveBtn = new JButton("Save");
        saveBtn.addActionListener(e -> doSave());
        control.add(saveBtn);
        JButton loadBtn = new JButton("Load");
        loadBtn.addActionListener(e -> doLoad());
        control.add(loadBtn);
        right.add(control);

        statusLabel = new JLabel("Game started. " + game.getPlayers().get(game.getCurrentPlayer()).getName() + "'s turn.");
        right.add(statusLabel);

        frame.add(right, BorderLayout.EAST);

        frame.setSize(900,700);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        startTurnTimer();
        refreshGrid();
        refreshPlayerPanels();
    }

    private void startTurnTimer() {
        secondsLeft = game.getTurnTimeSeconds();
        if (swingTimer != null) swingTimer.stop();
        swingTimer = new Timer(1000, ev -> {
            secondsLeft--;
            timerLabel.setText("Time: " + secondsLeft);
            if (secondsLeft <= 0) {
                swingTimer.stop();
                statusLabel.setText("Time up! Turn passed.");
                advanceTurnDueToTimeout();
            }
        });
        swingTimer.start();
    }

    private void resetTurnTimer() {
        secondsLeft = game.getTurnTimeSeconds();
        if (swingTimer != null) swingTimer.restart();
        timerLabel.setText("Time: " + secondsLeft);
    }

    private void stopTurnTimer() {
        if (swingTimer != null && swingTimer.isRunning()) swingTimer.stop();
    }

    private void startTurnTimerIfPresent() {
        if (swingTimer != null) swingTimer.start();
    }

    private void advanceTurnDueToTimeout() {
        int cur = game.getCurrentPlayer();
        game.setCurrentPlayer((cur + 1) % game.getPlayers().size());
        statusLabel.setText(game.getPlayers().get(game.getCurrentPlayer()).getName() + "'s turn (timed out).");
        resetTurnTimer();
        refreshPlayerPanels();
    }

    private void onCellClicked(int r, int c) {
        int cur = game.getCurrentPlayer();
        boolean useMega = megaToggle.isSelected();
        StringBuilder msg = new StringBuilder();

        // pause timer while processing and showing result dialog
        stopTurnTimer();

        Game.ShotType res = game.playerShot(cur, r, c, useMega, msg);

        // show a modal pop-up telling the user what happened
        String title = "Search result";
        int msgType = JOptionPane.INFORMATION_MESSAGE;
        if (res == Game.ShotType.INVALID || res == Game.ShotType.ALREADY) msgType = JOptionPane.WARNING_MESSAGE;
        JOptionPane.showMessageDialog(frame, msg.toString(), title, msgType);

        // update UI for next player / state
        refreshGrid();
        refreshPlayerPanels();
        megaToggle.setSelected(false);

        // reset turn timer for the next player (preserve original behavior)
        resetTurnTimer();

        // check end condition
        if (game.allCreaturesFound() || anyPlayerDead()) {
            if (swingTimer != null) swingTimer.stop();
            String winner = determineWinner();
            JOptionPane.showMessageDialog(frame, "Game over! " + winner, "Game Over", JOptionPane.INFORMATION_MESSAGE);
            setGridEnabled(false);
            return;
        }

        // re-enable grid and resume timer
        setGridEnabled(true);
        startTurnTimerIfPresent();
    }

    private void setGridEnabled(boolean enabled) {
        for (int r = 0; r < buttons.length; r++) {
            for (int c = 0; c < buttons[0].length; c++) {
                // keep already revealed cells disabled
                boolean[][] rev = game.getRevealed();
                if (rev[r][c]) buttons[r][c].setEnabled(false);
                else buttons[r][c].setEnabled(enabled);
            }
        }
    }

    private boolean anyPlayerDead() {
        for (Player p : game.getPlayers()) {
            if (p.getHealth() <= 0) return true;
        }
        return false;
    }

    private String determineWinner() {
        // Immediate loss if a player has 0 HP
        Player a = game.getPlayers().get(0);
        Player b = game.getPlayers().get(1);
        boolean aDead = a.getHealth() <= 0;
        boolean bDead = b.getHealth() <= 0;
        if (aDead && !bDead) return b.getName() + " wins (opponent KO)!";
        if (bDead && !aDead) return a.getName() + " wins (opponent KO)!";
        if (aDead && bDead) return "Both players knocked out — draw.";

        // Otherwise, decide by score
        if (a.getScore() > b.getScore()) return a.getName() + " wins!";
        if (b.getScore() > a.getScore()) return b.getName() + " wins!";
        return "Draw";
    }

    private void refreshGrid() {
        int[][] g = game.getGrid();
        boolean[][] rev = game.getRevealed();
        for (int r=0; r<g.length; r++) for (int c=0; c<g[0].length; c++) {
            JButton b = buttons[r][c];
            if (!rev[r][c]) {
                b.setBackground(Color.BLUE.brighter());
                b.setText("");
                b.setEnabled(true);
            } else {
                b.setEnabled(false);
                if (g[r][c] > 0) { b.setBackground(Color.RED); b.setText("X"); }
                else if (g[r][c] == -1) { b.setBackground(Color.BLACK); b.setText("T"); }
                else if (g[r][c] == -2) { b.setBackground(Color.GREEN); b.setText("+"); }
                else { b.setBackground(Color.CYAN); b.setText("."); }
            }
        }
    }

    private void refreshPlayerPanels() {
        for (int i = 0; i < game.getPlayers().size(); i++) {
            Player p = game.getPlayers().get(i);
            healthBars[i].setValue(p.getHealth());
            scoreLabels[i].setText("Score: " + p.getScore());
        }
        statusLabel.setText(game.getPlayers().get(game.getCurrentPlayer()).getName() + "'s turn.");
    }

    private void doSave() {
        // pause timer while user chooses filename / confirms
        stopTurnTimer();
        JFileChooser chooser = new JFileChooser();
        if (chooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            try {
                game.saveGame(f.getAbsolutePath());
                JOptionPane.showMessageDialog(frame, "Saved to " + f.getAbsolutePath(), "Saved", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Save failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        // resume timer without resetting remaining seconds
        startTurnTimerIfPresent();
    }

    private void doLoad() {
        // pause timer while loading
        stopTurnTimer();
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            try {
                Game loaded = Game.loadGame(f.getAbsolutePath());
                this.game = loaded;
                refreshGridAfterLoad();
                JOptionPane.showMessageDialog(frame, "Loaded " + f.getAbsolutePath(), "Loaded", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Load failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        // resume timer without resetting remaining seconds
        startTurnTimerIfPresent();
    }

    private void refreshGridAfterLoad() {
        refreshGrid();
        for (int i = 0; i < game.getPlayers().size() && i < healthBars.length; i++) {
            healthBars[i].setValue(game.getPlayers().get(i).getHealth());
            scoreLabels[i].setText("Score: " + game.getPlayers().get(i).getScore());
        }
        resetTurnTimer();
    }
}