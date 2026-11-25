import java.util.Random;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.Serializable;

public class Game implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final int SIZE = 10; // fixed 10x10 grid
    private int rows = SIZE, cols = SIZE;

    // For each player: an int[][] board where creatures are placed (owner boards)
    private List<int[][]> boards = new ArrayList<>();                // boards.get(ownerIndex)[r][c]

    // For each searching player: boolean[][] showing what that player has revealed on opponent boards
    private List<boolean[][]> revealedByPlayer = new ArrayList<>(); // revealedByPlayer.get(playerIndex)[r][c]

    // Creatures tracked per owner board
    private List<Map<Integer, Creature>> creaturesPerPlayer = new ArrayList<>();
    private List<Integer> nextIdPerPlayer = new ArrayList<>();

    private transient Random rand = new Random();

    private List<Player> players = new ArrayList<>();
    private int currentPlayer = 0;
    private int turnTimeSeconds = 15; // set at start

    // config for special cells & runtime options
    private int trapDamage = -20;
    private int powerupHeal = 20;
    private int trapsCount = 6;
    private int powerupsCount = 4;

    private boolean enableMega = true;
    private boolean enableTraps = true;
    private boolean enablePowerups = true;

    // setters for runtime options (call before initNewGame if desired)
    public void setEnableMega(boolean v) { enableMega = v; }
    public void setEnableTraps(boolean v) { enableTraps = v; }
    public void setEnablePowerups(boolean v) { enablePowerups = v; }
    public boolean isEnableMega() { return enableMega; }
    public boolean isEnableTraps() { return enableTraps; }
    public boolean isEnablePowerups() { return enablePowerups; }

    public void initNewGame(String p1Name, String p2Name, int timeLimitSeconds) {
        this.rows = SIZE; this.cols = SIZE;
        // clear old single-grid fields if present
        this.boards.clear();
        this.revealedByPlayer.clear();
        this.creaturesPerPlayer.clear();
        this.nextIdPerPlayer.clear();

        // create players
        this.players.clear();
        this.players.add(new Player(p1Name));
        this.players.add(new Player(p2Name));
        this.currentPlayer = 0;
        this.turnTimeSeconds = Math.max(5, timeLimitSeconds);
        if (rand == null) rand = new Random();

        // for each player create an empty board, their revealed view, and creature map
        for (int i = 0; i < players.size(); i++) {
            int[][] board = new int[rows][cols];
            boolean[][] revealed = new boolean[rows][cols]; // what this player has revealed on opponent
            boards.add(board);
            revealedByPlayer.add(revealed);
            creaturesPerPlayer.add(new HashMap<>());
            nextIdPerPlayer.add(1);
        }

        // place creatures separately on each player's board
        for (int owner = 0; owner < players.size(); owner++) {
            placeAllCreaturesOnBoard(owner);
        }
        // place special cells per board if desired (traps/powerups)
        for (int owner = 0; owner < players.size(); owner++) {
            placeSpecialCellsOnBoard(owner);
        }
    }

    // For saving/loading
    public List<Player> getPlayers() { return players; }
    public int getCurrentPlayer() { return currentPlayer; }
    public void setCurrentPlayer(int i) { currentPlayer = i; }
    public int getTurnTimeSeconds() { return turnTimeSeconds; }
    public int getRows() { return rows; }
    public int getCols() { return cols; }

    // GUI expects: getRevealed = what currentPlayer has revealed, getGrid = opponent's board
    public boolean[][] getRevealed() { return revealedByPlayer.get(currentPlayer); }
    public int[][] getGrid() { return boards.get((currentPlayer + 1) % players.size()); }

    private void placeAllCreaturesOnBoard(int owner) {
        // creatures definitions as offset arrays (row,col)
        // crab: single cell (2 crabs)
        int[][] crab = new int[][] { {0,0} };
        // anemone: 3x3 block (center filled) + extra cells adjacent to left-central and right-central
        int[][] anemone = new int[][] {
            {0,0},{0,1},{0,2},
            {1,-1},{1,0},{1,1},{1,2},{1,3},
            {2,0},{2,1},{2,2}
        };
        // manta: 5-cell shape (as before)
        int[][] manta = new int[][] {
            {0,0},{1,1},{2,2},{1,3},{0,4}
        };
        // eel: straight 3-cell line
        int[][] eel = new int[][] { {0,0},{0,1},{0,2} };

        // place 2 crabs, 1 anemone, 1 manta, 2 eels
        placeMultiple(crab, 2, "Crab", true, owner);     // allow rotation (no-op for single cell)
        placeMultiple(anemone, 1, "Anemone", true, owner);
        placeMultiple(manta, 1, "Manta", true, owner);
        placeMultiple(eel, 2, "Eel", true, owner);
    }

    private void placeMultiple(int[][] shape, int count, String type, boolean allowRot, int owner) {
        for (int i = 0; i < count; i++) {
            boolean ok = tryPlaceShape(shape, type, allowRot, 2000, owner);
            if (!ok) {
                System.out.println("Warning: Couldn't place " + type + " after many attempts.");
            }
        }
    }

    private int[][] rotateOffsets(int[][] offs, int rot) {
        // rot = 0,1,2,3 -> 0,90,180,270 degrees
        int[][] r = new int[offs.length][2];
        for (int i = 0; i < offs.length; i++) {
            int x = offs[i][0], y = offs[i][1];
            int nx = x, ny = y;
            if (rot == 0) { nx = x; ny = y; }
            else if (rot == 1) { nx = -y; ny = x; }
            else if (rot == 2) { nx = -x; ny = -y; }
            else if (rot == 3) { nx = y; ny = -x; }
            r[i][0] = nx; r[i][1] = ny;
        }
        return r;
    }

    private boolean tryPlaceShape(int[][] shape, String type, boolean allowRot, int attempts, int owner) {
        while (attempts-- > 0) {
            int baseR = rand.nextInt(rows);
            int baseC = rand.nextInt(cols);
            int rot = allowRot ? rand.nextInt(4) : 0;
            int[][] offs = rotateOffsets(shape, rot);
            Coordinates[] absolute = new Coordinates[offs.length];
            boolean ok = true;
            for (int k = 0; k < offs.length; k++) {
                int r = baseR + offs[k][0];
                int c = baseC + offs[k][1];
                if (r < 0 || r >= rows || c < 0 || c >= cols || boards.get(owner)[r][c] != 0) { ok = false; break; }
                absolute[k] = new Coordinates(r, c);
            }
            if (ok) {
                int id = nextIdPerPlayer.get(owner);
                nextIdPerPlayer.set(owner, id + 1);
                for (Coordinates cell : absolute) boards.get(owner)[cell.getLat()][cell.getLon()] = id;
                creaturesPerPlayer.get(owner).put(id, new Creature(id, type, absolute));
                return true;
            }
        }
        return false;
    }

    private void placeSpecialCellsOnBoard(int owner) {
        // place traps (-1) and powerups (-2) on empty cells, only if enabled
        if (enableTraps) placeRandomSpecial(-1, trapsCount, owner);
        if (enablePowerups) placeRandomSpecial(-2, powerupsCount, owner);
    }

    private void placeRandomSpecial(int code, int count, int owner) {
        int placed = 0;
        int attempts = rows * cols * 10;
        while (placed < count && attempts-- > 0) {
            int r = rand.nextInt(rows), c = rand.nextInt(cols);
            if (boards.get(owner)[r][c] == 0) { boards.get(owner)[r][c] = code; placed++; }
        }
    }

    // Shot result types
    public static enum ShotType { MISS, HIT, TRAP, POWERUP, ALREADY, INVALID }

    // Player attempts a shot (useMega toggles mega-guess); returns type and message; advances turn
    // Important: playerIndex is the shooter; target is opponent = (playerIndex+1)%players.size()
    public ShotType playerShot(int playerIndex, int r, int c, boolean useMega, StringBuilder msg) {
        if (r < 0 || r >= rows || c < 0 || c >= cols) { msg.append("Out of bounds."); return ShotType.INVALID; }
        if (revealedByPlayer.get(playerIndex)[r][c]) { msg.append("Already revealed."); return ShotType.ALREADY; }

        int opponent = (playerIndex + 1) % players.size();
        Player p = players.get(playerIndex);

        // reveal this cell for the shooter
        revealedByPlayer.get(playerIndex)[r][c] = true;

        // helper to process a revealed cell (owner board = opponent)
        final java.util.function.BiConsumer<Integer,Integer> processCell = (rr, cc) -> {
            int val = boards.get(opponent)[rr][cc];
            if (val == -1 && enableTraps) {
                p.changeHealth(trapDamage);
                msg.append("Boom at ("+rr+","+cc+"): trap -"+Math.abs(trapDamage)+" HP. ");
                // consume trap
                boards.get(opponent)[rr][cc] = 0;
            } else if (val == -2 && enablePowerups) {
                p.changeHealth(powerupHeal);
                msg.append("Powerup at ("+rr+","+cc+"): +"+powerupHeal+" HP. ");
                boards.get(opponent)[rr][cc] = 0;
            } else if (val > 0) {
                // creature cell
                // increment score only if this cell was previously unrevealed
                p.addScore(1);
                Creature cr = creaturesPerPlayer.get(opponent).get(val);
                msg.append("Hit " + cr.getType() + " at ("+rr+","+cc+"). ");
                // check if whole creature now revealed by this shooter
                boolean allFound = true;
                for (Coordinates cell : cr.getCells()) {
                    if (!revealedByPlayer.get(playerIndex)[cell.getLat()][cell.getLon()]) { allFound = false; break; }
                }
                if (allFound) msg.append("Creature #" + val + " completely found! ");
            }
        };

        // process main cell
        processCell.accept(r, c);

        // handle optional mega: reveal orthogonal neighbors AND apply consequences for each
        if (useMega && enableMega) {
            if (p.useMegaGuess()) {
                msg.append("Mega used: revealing neighbors. ");
                int[][] neigh = { {-1,0},{1,0},{0,-1},{0,1} };
                for (int[] d : neigh) {
                    int nr = r + d[0], nc = c + d[1];
                    if (nr < 0 || nr >= rows || nc < 0 || nc >= cols) continue;
                    if (!revealedByPlayer.get(playerIndex)[nr][nc]) {
                        revealedByPlayer.get(playerIndex)[nr][nc] = true;
                        processCell.accept(nr, nc);
                    }
                }
            } else {
                msg.append("No mega guesses left. ");
            }
        } else if (useMega && !enableMega) {
            msg.append("Mega disabled for this game. ");
        }

        // advance turn after processing
        advanceTurn();
        return ShotType.HIT; // note: message contains specifics including misses/hits/traps/powerups
    }

    private void advanceTurn() {
        currentPlayer = (currentPlayer + 1) % players.size();
    }

    public boolean allCreaturesFound() {
        // Game over if any owner's creatures are all revealed by their opponent (two-player assumption)
        if (players.size() < 2) return false;
        for (int owner = 0; owner < players.size(); owner++) {
            int opponent = (owner + 1) % players.size();
            boolean all = true;
            for (Creature cr : creaturesPerPlayer.get(owner).values()) {
                for (Coordinates cell : cr.getCells()) {
                    if (!revealedByPlayer.get(opponent)[cell.getLat()][cell.getLon()]) { all = false; break; }
                }
                if (!all) break;
            }
            if (all) return true;
        }
        return false;
    }

    // Save/load
    public void saveGame(String filename) throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filename))) {
            out.writeObject(this);
        }
    }

    public static Game loadGame(String filename) throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(filename))) {
            Game g = (Game) in.readObject();
            g.rand = new Random();
            return g;
        }
    }

    // When loading and using GUI, ensure rand not null
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (rand == null) rand = new Random();
    }
}