import java.io.Serializable;
public class Coordinates implements Serializable {
    private static final long serialVersionUID = 1L;
    private int lat;
    private int lon;
    public Coordinates(int lat, int lon) { this.lat = lat; this.lon = lon; }
    public int getLon() { return lon; }
    public int getLat() { return lat; }
}