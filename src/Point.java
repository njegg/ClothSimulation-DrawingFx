import javafx.scene.paint.Color;
import mars.geometry.Vector;

public class Point {
    Vector currPos, prevPos;
    boolean fixed;
    Color color;

    public Point(Vector pos, boolean fixed) {
        this.currPos = pos;
        this.prevPos = pos;
        this.fixed = fixed;

        color = fixed ? Color.ORANGE : Color.CORNFLOWERBLUE;
    }

    public Point(Vector pos) {
        this(pos, false);
    }


    public void toggleFixed() {
        fixed = !fixed;
    }

    @Override
    public String toString() {
        return String.format("( %f,%f )", currPos.x(), currPos.y());
    }
}

