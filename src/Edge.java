public class Edge {

    Point p1, p2;

    public Edge(Point p1, Point p2) {
        this.p1 = p1;
        this.p2 = p2;
    }

    public double dist() {

        return Math.abs(p1.currPos.distanceTo(p2.currPos));

    }

}