package nars.gui.graph.layout;

import com.gs.collections.impl.map.mutable.primitive.ObjectIntHashMap;
import nars.data.Range;
import nars.gui.graph.GraphLayout;
import nars.gui.graph.GraphWindow;
import nars.gui.graph.GraphWindow.EDraw;
import nars.gui.graph.GraphWindow.VDraw;
import nars.util.data.list.FasterList;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.apache.commons.math3.util.DoubleArray;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

/**
 * Fast organic layout algorithm, adapted from JGraph
 */
public class FastOrganicLayout implements GraphLayout {

    @Range(min = 0, max = 1f)
    public final MutableFloat nodeSpeed = new MutableFloat(0.05);

    /**
     * Specifies if the top left corner of the input cells should be the origin
     * of the layout result. Default is true.
     */
    protected boolean useInputOrigin;

    /**
     * Specifies if all edge points of traversed edge should be removed.
     * Default is true.
     */
    protected boolean resetEdges = true;


    /**
     * The force constant by which the attractive forces are divided and the
     * replusive forces are multiple by the square of. The value equates to the
     * average radius there is of free space around each node. Default is 50.
     */

    @Range(min = 0, max = 300f)
    public final MutableFloat forceConstant = new MutableFloat(1);

    @Range(min = 0.5f, max = 4f)
    public final MutableFloat spacing = new MutableFloat(1f);




    /**
     * Minimal distance limit. Default is 2. Prevents of dividing by zero.
     */
    protected double minDistanceLimit;

    /**
     * The maximum distance between vertex, beyond which their repulsion no
     * longer has an effect
     */
    protected double maxDistanceLimit;

    /**
     * Start value of temperature. Default is 200.
     */
    protected double initialTemp;

    /**
     * Temperature to limit displacement at later stages of layout.
     */
    protected double temperature;

    /**
     * Total number of iterations to run the layout though.
     */
    protected double maxIterations = 1;


    /**
     * An array of locally stored X co-ordinate displacements for the vertex.
     */
    protected double[] dispX;

    /**
     * An array of locally stored Y co-ordinate displacements for the vertex.
     */
    protected double[] dispY;

    /**
     * An array of locally stored co-ordinate positions for the vertex.
     */
    protected double[][] cellLocation;

    /**
     * The approximate radius of each cell, nodes only.
     */
    protected double[] radius;

//    /**
//     * The approximate radius squared of each cell, nodes only.
//     */
//    protected double[] radiusSquared;


    /**
     * Local copy of cell neighbours.
     */
    protected int[][] neighbors;

    /**
     * final normalization step to center all nodes
     */
    private static final boolean center = false;
    //private final FasterList<VDraw> cells = new FasterList<>();


    /**
     * Constructs a new fast organic layout for the specified graph.
     */
    public FastOrganicLayout() {

        setInitialTemp(9f);
        setMinDistanceLimit(1f);
        setMaxDistanceLimit(50f);

    }


    /**
     *
     */
    public boolean isUseInputOrigin() {
        return useInputOrigin;
    }

    /**
     * @param value
     */
    public void setUseInputOrigin(boolean value) {
        useInputOrigin = value;
    }

    /**
     *
     */
    public boolean isResetEdges() {
        return resetEdges;
    }

    /**
     * @param value
     */
    public void setResetEdges(boolean value) {
        resetEdges = value;
    }


    /**
     *
     */
    public double getMaxIterations() {
        return maxIterations;
    }

    /**
     * @param value
     */
    public void setMaxIterations(double value) {
        maxIterations = value;
    }


    /**
     *
     */
    public double getMinDistanceLimit() {
        return minDistanceLimit;
    }

    /**
     * @param value
     */
    public void setMinDistanceLimit(double value) {
        minDistanceLimit = value;
    }

    /**
     * @return the maxDistanceLimit
     */
    public double getMaxDistanceLimit() {
        return maxDistanceLimit;
    }

    /**
     * @param maxDistanceLimit the maxDistanceLimit to set
     */
    public void setMaxDistanceLimit(double maxDistanceLimit) {
        this.maxDistanceLimit = maxDistanceLimit;
    }

    /**
     *
     */
    public double getInitialTemp() {
        return initialTemp;
    }

    /**
     * @param value
     */
    public void setInitialTemp(double value) {
        initialTemp = value;
    }

    /**
     * Reduces the temperature of the layout from an initial setting in a linear
     * fashion to zero.
     */
    protected void reduceTemperature(int iteration) {
        temperature = initialTemp * (1.0 - iteration / maxIterations);
    }



    @Override
    public void update(GraphWindow graph, List<VDraw> vertices, float dt) {


        //? graph.getBoundsForCells(vertexArray, false, false, true) : null;

        int n = vertices.size();


        if ((cellLocation == null) || (cellLocation.length != n)) {
            dispX = new double[n];
            dispY = new double[n];
            cellLocation = new double[n][2];
            //if (neighbors == null || neighbors.length<n)
            neighbors = new int[n][];
            radius = new double[n];
            //radiusSquared = new double[n];
        }

        // Create a map of vertex first. This is required for the array of
        // arrays called neighbours which holds, for each vertex, a list of
        // ints which represents the neighbours cells to that vertex as
        // the indices into vertexArray

        //final double spacing = this.spacing.get();

        int[][] neighbors = this.neighbors;

        double[][] cl = this.cellLocation;

        for (int ii = 0; ii < n; ii++) {
            final int i = ii;

            VDraw V = vertices.get(i);

            //TODO is this necessary?
            /*if (!graph.containsVertex(vd.getVertex()))
                continue;*/
            
            /*if (vd == null) {
                vd = new VDraw(vertex);
                displayed.put(vertex, vd);
            }*/

            // Set up the mapping from array indices to cells

            //mxRectangle bounds = getVertexBounds(vertex);

            // Set the X,Y value of the internal version of the cell to
            // the center point of the vertex for better positioning

            double vr = radius[i] = V.radius(); /*getRadius()*/
            double width = vr * 2f; //bounds.getWidth();
            double height = width; //vr * 2f; //bounds.getHeight();


            // Randomize (0, 0) locations
            //TODO re-use existing location

            float[] p = V.p;
            final double x = p[0];
            final double y = p[1];

            double[] cli = cl[i];
            cli[0] = x + width / 2f;
            cli[1] = y + height / 2f;

            // Moves cell location back to top-left from center locations used in
            // algorithm, resetting the edge points is part of the transaction


            dispX[i] = 0;
            dispY[i] = 0;
            // Get lists of neighbours to all vertex, translate the cells
            // obtained in indices into vertexArray and store as an array
            // against the original cell index
            //VDraw VDraw = vertexArray.get(i).getVertex();
            //ProcessingGraphCanvas.VDraw vd = displayed.get(v);


            //TODO why does a vertex disappear from the graph... make this unnecessary


            //Set<E> edges = vd.getEdges();
            EDraw[] edges = V.edges;
            int ne = V.edgeCount();


            int[] ni = neighbors[i];
            if (ni == null || ni.length!=ne) {
                neighbors[i] = ni = new int[ne];
            }

            for (int j = 0; j < ne; j++) {

                ni[j] = edges[j].key.order;


                // Check the connected cell in part of the vertex list to be
                // acted on by this layout
                // Else if index of the other cell doesn't correspond to
                // any cell listed to be acted upon in this layout. Set
                // the index to the value of this vertex (a dummy self-loop)
                // so the attraction force of the edge is not calculated

            }
        }

        temperature = initialTemp;

        // Main iteration loop

        for (int iteration = 0; iteration < maxIterations; iteration++) {
            calcRepulsion(); // Calculate repulsive forces on all vertex
            calcAttraction(); // Calculate attractive forces through edge
            calcPositions();
            reduceTemperature(iteration);
        }

        double minx = 0, miny = 0, maxx = 0, maxy = 0;

        float speed = nodeSpeed.floatValue();

        double[] radius = this.radius;

        for (int i = 0; i < n; i++) {
            VDraw vd = vertices.get(i);
            double[] ci = cl[i];

            //cellLocation[i][0] -= 1/2.0; //geo.getWidth() / 2.0;
            //cellLocation[i][1] -= 1/2.0; //geo.getHeight() / 2.0;


            float r = (float) radius[i];

            double x = /*graph.snap*/(ci[0] - r);
            double y = /*graph.snap*/(ci[1] - r);


            vd.move((float) x, (float) y, 0, speed);

            if (i == 0) {
                minx = maxx = x;
                miny = maxy = y;
            } else {
                if (x < minx) minx = x;
                if (y < miny) miny = y;
                if (x > maxx) maxx = x;
                if (y > maxy) maxy = y;
            }
        }

        if (center) {
            // Modifies the cloned geometries in-place. Not needed
            // to clone the geometries again as we're in the same
            // undoable change.
            double dx = -(maxx + minx) / 2f;
            double dy = -(maxy + miny) / 2f;

            Rectangle initialBounds = null; //new mxRectangle(-100, -50, 100, 50);
            if (initialBounds != null) {
                dx += initialBounds.getX();
                dy += initialBounds.getY();
            }

            for (int i = 0; i < n; i++) {
                vertices.get(i).moveDelta((float) dx, (float) dy, 0);
            }
        }
    }

    /**
     * Takes the displacements calculated for each cell and applies them to the
     * local cache of cell positions. Limits the displacement to the current
     * temperature.
     */
    protected void calcPositions() {
        int n = dispX.length;
        for (int index = 0; index < n; index++) {
            // Get the distance of displacement for this node for this
            // iteration
            double xind = dispX[index];
            double yind = dispY[index];
            // reset displacements
            dispX[index] = 0;
            dispY[index] = 0;

            double deltaLength = Math.sqrt(xind * xind + yind * yind);

            if (deltaLength < 0.001) {
                //deltaLength = 0.001;
                continue;
            }

            // Scale down by the current temperature if less than the
            // displacement distance
            double dtemp = Math.min(deltaLength, temperature) / deltaLength;
            double newXDisp = xind * dtemp;
            double newYDisp = yind * dtemp;


            // Update the cached cell locations
            double[] ci = cellLocation[index];
            ci[0] += newXDisp;
            ci[1] += newYDisp;

        }
    }

    /**
     * Calculates the attractive forces between all laid out nodes linked by
     * edge
     */
    protected void calcAttraction() {
        // Check the neighbours of each vertex and calculate the attractive
        // force of the edge connecting them

        final double forceConstant = this.forceConstant.doubleValue();
        //final double spacing = this.spacing.doubleValue();

        double[][] cl = this.cellLocation;
        int n = cl.length;

        //double[] radiusSquared = this.radiusSquared;
        int[][] neighbors = this.neighbors;

        double minDistanceLimitSq = this.minDistanceLimit * this.minDistanceLimit;

        int numNeighbors = neighbors.length;
        for (int i = 0; i < numNeighbors; i++) {

            int[] neighbor = neighbors[i];

            if (neighbor == null || cl[i] == null)
                continue;

            for (int nj = 0; nj < neighbor.length; nj++) {
                // Get the index of the othe cell in the vertex array
                int j = neighbor[nj];


                if (j < 0 || j >= n) {
                    //System.err.println("vert " + i + ": " + Arrays.toString(neighbor) + " wtf " + n);
                    continue;
                }
                if (i == j)
                    continue; // Do not proceed self-loops


                double[] clj = cl[j];
                double[] cli = cl[i];
                double xDelta = cli[0] - clj[0];
                double yDelta = cli[1] - clj[1];

                // The distance between the nodes
                double distance = (xDelta * xDelta + yDelta * yDelta);
                    // - (spacing * (radiusSquared[i] + radiusSquared[j]));

                if (distance < minDistanceLimitSq)
                    continue;

                distance = Math.sqrt(distance);

                double force = 1f / (distance * forceConstant);

                double displacementX = (xDelta) * force;
                double displacementY = (yDelta) * force;

                this.dispX[i] -= displacementX;
                this.dispY[i] -= displacementY;
                this.dispX[j] += displacementX;
                this.dispY[j] += displacementY;

            }
        }
    }

    /**
     * Calculates the repulsive forces between all laid out nodes
     */
    protected void calcRepulsion() {

        //double[] radius = this.radius;
        double[] dispX = this.dispX;
        double[] dispY = this.dispY;

        double[][] cl = this.cellLocation;
        final int vertexCount = cl.length;

        double minDistanceLimitSq = minDistanceLimit*minDistanceLimit;
        double maxDistanceLimitSq = maxDistanceLimit*maxDistanceLimit;


        final double forceConstantSq = this.forceConstant.doubleValue()*this.forceConstant.doubleValue();

        for (int i = 0; i < vertexCount; i++) {

            double[] ci = cl[i];

            for (int j = i; j < vertexCount; j++) {

                double[] cj = cl[j];

                if ((ci != null) && (cj != null)) {
                    double xDelta = ci[0] - cj[0];
                    double yDelta = ci[1] - cj[1];

                    if (xDelta == 0) {
                        xDelta = 0.01 + Math.random();
                    }

                    if (yDelta == 0) {
                        yDelta = 0.01 + Math.random();
                    }

                    // Distance between nodes
                    double deltaLength = ((xDelta * xDelta) + (yDelta * yDelta));


//                    double deltaLengthWithRadius = deltaLength - radius[i]
//                            - radius[j];

                    if (deltaLength> maxDistanceLimitSq) {
                        // Ignore vertex too far apart
                        continue;
                    }

                    if (deltaLength < minDistanceLimitSq) {
                        deltaLength = minDistanceLimit;
                    } else {
                        deltaLength = Math.sqrt(deltaLength);
                    }

                    double force = forceConstantSq / deltaLength;

                    double displacementX = (xDelta /*/ deltaLength*/) * force;
                    double displacementY = (yDelta /*/ deltaLength*/) * force;

                    dispX[i] += displacementX;
                    dispY[i] += displacementY;
                    dispX[j] -= displacementX;
                    dispY[j] -= displacementY;
                }
            }
        }
    }


}