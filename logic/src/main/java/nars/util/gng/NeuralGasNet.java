package nars.util.gng;

import com.google.common.collect.Sets;
import nars.util.data.list.FasterList;
import org.jetbrains.annotations.NotNull;
import org.jgrapht.graph.SimpleGraph;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * from: https://github.com/scadgek/NeuralGas
 * TODO use a graph for incidence structures to avoid some loops
 */
abstract public class NeuralGasNet<N extends Node> extends SimpleGraph<N, Connection<N>> {
    private final int dimension;

    private int iteration;


    private int maxNodes;
    private int lambda;
    private int maxAge;
    private double alpha;
    private double beta;
    private double epsW;
    private double epsN;

    public int getLambda() {
        return lambda;
    }

    /**
     * lifespan of a node
     */
    public void setLambda(int lambda) {
        this.lambda = lambda;
    }

    public void setAlpha(double alpha) {
        this.alpha = alpha;
    }

    public void setEpsN(double epsN) {
        this.epsN = epsN;
    }

    public void setEpsW(double epsW) {
        this.epsW = epsW;
    }

    public void setBeta(double beta) {
        this.beta = beta;
    }

    public void setMaxAge(int maxAge) {
        this.maxAge = maxAge;
    }

    public int getMaxAge() {
        return maxAge;
    }

    public double getAlpha() {
        return alpha;
    }

    public double getBeta() {
        return beta;
    }

    public double getEpsW() {
        return epsW;
    }

    public double getEpsN() {
        return epsN;
    }


    public NeuralGasNet(int dimension, int maxNodes) {

        super((sourceVertex, targetVertex) -> {
            throw new UnsupportedOperationException();
        });

        this.iteration = 0;
        this.dimension = dimension;
        this.maxNodes = maxNodes;

        //default values
        setLambda(20);
        setMaxAge(20);

        setAlpha(0.8);
        setBeta(0.9);

        setEpsW(0.05);
        setEpsN(0.02);


        /** the node should have randomized coordinates */
        for (int i = 0; i < maxNodes; i++) {
            addVertex((N) newNode(i, dimension));
        }


//        pw = new PrintWriter("resources/output.txt");
//
//        for (Node node : nodes)
//        {
//            pw.println(node.getWeights()[0] + " " + node.getWeights()[1] + " " + node.getWeights()[2] + " black");
//        }
//        pw.println("*");
    }

    @NotNull
    abstract public N newNode(int i, int dims);

    public N closest(double... x) {
        //find closest nodes
        double minDist = Double.POSITIVE_INFINITY;
        N closest = null;
        for (N node : vertexSet()) {
            if (node.getDistanceSq(x) < minDist)
                closest = node;
        }

        return closest;
    }

    /**
     * translates all nodes uniformly
     */
    public void addToNodes(double[] x) {
        for (Node n : vertexSet()) {
            n.add(x);
        }
    }

    public N learn(double... x) {

        //find closest nodes
        double minDist = Double.POSITIVE_INFINITY;
        double maxDist = Double.NEGATIVE_INFINITY;
        N closest = null;
        N nextClosestNode = null;
        N furthest = null;
        for (N node : vertexSet()) {
            double dd = node.updateDistanceSq(x);

            if (dd > maxDist) {
                furthest = node;
                maxDist = dd;
            }
            if (dd < minDist) {
                closest = node;
                minDist = dd;
            }

        }

        double minDist2 = Double.POSITIVE_INFINITY;
        for (N node : vertexSet()) {
            if (node == closest) continue;
            double dd = node.getLocalDistanceSq(); //TODO cache this localDist
            if (dd < minDist2) {
                nextClosestNode = node;
                minDist2 = dd;
            }
        }


        if (closest == null || nextClosestNode == null) {
            throw new RuntimeException("closest=" + closest + ", nextClosest=" + nextClosestNode);
            //return lastNode;
        }

        //update local error of the "winner"
        closest.setLocalError(closest.getLocalError() + closest.getLocalDistance());

        //update weights for "winner"
        closest.update(getEpsW(), x);

        //update weights for "winner"'s neighbours
        for (Connection connection : edgesOf(closest)) {
            Node toUpdate = null;
            if (connection.from == closest) {
                toUpdate = connection.to;
            } else if (connection.to == closest) {
                toUpdate = connection.from;
            }

            //if (toUpdate != null) { //should not be null
            toUpdate.update(getEpsN(), x);
            connection.age();
            //}
        }

        //remove connections with age > maxAge
        List<Connection<N>> toRemove = new FasterList(1);
        for (Connection c : edgeSet()) {
            if (c.getAge() > getMaxAge()) {
                toRemove.add(c);
            }
        }
        removeAllEdges(toRemove);

        //set connection between "winners" to age zero
        Connection nc = new Connection(closest, nextClosestNode);
        removeEdge(nc);
        addEdge(nc);


        //if iteration is lambda
        if (iteration != 0 && iteration % getLambda() == 0) {

            int nextID = furthest.id;
            removeVertex(furthest);
            removed(furthest);

            //find node with maximal local error

            N maxErrorNode = null;
            {
                double maxError = Double.NEGATIVE_INFINITY;
                for (N node : vertexSet()) {
                    if (node.getLocalError() > maxError) {
                        maxErrorNode = node;
                        maxError = node.getLocalError();
                    }
                }

                if (maxErrorNode == null) {
                    throw new RuntimeException("maxErrorNode=null");
                }
            }

            N maxErrorNeighbour = null;

            Set<Connection<N>> ee = edgesOf(maxErrorNode);
            if (!ee.isEmpty()) {
                //find max error neighbour of the mentioned node
                double maxError = Double.NEGATIVE_INFINITY;

                for (Connection<N> connection : ee) {

                    N otherNode = connection.to == maxErrorNode ? connection.from : connection.to;

                    if (otherNode.getLocalError() > maxError) {
                        maxErrorNeighbour = otherNode;
                        maxError = otherNode.getLocalError();
                    }

                }
            } else {
                maxErrorNeighbour = randomNode(maxErrorNode);
            }

            if (maxErrorNeighbour == null) {
                throw new RuntimeException("maxErrorNeighbor=null");
                //return null;
            }

            //remove connection between them
            removeEdge(maxErrorNode, maxErrorNeighbour);

            //System.out.println("creating new node " + nextID + " in: " + vertexSet());

            //create node between errorest nodes
            N newNode = newNode(nextID, dimension);
            newNode.set(maxErrorNode, maxErrorNeighbour);
            addVertex(newNode);

            if (maxErrorNode.id == newNode.id) {
                throw new RuntimeException("new node has same id as max error node");
            }

            //create connections between them
            addEdge(new Connection(maxErrorNode, newNode));
            addEdge(new Connection(maxErrorNeighbour, newNode));

            //update errors of the error nodes
            maxErrorNode.setLocalError(maxErrorNode.getLocalError() * getAlpha());
            maxErrorNeighbour.setLocalError(maxErrorNeighbour.getLocalError() * getAlpha());



    }


    //System.out.println(vertexSet().size() + " nodes, " + edgeSet().size() + " edges in neuralgasnet");

    //update errors of the nodes
    for(
    Node node
    :

    vertexSet()

    )

    {
        node.setLocalError(node.getLocalError() * getBeta());

        //System.out.println("  "+ node);
    }

//            //save positions
//            for (Node node : nodes)
//            {
//                pw.println(node.getWeights()[0] + " " + node.getWeights()[1] + " " + node.getWeights()[2] + " black");
//            }
//            pw.println("*");


    //System.out.println("Iteration: " + iteration++);

    //pw.close();

    iteration++;

    return closest;
}

    private N randomNode(N except) {
        Set<N> vv = vertexSet();
        int vs = vv.size();
        if (vs < 2)
            throw new UnsupportedOperationException();


        while (true) { //HACK
            int n = (int) Math.floor(Math.random() * vs);
            Iterator<N> vi = vv.iterator();
            while (vi.hasNext()) {
                N v = vi.next();
                if (n-- == 0) {
                    if (v != except)
                        return v;
                }
            }
        }
    }

    /**
     * called before a node will be removed
     */
    protected void removed(N furthest) {

    }

    private void addEdge(Connection<N> connection) {

        addEdge(connection.from, connection.to, connection);
    }

    public double[] getDimensionRange(final int dimension) {
        final double[] x = new double[]{Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};

        for (Node node : vertexSet()) {
            double v = node.getEntry(dimension);
            if (v < x[0]) x[0] = v;
            if (v > x[1]) x[1] = v;
        }

        return x;
    }

    /**
     * pulls a dimension out of all the nodes, as an array, and sorts it
     */
    public double[] getDimension(int dim) {
        double[] d = new double[vertexSet().size()];
        int i = 0;
        for (Node n : vertexSet()) {
            d[i++] = n.getEntry(dim);
        }
        Arrays.sort(d);
        return d;
    }
}
