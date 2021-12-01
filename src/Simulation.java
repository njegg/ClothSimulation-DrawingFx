import java.util.ArrayList;

import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import mars.drawingx.application.DrawingApplication;
import mars.drawingx.drawing.Drawing;
import mars.drawingx.drawing.DrawingUtils;
import mars.drawingx.drawing.View;
import mars.drawingx.gadgets.annotations.GadgetBoolean;
import mars.drawingx.gadgets.annotations.GadgetColorPicker;
import mars.drawingx.gadgets.annotations.GadgetDouble;
import mars.drawingx.gadgets.annotations.GadgetInteger;
import mars.drawingx.gadgets.annotations.GadgetVector;
import mars.drawingx.utils.camera.CameraSimple;
import mars.geometry.Vector;
import mars.input.InputEvent;
import mars.input.InputState;
import mars.time.Profiler;


public class Simulation implements Drawing {


    @GadgetBoolean
    boolean drawPoints = true;

    @GadgetBoolean
    boolean drawEdges = true;

    @GadgetBoolean
    boolean drawPolys;

    @GadgetColorPicker
    Color polyColor = Color.CORNFLOWERBLUE;
    
    @GadgetBoolean
    boolean toggleEditMove;

    @GadgetBoolean
    boolean reset = false;

    @GadgetDouble(p = 1, q = 20)
    double G = 10;

    @GadgetBoolean
    boolean gravity = false;

    @GadgetBoolean
    boolean deleteLastPoint = false;

    @GadgetDouble(p = 1, q = 100)
    double MaxEdgeLength = 25;

    @GadgetInteger
    int rigidness = 10; // edge update iterations

    @GadgetBoolean
    boolean continousEdgeConnecting = false;


    double pointRadius = 5;

    ArrayList<Point> points;
    ArrayList<Edge> edges;
    ArrayList<Cloth> clothes;

    Point savedSelection;
    Point clothSelection;

    CameraSimple camera = new CameraSimple();
    Profiler profiler = new Profiler("profiler");
    
    double FPS = 1;
    double deltaTime = 1;


    public Simulation() {
        super();
        points = new ArrayList<>();
        edges = new ArrayList<>();
        clothes = new ArrayList<>();
    }


    @Override
    public void draw(View view) {
    	profiler.enter();
    	
        view.setTransformation(camera.getTransformation());
        view.stateStore();

        view.setFill(Color.CORNFLOWERBLUE);
        view.setStroke(Color.CORNFLOWERBLUE);
        view.setLineWidth(1);

        DrawingUtils.clear(view, Color.gray(0.85));

        if (drawPoints) drawPoints(view);
        if (drawEdges)  drawEdges(view);
        if (drawPolys) updatePolys(view);

        if (gravity) {
            updatePoints();
            updateEdges();
        }
        
        FPS = profiler.getCountsPerSecond();

        // attempt to make fps not impact stuff
        deltaTime = 1d/FPS;

        view.stateRestore();
        
        
        if (reset) {
        	points = new ArrayList<>();
            edges = new ArrayList<>();
            clothes = new ArrayList<>();
            
        	reset = false;
        }
        
        profiler.exit();
    }


    public void drawPoints(View view) {

        if (deleteLastPoint && points.size() > 0) {
            points.remove(points.size() - 1);
            deleteLastPoint = false;
        }

        for (Point p : points) {
            view.setFill(p.fixed ? Color.RED : Color.CORNFLOWERBLUE);
            view.fillCircleCentered(p.currPos, pointRadius);
        };

    }

    
    public void drawEdges(View view) {
        for (Edge e : edges) {
            view.strokeLineSegment(e.p1.currPos, e.p2.currPos);
        }
    }


    private void makePoint(Vector v, boolean fixed) {
        points.add(new Point(v, fixed));
    }


    public void updateEdges() {

        /*   just a reminder how it works
         *
         *   on center between points add half of the max edge length
         *   while preserving the direction
         *
         *   resulting vector is going to be a bit closer to the other one
         *
         *   when repeated, distance between point and center is approaching
         *   half of max length hence the edge is approaching max length
         *
         *   other point goes on the opposite direction for the same amount */

        for (int i = 0; i < rigidness; i++) {

            edges.stream().parallel().filter(e -> e.dist() >= MaxEdgeLength).forEach(e -> {

                Point p1 = e.p1;
                Point p2 = e.p2;

                Vector edgeCenter = p1.currPos.add(p2.currPos).div(2);
                Vector edgeDirection = p1.currPos.sub(p2.currPos).normalized();

                if (!p1.fixed) {
                    p1.currPos = edgeCenter.add(edgeDirection.mul(MaxEdgeLength).div(2));
                }

                if (!p2.fixed) {
                    p2.currPos = edgeCenter.sub(edgeDirection.mul(MaxEdgeLength).div(2));
                }

            });
        }
    }


    public void updatePolys(View view) {

        view.setFill(polyColor);

        clothes.stream().parallel().forEach(c -> {
            Point[][] pm = c.pointMatrix;

            int n = pm.length;
            int m = pm[0].length;

            for (int i = 1; i < n; i++) {
                for (int j = 1; j < m; j++) {

                    view.fillPolygon(
                            pm[i][j].currPos,
                            pm[i-1][j].currPos,
                            pm[i-1][j-1].currPos,
                            pm[i][j-1].currPos
                    );

                }
            };

        });

    }


    public void updatePoints() {

        points.stream().parallel().filter(p -> !p.fixed).forEach(p -> {
            Vector tempPos = p.currPos;
            p.currPos = p.currPos.add(Vector.y(G * deltaTime));
            p.currPos = p.currPos.add(p.currPos.sub(p.prevPos));
            p.prevPos = tempPos;
        });

    }


    @Override
    public void receiveEvent(View view, InputEvent event, InputState state, Vector pointerWorld, Vector pointerViewBase) {

        if (event.isKeyPress(KeyCode.SPACE)) {
                toggleEditMove = !toggleEditMove;
        }
    	
    	if (toggleEditMove) {
    		camera.receiveEvent(view, event, state, pointerWorld, pointerViewBase);
    	} else {
    		
    		if (event.isMouseButtonPress(2)) {
                makePoint(pointerWorld, true);
            }

            if (event.isMouseButtonPress(1)) {
                makePoint(pointerWorld, false);
            }

            if (event.isMouseButtonPress(3)) {

                Point currSelected = null;

                for (Point p : points) {
                    if (p.currPos.distanceTo(pointerWorld) < pointRadius + 3) {
                        currSelected = p;
                        break;
                    }
                }

                if (currSelected != null) {
                    // selected a point under cursor

                    if (savedSelection == null) {
                        // select a point on cursor
                        savedSelection = currSelected;

                    } else {
                        // already selected a point, connect them
                        edges.add(new Edge(savedSelection, currSelected));
                        savedSelection = continousEdgeConnecting ? currSelected : null;

                    }

                } else {
                    // missed, reset the selection
                    savedSelection = null;
                }
            }

            if (event.isKeyPress(KeyCode.C)) {

                // Disgustingly repeated code

                Point currSelected = new Point(pointerWorld);

                if (clothSelection == null) {
                    // select a point on cursor
                    clothSelection = currSelected;
                } else {
                    // already selected a point, make cloth

                    Cloth c = new Cloth();

                    // flipped args
                    c.generatePointsAndEdges(clothSelection.currPos, currSelected.currPos);
                    
                    
                    points.addAll(c.points);
                    edges.addAll(c.edges);
                    clothes.add(c);

                    clothSelection = null;
                }
            }

            if (event.isKeyPress(KeyCode.F)) {

                for (Point p : points) {
                    if (p.currPos.distanceTo(pointerWorld) < pointRadius + 3) {
                        p.toggleFixed();
                    }
                }
            }
    	}
    	
    }

    
    public static void main(String[] args) {
        DrawingApplication.launch(600, 600);
    }

}