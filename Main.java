import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JButton;  
import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;
import javax.swing.JFileChooser;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> showMenu());
    }

    private static void showMenu() {
        JFrame menu = new JFrame("Sea Creatures - Menu");
        menu.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel p = new JPanel(new FlowLayout());

        JButton newBtn = new JButton("Start New Game");
        JButton loadBtn = new JButton("Load Game");
        JButton helpBtn = new JButton("Help");
        JButton exitBtn = new JButton("Exit");

        newBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String p1 = JOptionPane.showInputDialog(menu, "Player 1 name:", "Player setup", JOptionPane.PLAIN_MESSAGE);
                if (p1 == null || p1.trim().isEmpty()) p1 = "Player1";
                String p2 = JOptionPane.showInputDialog(menu, "Player 2 name:", "Player setup", JOptionPane.PLAIN_MESSAGE);
                if (p2 == null || p2.trim().isEmpty()) p2 = "Player2";
                String t = JOptionPane.showInputDialog(menu, "Turn time limit in seconds (min 5):", "15");
                int timeLimit = 15;
                try { timeLimit = Math.max(5, Integer.parseInt(t)); } catch (Exception ex) { timeLimit = 15; }

                Game game = new Game();
                game.initNewGame(p1, p2, timeLimit);

                GameGui gui = new GameGui(game);
                gui.createAndShow();

                menu.dispose();
            }
        });

        loadBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                if (chooser.showOpenDialog(menu) == JFileChooser.APPROVE_OPTION) {
                    File f = chooser.getSelectedFile();
                    try {
                        Game loaded = Game.loadGame(f.getAbsolutePath());
                        GameGui gui = new GameGui(loaded);
                        gui.createAndShow();
                        menu.dispose();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(menu, "Load failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        helpBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String help = "Sea Creatures - How to play:\n\n"
                    + "Two players take turns picking cells on the opponent's hidden board.\n"
                    + "Reveal creatures, avoid traps, collect powerups. Use 'Mega Guess' to reveal neighbors\n"
                    + "once (if enabled). First player to find all opponent creatures or with highest score wins.\n\n"
                    + "Use Save/Load from the in-game buttons to persist a game.";
                JOptionPane.showMessageDialog(menu, help, "Help", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        exitBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

        p.add(newBtn);
        p.add(loadBtn);
        p.add(helpBtn);
        p.add(exitBtn);

        menu.add(p, BorderLayout.CENTER);
        menu.pack();
        menu.setLocationRelativeTo(null);
        menu.setVisible(true);
    }
}