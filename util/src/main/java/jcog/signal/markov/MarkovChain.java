package jcog.signal.markov;

import jcog.list.FasterList;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A class for generating a Markov phrase out of some sort of
 * arbitrary comparable data.
 *
 * @param <T> the type of data you would like to generate phrases for (e.g., <code>java.lan
 * @author pkilgo
 */
public class MarkovChain<T> {

    /**
     * HashMap to help us resolve data to the node that contains it
     */
    public final Map<List<T>, Node> nodes;

    /**
     * Node that marks the beginning of a phrase. All Markov phrases start here.
     */
    private final Node header = makeNode();
    /**
     * Node that signals the end of a phrase. This node should have no edges.
     */
    private final Node trailer = makeNode();
    /**
     * Stores how long our tuple length is (how many data elements a node has)
     */
    public final int arity;

    /**
     * Pointer to the current node. Methods next() uses this
     */
    @Deprecated private Node<T> mCurrent;
    /**
     * Index for which data element is next in our tuple
     */
    @Deprecated private int mTupleIndex;

    /**
     * Keeps up with how long our gradual chain is
     */
    @Deprecated private int mElements;

    /**
     * Nodes use this to find the next node
     */
    private final Random rng;

    public MarkovChain(int n) {
        this(n, new Random());
    }

    public MarkovChain(int n, Random rng) {
        this(new HashMap(), n, rng);
    }

    public MarkovChain(Map<List<T>, Node> nodes, int n, Random rng) {
        this.nodes = nodes;
        this.rng = rng;
        if (n <= 0) throw new IllegalArgumentException("Can't have MarkovChain with tuple length <= 0");

        arity = n;
    }


    /**
     * Forget everything.
     */
    public void clear() {
        nodes.clear();
        header.clear();
        trailer.clear();
    }



    /**
     * Get the number of nodes in this graph.
     *
     * @return number of nodes
     */
    public int getNodeCount() {
        return nodes.size();
    }

    /**
     * Re-initialize the chain pointer  and
     * tuple index to start from the top.
     */
    public void reset() {
        mCurrent = null;
        mTupleIndex = 0;
    }

    /**
     * Returns the next element in our gradual chain.
     * Ignores maximum length.
     *
     * @return next data element
     */
    public T next() {
        return next(false, 0);
    }

    /**
     * Returns the next element and loops to the front of chain
     * on termination.
     *
     * @return next element
     */
    public T nextLoop() {
        return next(true, 0);
    }

    public T next(int maxLength) {
        return next(false, maxLength);
    }

    public T next(boolean loop) {
        return next(loop, 0);
    }

    /**
     * Get next element pointed by our single-element.
     * This will also update the data structure to get ready
     * to serve the next data element.
     *
     * @param loop if you would like to loop
     * @return data element at the current node tuple index
     */
    public T next(boolean loop, int maxLength) {

        if (nodes.isEmpty())
            return null;

        // In case mCurrent hasn't been initialized yet.
        if (mCurrent == null || mCurrent == header) mCurrent = header.next(rng);

        // Handle behavior in case we're at the trailer at the start.
        if (mCurrent == trailer) {
            if (loop == true) {

                if (maxLength > 0 && mElements >= maxLength) mCurrent = header.nextTerminal(rng);
                else mCurrent = header.next(rng);

                mTupleIndex = 0;
            }
            // No more data for non-loopers
            else {
                return null;
            }
        }

        T returnValue = mCurrent.getData(mTupleIndex);

        mTupleIndex++;
        mElements++;

        // We've reached the end of this tuple.
        if (mTupleIndex >= mCurrent.size()) {

            if (maxLength > 0 && mElements >= maxLength) mCurrent = mCurrent.nextTerminal(rng);
            else mCurrent = mCurrent.next(rng);

            mTupleIndex = 0;
        }

        return returnValue;
    }

    public void learn(@NotNull Iterable<T> phrase) {
        learn(StreamSupport.stream(phrase.spliterator(), false));
    }

    public void learn(@NotNull Stream<T> phrase) {
        learn(phrase, 1f);
    }

    /**
     * Interpret an ArrayList of data as a possible phrase.
     *
     * @param phrase to learn
     */
    public void learn(@NotNull Stream<T> phrase, float strength) {

        // All phrases start at the header.
        final Node[] current = {header};

        // Make temporary lists to help us resolve nodes.
        final List<T>[] tuple = new List[]{new FasterList<T>()};

        // Find or create each node, add to its weight for the current node
        // and iterate to the next node.
        phrase.forEach((T t) -> {
            List<T> tu = tuple[0];

            int sz = tu.size();
            if (sz < arity) {
                tu.add(t);
            } else {
                Node n = findOrCreate(tu);
                current[0] = current[0].learn(n, strength);
                tuple[0] = new FasterList<T>(t);
                //tuple[0].add(t);
            }
        });

        Node c = current[0];
        List<T> t = tuple[0];

        // Add any incomplete tuples if needed.
        if (!t.isEmpty()) {
            Node n = findOrCreate(t);
            c = c.learn(n, strength);
        }

        // We've reached the end of the phrase, add an edge to the trailer node.
        c.learn(trailer, strength);
    }

    /**
     * Interpret an array of data as a valid phrase.
     *
     * @param phrase to interpret
     */
    public void learn(T phrase[]) {
        learn(Stream.of(phrase));
    }

    public List<T> generate() {
        return generate(-1);
    }

    /**
     * Use our graph to randomly generate a possibly valid phrase
     * from our data structure.
     *
     * @param max sequence length, or -1 for unlimited
     * @return generated phrase
     */
    public List<T> generate(int maxLen) {
        // Go ahead and choose our first node
        Node<T> current = header.next(rng);

        // We will put our generated phrase in here.
        List<T> phrase = new FasterList<T>();

        // As a safety, check for nulls
        // Iterate til we get to the trailer
        int s = 0;
        while (current != null && current != trailer) {
            // Iterate over the data tuple in the node and add stuff to the phrase
            // if it is non-null
            List<T> cd = current.data;

            if (maxLen!=-1 && (s + cd.size() >= maxLen)) {

                //only get the prefix up to a certain length to avoid
                for (int i = 0; i < maxLen - s; i++) {
                    phrase.add(cd.get(i));
                    s++;
                }
                break;

            } else {
                phrase.addAll(cd);
                s += cd.size();
            }

            if (maxLen!=-1 && s == maxLen)
                break;

            current = current.next(rng);
        }

        // Out pops pure genius
        return phrase;
    }

    /**
     * This method is an alias to find a node if it
     * exists or create it if it doesn't.
     *
     * @param data to find a node for
     * @return the newly created node, or resolved node
     */
    private Node findOrCreate(List<T> data) {
        if (data.size() > arity) {
            throw new IllegalArgumentException(
                    String.format("Invalid tuple length %d. This structure: %d", data.size(), arity)
            );
        }

        return nodes.computeIfAbsent(data, this::makeNode);
    }

    private Node makeNode() {
        return makeNode(null);
    }

    private Node makeNode(List<T> data) {
        Node n = new Node(data);
        return n;
    }


    /**
     * This is our Markov phrase node. It contains the data
     * that this node represents as well as a list of edges to
     * possible nodes elsewhere in the graph.
     *
     * @author pkilgo
     */
    public static class Node<T> {
        /**
         * The data this node represents
         */
        public final List<T> data;


        /**
         * A list of edges to other nodes
         */
        protected final List<Edge> edges = new FasterList<Edge>();

        /**
         * Blank constructor for data-less nodes (the header or trailer)
         */
        public Node() {
            this(Collections.emptyList());
        }

        /**
         * Constructor for node which will contain data.
         *
         * @param d the data this node should represent
         */
        public Node(List<T> d) {
            this.data = d;
        }

        /**
         * Get the data from the tuple at given position
         *
         * @param i the index of the data
         * @return data at index
         */
        public T getData(int i) {
            return data.get(i);
        }

//        public int getTerminalPathLength(int mNodeCount) {
//            boolean visits[] = new boolean[mNodeCount];
//            return doGetTerminalPathLength(visits);
//        }
//
//        private int doGetTerminalPathLength(boolean visits[]) {
//            // The path length is 0 if this is a terminal node.
//            if (isTerminal()) return 0;
//
//            // We have visited the node we are currently in
//            visits[id] = true;
//
//            // Make this variable exist outside the scope of following loop
//            Edge e = null;
//            int i = 0;
//
//            // First let's iterate to find the first node we haven't visited
//            for (i = 0; i < mEdges.size(); i++) {
//                e = mEdges.get(i);
//                if (visits[e.node.id] == false) break;
//            }
//
//            // If we never found one, this path does not terminate
//            if (visits[e.node.id] == true) {
//                visits[id] = false;
//                return Integer.MAX_VALUE;
//            }
//
//            // Set the terminal path length of this first node as the minimum
//            int min = e.node.doGetTerminalPathLength(visits);
//
//            for (i++; i < mEdges.size(); i++) {
//                e = mEdges.get(i);
//
//                // Skip this guy if we have already visited
//                if (visits[e.node.id] == true) continue;
//
//                // Decide which is smaller
//                int pathLength = e.node.doGetTerminalPathLength(visits);
//                min = Math.min(min, pathLength);
//            }
//
//            // Set this guy to unvisited and return the path length
//            visits[id] = false;
//            return (min == Integer.MAX_VALUE) ? min : min + 1;
//        }

        public boolean isTerminal() {
            return data.isEmpty();
        }

        public void clear() {
            data.clear();
            edges.clear();
        }

        /**
         * Returns this node's tuple's size.
         *
         * @return size of tuple represented by this node
         */
        public int size() {
            return data.size();
        }

        /**
         * Add more weight to the given node
         * or create an edge to that node if we didn't
         * already have one.
         *
         * @param n node to add more weight to
         * @return the node that was learned
         */
        public Node<T> learn(Node<T> n, float strength) {
            // Iterate through the edges and see if we can find that node.
            for (Edge<T> e : edges) {
                if (e.node.equals(n)) {
                    e.weight+=strength;
                    return e.node;
                }
            }

            // Elsewise, create an edge.
            edges.add(new Edge<>(n));
            return n;
        }

        /**
         * Randomly choose which is the next node to go to, or
         * return null if there are no edges.
         *
         * @return next node, or null if we could not choose a next node
         */
        protected Node next(Random rng) {
            if (edges.isEmpty()) return null;
            Edge choice = chooseEdge(rng, edges);
            return choice.node;
        }

        protected Node nextTerminal(Random rng) {
            if (edges.isEmpty()) return null;

            List<Edge> candidates = new ArrayList<Edge>();
            Edge e = edges.get(0);
            candidates.add(e);
            //int min = e.node.getTerminalPathLength(mNodeCount);

            for (int i = 1; i < edges.size(); i++) {
                e = edges.get(i);
                //int pathLength = e.node.getTerminalPathLength(mNodeCount);
                //if (pathLength == min) {
                    candidates.add(e);
                //} else if (pathLength < min) {
                  //  candidates.clear();
                    //candidates.add(e);
                    //min = pathLength;
                //}
            }

            //if (min == Integer.MAX_VALUE) return null;

            Edge choice = chooseEdge(rng, candidates);
//			System.out.printf("Terminal path: %d\n", min);
//			System.out.printf("%s --> %s\n", data.toString(), choice.node.data.toString());
            return choice.node;
        }

        private Edge chooseEdge(Random RNG, List<Edge> edges) {
            if (edges.size() == 1)
                return edges.get(0);

            // First things first: count up the entirety of all the weight.
            float totalScore = 0;
            for (int i = 0; i < edges.size(); i++) totalScore += edges.get(i).weight;

            // Choose a random number that is less than or equal to that weight
            float r = RNG.nextFloat()*totalScore;

            // This variable contains how much weight we have "seen" in our loop.
            int current = 0;

            Edge e = null;

            // Iterate through the edges and find out where our generated number landed.
            for (int i = 0; i < edges.size()-1; i++) {
                e = edges.get(i);

                // Is it between the weight we've seen and the weight of this node?
                if (r >= current && r < current + e.weight) {
                    break;
                }

                // Add the weight we've seen
                current += e.weight;
            }

            return e;
        }

    }

    /**
     * Simple container class that holds the node this edge represents and the weight
     * of the edge.
     *
     * @author pkilgo
     */
    protected static class Edge<T> {
        public final Node<T> node;
        float weight = 1;

        public Edge(Node<T> n) {
            node = n;
            weight = 1;
        }
    }
}
