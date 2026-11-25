import java.io.Serializable;

public class Creature implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id;
    private String type;
    private Coordinates[] cells;

    public Creature(int id, String type, Coordinates[] cells) {
        this.id = id;
        this.type = type;
        this.cells = cells;
    }

    public int getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public Coordinates[] getCells() {
        return cells;
    }
}