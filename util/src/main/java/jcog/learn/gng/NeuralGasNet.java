package jcog.learn.gng;

import com.google.common.base.Joiner;
import jcog.learn.gng.impl.DenseIntUndirectedGraph;
import jcog.learn.gng.impl.Node;
import jcog.learn.gng.impl.ShortUndirectedGraph;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * from: https://github.com/scadgek/NeuralGas
 * TODO use a graph for incidence structures to avoid some loops
 */
abstract public class NeuralGasNet<N extends Node>  /*extends SimpleGraph<N, Connection<N>>*/ {


    private final int dimension;


    private final ShortUndirectedGraph e;
    public final Node[] node;

    private int iteration;


    private final int maxNodes;
    private int lambda;
    private int maxAge;
    private double alpha;
    private double beta;
    private double winnerUpdateRate;
    private double winnerNeighborUpdateRate;

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

    public void setWinnerUpdateRate(double rate, double neighborRate) {
        this.winnerUpdateRate = rate;
        this.winnerNeighborUpdateRate = neighborRate;
    }

    public void setBeta(double beta) {
        this.beta = beta;
    }

    public void setMaxEdgeAge(int maxAge) {
        this.maxAge = maxAge;
    }

    public int getMaxAge() {
        return maxAge;
    }

    public double getWinnerUpdateRate() {
        return winnerUpdateRate;
    }


    public NeuralGasNet(int dimension, int maxNodes) {
        super();

        this.e =
                //new SemiDenseShortUndirectedGraph((short) maxNodes);
                new DenseIntUndirectedGraph((short)maxNodes);
        this.node = new Node[maxNodes];
        clear();
     /** nodes should begin with randomized coordinates */
        for (int i = 0; i < maxNodes; i++) {
            node[i] = newNode(i, dimension);
        }

        this.iteration = 0;
        this.dimension = dimension;
        this.maxNodes = maxNodes;

        //default values
        setLambda(maxNodes*2);
        setMaxEdgeAge(maxNodes*2);

        setAlpha(0.8);
        setBeta(0.9);

        setWinnerUpdateRate(0.05, 0.02);





//        pw = new PrintWriter("resources/output.txt");
//
//        for (Node node : nodes)
//        {
//            pw.println(node.getWeights()[0] + " " + node.getWeights()[1] + " " + node.getWeights()[2] + " black");
//        }
//        pw.println("*");
    }

    public void forEachNode(Consumer<N> each) {
        for (Node n : node)
            each.accept((N)n);
    }

    public void clear() {
        e.clear();
        //Arrays.fill(node, null);
    }

    @NotNull
    abstract public N newNode(int i, int dims);

    public N closest(double... x) {
        //find closest nodes
        double minDist = Double.POSITIVE_INFINITY;
        Node closest = null;
        for (Node n : node) {
            if (n.distanceSq(x) < minDist)
                closest = n;
        }

        return (N) closest;
    }

    /**
     * translates all nodes uniformly
     */
    public void translate(double[] x) {
        for (Node n : node) {
            n.add(x);
        }
    }

    public N put(double... x) {

        //find closest nodes
        double minDist = Double.POSITIVE_INFINITY;
        double maxDist = Double.NEGATIVE_INFINITY;
        short closest = -1;
        short nextClosestNode = -1;
        short furthest = -1;

        final int nodes = maxNodes;
        for (short j = 0; j < nodes; j++) {
            Node nj = this.node[j];
            if (nj == null)
                continue;
            double dd = nj.learn(x);

            if (dd > maxDist) {
                furthest = j;
                maxDist = dd;
            }
            if (dd < minDist) {
                closest = j;
                minDist = dd;
            }
        }

        double minDist2 = Double.POSITIVE_INFINITY;
        for (short j = 0; j < nodes; j++) {
            Node n = node[j];
            if (n == null)
                continue;
            if (j == closest) continue;
            double dd = n.localDistanceSq(); //TODO cache this localDist
            if (dd < minDist2) {
                nextClosestNode = j;
                minDist2 = dd;
            }
        }


        if (closest == -1 || nextClosestNode == -1) {
            //throw new RuntimeException("closest=" + closest + ", nextClosest=" + nextClosestNode);
            //return lastNode;
            return null;
        }

        //update local error of the "winner"
        node[closest].updateLocalError(getWinnerUpdateRate(), x);

        //update weights for "winner"'s neighbours
        short sc = closest;
        e.edgesOf(closest, (connection,age) -> {
            node[connection].update(winnerNeighborUpdateRate, x);
        });
        e.addToEdges(sc, +1);


        //remove connections with age > maxAge
        e.removeEdgeIf(c -> c > maxAge);

        //set connection between "winners" to age zero
        e.setEdge(closest, nextClosestNode, 0);


        //if iteration is lambda
        if (iteration != 0 && iteration % getLambda() == 0) {

            e.removeVertex(furthest);
            removed((N) node[furthest]);

            //find node with maximal local error

            short maxErrorID = -1;
            {
                double maxError = Double.NEGATIVE_INFINITY;
                for (int i = 0, nodeLength = node.length; i < nodeLength; i++) {
                    Node n = node[i];
                    if (i == furthest) continue; //skip furthest which was supposed to be removed, and will be replaced below
                    if (n.localError() > maxError) {
                        maxErrorID = (short) i;
                        maxError = n.localError();
                    }
                }

                if (maxErrorID == -1) {
                    throw new RuntimeException("maxErrorID=null");
                }
            }


            short maxErrorNeighborID = -1;
            //if (e has edges for maxErrorID..)
            final double[] maxError = {Double.NEGATIVE_INFINITY};
            short _maxErrorNeighbour[] = new short[]{-1};
            e.edgesOf(maxErrorID, (otherNodeID) -> {

                Node otherNode = node[otherNodeID];

                if (otherNode.localError() > maxError[0]) {
                    _maxErrorNeighbour[0] = otherNodeID;
                    maxError[0] = otherNode.localError();
                }
            });

            if (_maxErrorNeighbour[0] != -1) {
                maxErrorNeighborID = _maxErrorNeighbour[0];
            } else {

                if (maxErrorNeighborID == -1) {
                    do {
                        maxErrorNeighborID = randomNode();
                    } while (maxErrorID == maxErrorNeighborID);
                }
            }

//            if (maxErrorNeighborID == -1) {
//                throw new RuntimeException("maxErrorNeighbor=null");
//                //return null;
//            }

            //remove connection between them
            e.removeEdge(maxErrorID, maxErrorNeighborID);

            //System.out.println("creating new node " + nextID + " in: " + node);

            //create node between errorest nodes
            N newNode = newNode(furthest, dimension);
            Node maxErrorNeighbor = node[maxErrorNeighborID];
            Node maxErrorNode = node[maxErrorID];
            newNode.set(maxErrorNode, maxErrorNeighbor);
            node[furthest] = newNode;

            if (maxErrorID == furthest) {
                throw new RuntimeException("new node has same id as max error node");
            }

            //create connections between them
            e.setEdge(maxErrorID, furthest, 0);
            e.setEdge(maxErrorNeighborID, furthest, 0);

            //update errors of the error nodes
            maxErrorNode.mulLocalError(alpha);
            maxErrorNeighbor.mulLocalError(alpha);
        }


        //System.out.println(node.size() + " nodes, " + edgeSet().size() + " edges in neuralgasnet");

        //update errors of the nodes
        for (Node n : node) {
            n.mulLocalError(beta);
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

        return node(closest);
    }



    public final N node(int i) {
        return (N) node[i];
    }

    private short randomNode() {
        return (short) (Math.random() * node.length);
//        Set<N> vv = node;
//        int vs = vv.size();
//        if (vs < 2)
//            throw new UnsupportedOperationException();
//
//
//        while (true) { //HACK
//            int n = (int) Math.floor(Math.random() * vs);
//            Iterator<N> vi = vv.iterator();
//            while (vi.hasNext()) {
//                N v = vi.next();
//                if (n-- == 0) {
//                    if (v != except)
//                        return v;
//                }
//            }
//        }
    }

    /**
     * called before a node will be removed
     */
    protected void removed(N furthest) {

    }

    public Stream<N> nodeStream() {
        return Stream.of(node).map(n -> (N)n);
    }

    public void compact() {
        e.compact();
    }

    public int size() { return node.length; }

    @Override
    public String toString() {
        return Joiner.on("\n").join(node);
    }

    /** snapshot state of the node status */
    public static class NeuralGasNetState {

        public double[][] range = new double[0][0];

        public double[][] coord = new double[0][0];

        //public double[] size = new double[0];

        final static double epsilon = 0.001;

        public NeuralGasNetState update(NeuralGasNet n) {

            int dims = n.dimension;
            if (this.range.length!= dims) {
                this.range = new double[dims][2];
            }
            int s = n.size();
            if (this.coord.length!= s) {
                this.coord = new double[s][dims];
                //this.size = new double[s];
            }

            double[][] range = this.range;
            double[][] coord = this.coord;

            for (int d = 0; d < dims; d++) {
                range[d][0] = Float.POSITIVE_INFINITY;
                range[d][1] = Float.NEGATIVE_INFINITY;
            }
            Node[] node1 = n.node;
            for (int i = 0, node1Length = node1.length; i < node1Length; i++) {
                Node x = node1[i];

                double[] cc = coord[i];
                double[] dd = x.getDataRef();

                for (int d = 0; d < dims; d++) {
                    double y;
                    cc[d] = y = dd[d];
                    double[] rr = range[d];
                    rr[0] = Math.min(rr[0], y);
                    rr[1] = Math.max(rr[1], y);
                }

            }
            return this;
        }

        public void normalize() {
            double[][] range = this.range;
            double[][] coord = this.coord;

            int dims = range.length;
            double span[] = new double[dims];
            for (int i = 0; i < dims; i++) {
                double[] ri = range[i];
                double dr = ri[1] - ri[0];
                if (dr > epsilon)
                    span[i] = dr;
                //else = 0
            }

            for (double[] x : coord) {
                for (int i = 0; i < dims; i++) {
                    double s = span[i];
                    x[i] = (s == 0) ? 0 : ((x[i] - range[i][0]) / s);
                }
            }
        }

    }
//    private void addEdge(Connection<N> connection) {
//
//        addEdge(connection.from, connection.to, connection);
//    }

//    public double[] getDimensionRange(final int dimension) {
//        final double[] x = new double[]{Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
//
//        for (Node node : node) {
//            double v = node.getEntry(dimension);
//            if (v < x[0]) x[0] = v;
//            if (v > x[1]) x[1] = v;
//        }
//
//        return x;
//    }

//    /**
//     * pulls a dimension out of all the nodes, as an array, and sorts it
//     */
//    public double[] getDimension(int dim) {
//        double[] d = new double[node.size()];
//        int i = 0;
//        for (Node n : node) {
//            d[i++] = n.getEntry(dim);
//        }
//        Arrays.sort(d);
//        return d;
//    }
}

//package nars.rl.gng;
//
//import org.xml.sax.SAXException;
//
//import javax.xml.parsers.ParserConfigurationException;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//
//public class Main {
//
//    private List<Node> nodes;
//    private List<Connection> connections;
//
//    public Main() {
//        nodes = new ArrayList<Node>();
//        connections = new ArrayList<Connection>();
//    }
//
//    public List<Node> getNodes() {
//        return nodes;
//    }
//
//    public void setNodes(List<Node> nodes) {
//        this.nodes = nodes;
//    }
//
//    public List<Connection> getConnections() {
//        return connections;
//    }
//
//    public void setConnections(List<Connection> connections) {
//        this.connections = connections;
//    }
//
//    public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {
//        NeuralGasNet network = new NeuralGasNet(new Config("resources/config.xml"));
//        network.learn(new Dataset());
//
//        System.out.println("Learning finished");
//        for (Node node : network.nodes)
//        {
//            System.out.println(node.getWeights()[0]+ " " + node.getWeights()[1] + " " + node.getWeights()[2]);
//        }
//
////        Main main = new Main();
////
////        Random random = new Random();
////
////        Node node1 = new Node();
////        node1.setWeights(new double[]{random.nextDouble(), random.nextDouble(), random.nextDouble()});
////        node1.setLocalError(0);
////
////        Node node2 = new Node();
////        node2.setWeights(new double[]{random.nextDouble(), random.nextDouble(), random.nextDouble()});
////        node2.setLocalError(0);
////
////        main.getNodes().add(node1);
////        main.getNodes().add(node2);
////
////        double[] x = {random.nextDouble(), random.nextDouble(), random.nextDouble()};
////        Node closestNode = main.getNodes().get(0);
////        Node secondClosestNode = main.getNodes().get(0);
////        double min = Double.MAX_VALUE;
////        for (Node node : main.getNodes()) {
////            if (main.distance(x, node.getWeights()) < min) {
////                secondClosestNode = closestNode;
////                closestNode = node;
////            }
////        }
////
////        closestNode.setLocalError(closestNode.getLocalError() + main.distance(x, closestNode.getWeights()));
////
////
////        System.out.println();
//    }
//
//    public double distance(double[] x, double[] y) {
//        double coord1Diff = Math.pow(x[0] - y[0], 2);
//        double coord2Diff = Math.pow(x[1] - y[1], 2);
//        double coord3Diff = Math.pow(x[2] - y[2], 2);
//        return Math.sqrt(coord1Diff + coord2Diff + coord3Diff);
//    }
//}
