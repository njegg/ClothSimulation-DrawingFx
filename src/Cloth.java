import mars.geometry.Vector;

import java.util.ArrayList;

public class Cloth {

    public static double dist = 20;

    ArrayList<Point> points = new ArrayList<>();
    ArrayList<Edge>  edges = new ArrayList<>();

    Point[][] pointMatrix;


    public void generatePointsAndEdges(Vector start, Vector end) {

        System.out.println("Start = " + start);
        System.out.println("End =   " + end);

        double dx = start.x() - end.x();
        double dy = start.y() - end.y();

        int w = (int) Math.sqrt(dx*dx);
        int h = (int) Math.sqrt(dy*dy);

        System.out.println(w  + " x " + h);

        pointMatrix = new Point[(int) (h / dist + 1)][(int) (w / dist + 1)];
        double curX = start.x();
        double curY = start.y();

        int ylen = pointMatrix.length;
        int xlen = pointMatrix[0].length;

        System.out.println("Num of points = " + ylen + " x " + xlen);

        for (int i = 0; i < ylen; i++) {
            for (int j = 0; j < xlen; j++) {

                Point p = new Point(Vector.xy(curX, curY));
                pointMatrix[i][j] = p;
                points.add(p);

                curX += dist;

                if (i - 1 >= 0) {
                    // there is a point above current, can connect
                    edges.add(new Edge(p, pointMatrix[i-1][j]));
                }

                if (j - 1 >= 0) {
                    // there is a point on the left, connect
                    edges.add(new Edge(pointMatrix[i][j-1], p));
                }

            }
            curX = start.x();
            curY -= dist;
        }

    }

}