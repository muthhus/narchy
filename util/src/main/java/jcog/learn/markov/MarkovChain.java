package jcog.learn.markov;

import com.google.common.collect.Streams;
import jcog.list.FasterList;
import jcog.pri.WLink;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

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
    public final Map<List<T>, Chain<T>> nodes;

    /**
     * Stores how long our tuple length is (how many data elements a node has)
     */
    public final int arity;

    public MarkovChain(int n) {
        this(new HashMap(), n);
    }

    public MarkovChain(Map<List<T>, Chain<T>> nodes, int n) {
        this.nodes = nodes;
        this.arity = n;

        if (n <= 0) throw new IllegalArgumentException("Can't have MarkovChain with tuple length <= 0");
    }

    /**
     * Node that marks the beginning of a phrase. All Markov phrases start here.
     */
    public final Chain START = new Chain(List.of());
    /**
     * Node that signals the end of a phrase. This node should have no edges.
     */
    public final Chain END = new Chain(List.of());


    /**
     * Forget everything.
     */
    public void clear() {
        nodes.clear();
        START.clear();
        END.clear();
    }


    public MarkovChain learn(@NotNull Iterable<T> phrase) {
        return learn(Streams.stream(phrase));
    }

    public MarkovChain learn(@NotNull Stream<T> phrase) {
        return learn(phrase, 1f);
    }

    /**
     * Interpret an ArrayList of data as a possible phrase.
     *
     * @param phrase to learn
     */
    public MarkovChain learn(Stream<T> phrase, float strength) {

        // All phrases start at the header.
        final Chain[] current = {START};

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
                current[0] = current[0].learn(getOrAdd(tu), strength);
                tuple[0] = new FasterList<T>(1);
                tuple[0].add(t);
                //tuple[0].add(t);
            }
        });

        Chain c = current[0];
        List<T> t = tuple[0];

        // Add any incomplete tuples if needed.
        if (!t.isEmpty()) {
            c = c.learn(getOrAdd(t), strength);
        }

        // We've reached the end of the phrase, add an edge to the trailer node.
        c.learn(END, strength);
        return this;
    }

    /**
     * Interpret an array of data as a valid phrase.
     *
     * @param phrase to interpret
     */
    public MarkovChain learn(T phrase[]) {
        return learn(Stream.of(phrase));
    }

    public MarkovChain learnAll(T[]... phrases) {
        for (T[] p : phrases)
            learn(p);
        return this;
    }

    public MarkovSampler<T> sample() {
        return sample(ThreadLocalRandom.current());
    }

    public MarkovSampler<T> sample(Random rng) {
        return new MarkovSampler(this, rng);
    }

    /**
     * This method is an alias to find a node if it
     * exists or create it if it doesn't.
     *
     * @param x to find a node for
     * @return the newly created node, or resolved node
     */
    private Chain getOrAdd(List<T> x) {
        if (x.size() > arity) {
            throw new IllegalArgumentException(
                    String.format("Invalid tuple length %d. This structure: %d", x.size(), arity)
            );
        }

        return nodes.computeIfAbsent(x, Chain::new);
    }


    /**
     * This is our Markov phrase node. It contains the data
     * that this node represents as well as a list of edges to
     * possible nodes elsewhere in the graph.
     *
     * @author pkilgo
     */
    public static class Chain<T> {


        /**
         * The data this node represents
         */
        public final List<T> data;


        /**
         * A list of edges to other nodes
         */
        protected final Map<Chain<T>, WLink<Chain<T>>> edges = new LinkedHashMap();
        private final int hash;

        /**
         * Blank constructor for data-less nodes (the header or trailer)
         */
        public Chain() {
            this(Collections.emptyList());
        }

        /**
         * Constructor for node which will contain data.
         *
         * @param d the data this node should represent
         */
        public Chain(List<T> d) {
            this.data = d;
            this.hash = data.hashCode();
        }

        @Override
        public final int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            throw new UnsupportedOperationException();
        }

        /**
         * Get the data from the tuple at given position
         *
         * @param i the index of the data
         * @return data at index
         */
        public T get(int i) {
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
            edges.clear();
        }

        /**
         * Returns this node's tuple's size.
         *
         * @return size of tuple represented by this node
         */
        public int length() {
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
        public Chain<T> learn(final Chain<T> n, float strength) {
            // Iterate through the edges and see if we can find that node.
            WLink<Chain<T>> e = edges.computeIfAbsent(n, nn -> new WLink<>(nn, 0));
            e.priAdd(strength);
            return e.get();
        }

        /**
         * Randomly choose which is the next node to go to, or
         * return null if there are no edges.
         *
         * @return next node, or null if we could not choose a next node
         */
        protected Chain next(Random rng) {
            if (edges.isEmpty()) return null;
            return selectRoulette(rng, edges.values()).get();
        }

//        protected Node nextTerminal(Random rng) {
//            if (edges.isEmpty()) return null;
//
//            List<Edge> candidates = new ArrayList<Edge>();
//            Edge e = edges.get(0);
//            candidates.add(e);
//            //int min = e.node.getTerminalPathLength(mNodeCount);
//
//            for (int i = 1; i < edges.size(); i++) {
//                e = edges.get(i);
//                //int pathLength = e.node.getTerminalPathLength(mNodeCount);
//                //if (pathLength == min) {
//                    candidates.add(e);
//                //} else if (pathLength < min) {
//                  //  candidates.clear();
//                    //candidates.add(e);
//                    //min = pathLength;
//                //}
//            }
//
//            //if (min == Integer.MAX_VALUE) return null;
//
//            Edge choice = selectRoulette(rng, candidates);
////			System.out.printf("Terminal path: %d\n", min);
////			System.out.printf("%s --> %s\n", data.toString(), choice.node.data.toString());
//            return choice.node;
//        }

        static <T> WLink<T> selectRoulette(Random RNG, Collection<WLink<T>> edges) {
            int s = edges.size();
            if (s == 0)
                return null;
            if (s == 1)
                return edges.iterator().next();

            // First things first: count up the entirety of all the weight.
            float totalScore = 0;
            for (WLink e : edges)
                totalScore += e.pri();

            // Choose a random number that is less than or equal to that weight
            float r = RNG.nextFloat() * totalScore;

            // This variable contains how much weight we have "seen" in our loop.
            int current = 0;

            // Iterate through the edges and find out where our generated number landed.
            for (WLink e : edges) {

                // Is it between the weight we've seen and the weight of this node?
                float dw = e.pri();

                if (r >= current && r < current + dw) {
                    return e;
                }

                // Add the weight we've seen
                current += dw;
            }

            //accident here
            return edges.iterator().next();
        }

    }

}
