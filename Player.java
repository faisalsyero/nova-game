import java.io.Serializable;
public class Player implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private int score = 0;
    private int health = 100;
    private int megaGuesses = 1; // one mega per player
    public Player(String name) { this.name = name; }
    public String getName() { return name; }
    public int getScore() { return score; }
    public void addScore(int s) { score += s; }
    public int getHealth() { return health; }
    public void changeHealth(int delta) { health = Math.max(0, Math.min(100, health + delta)); }
    public int getMegaGuesses() { return megaGuesses; }
    public boolean useMegaGuess() { if (megaGuesses > 0) { megaGuesses--; return true; } return false; }
}