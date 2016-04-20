package nars.task;

import nars.Global;
import nars.NAR;
import nars.bag.impl.ListTable;
import nars.budget.BudgetMerge;
import nars.concept.table.ArrayBeliefTable;
import nars.concept.table.BeliefTable;
import nars.concept.table.TaskTable;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.truth.TruthFunctions;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

/**
 Revection: revision, projection, rejection
 attempt to lossy compress temporal belief tables when they are full
 see:
    https://en.wikipedia.org/wiki/Closest_pair_of_points_problem
    http://rosettacode.org/wiki/Closest-pair_problem#Java
    http://algs4.cs.princeton.edu/99hull/ClosestPair.java.html
 */
public class Revection {

    /** returns true if the full table has been compacted allowing a free space for the new input task */
    public static boolean revect(@NotNull Task input, ArrayBeliefTable table, @NotNull NAR nar) {

        @NotNull ListTable<Task, Task> temporal = table.temporal;
        List<Task> tl = temporal.list();

        //find a potential pair of beliefs such that they are more similar than the most similar task to the input

        long now = nar.time();

        ClosestPair p = new ClosestPair(tl, now);


        //assert(p.best1 != null);


        /*(if (cc.equals(p.best1)) {
            remove(temporal, p.best1, nar);
        } else if (cc.equals(p.best2)) {
            replaced =
            remove(temporal, p.best2, nar);
        } else */
        /*Task cc = closest(input, tl, window, now);
        if ((p.bestDistance/2f < distance(input, cc, window, now)) )*/ {
            //remove the two closest ones
            //project them to one, add it, and return true (empty space for input to be added by callee)

            Task recombined = combine(p.best1, p.best2, now);
            if (recombined == null) {

                float r1 = BeliefTable.rankEternalByOriginality(p.best1);
                float r2 = BeliefTable.rankEternalByOriginality(p.best2);
                float rin = BeliefTable.rankEternalByOriginality(input);

                if ((rin < r1) && (rin < r2)) {
                    //reject the input, it is worse
                    return false;
                } else {

                    //only remove the "worst" existing task
                    remove(temporal,
                            ///* the weakest */ (p.best1.conf() < p.best2.conf()) ?
                            ///* the oldest */ (p.best1.occurrence() < p.best2.occurrence()) ?
                            (r1 < r2) ?
                                    p.best1 : p.best2, nar);
                }

            } else {

                remove(temporal, p.best1, nar);
                remove(temporal, p.best2, nar);

                nar.process(recombined);
                //temporal.put(recombined, recombined);
            }

            return true;
        }

        //return false;
    }

    private static Task combine(Task a, Task b, long now) {
        //TODO proper iterpolate: truth, time, dt
        long newOcc = (a.occurrence() + b.occurrence())/2;
        long[] newEv = Stamp.zip(a, b);

        float matchFactor = 1f;
        Truth newTruth = TruthFunctions.revision(a, b, newOcc, matchFactor, Global.TRUTH_EPSILON);
        if (newTruth == null) {
            return null;
        } else {
            return new MutableTask(a, b, now, newOcc, newEv, newTruth, BudgetMerge.avgDQBlend).log("Revection Revision");
        }
    }

    public static float distance(@NotNull Task a, @NotNull Task b, long now) {
        float fDist = 1f + Math.abs(a.freq() - b.freq());
        long ao = a.occurrence();
        long bo = b.occurrence();
        float tDist =  1f + Math.abs(ao - bo);
        float ageFactor = /*Math.max*/(Math.abs(now - ao) + Math.abs(now-bo)); //more age factor will cause them to seem closer together than they actually are, like a perspective collapsing to a point at the horizon
        return fDist * tDist / (1f + (float)Math.sqrt(ageFactor)); // * window);
    }
    private static Task closest(@NotNull Task input, @NotNull List<Task> list, long now) {
        float lowest = Float.POSITIVE_INFINITY;
        Task low = null;
        for (Task t : list) {
            float d = distance(input, t, now);
            if (d < lowest) {
                low = t;
                lowest = d;
            }
        }

        return low;
    }

    static void remove(ListTable<Task, Task> temporal, Task t, @NotNull NAR nar) {
        temporal.remove(t);
        TaskTable.removeTask(t, "Revection Remove", nar);
    }


    /**
     *  The <tt>ClosestPair</tt> data type computes a closest pair of points
     *  in a set of <em>N</em> points in the plane and provides accessor methods
     *  for getting the closest pair of points and the distance between them.
     *  The distance between two points is their Euclidean distance.
     *  <p>
     *  This implementation uses a divide-and-conquer algorithm.
     *  It runs in O(<em>N</em> log <em>N</em>) time in the worst case and uses
     *  O(<em>N</em>) extra space.
     *  <p>
     *  See also {@link FarthestPair}.
     *  <p>
     *  For additional documentation, see <a href="http://algs4.cs.princeton.edu/99hull">Section 9.9</a> of
     *  <i>Algorithms, 4th Edition</i> by Robert Sedgewick and Kevin Wayne.
     *
     *  @author Robert Sedgewick
     *  @author Kevin Wayne
     */
    public static class ClosestPair {

        // closest pair of points and their Euclidean distance
        private Task best1, best2;
        private double bestDistance = Double.POSITIVE_INFINITY;

        /**
         * Computes the closest pair of points in the specified array of points.
         *
         * @param  points the array of points
         * @throws NullPointerException if <tt>points</tt> is <tt>null</tt> or if any
         *         entry in <tt>points[]</tt> is <tt>null</tt>
         */
        public ClosestPair(List<Task> points, long now) {
            int N = points.size();
            if (N <= 1) return;

            // sort by x-coordinate (breaking ties by y-coordinate)
            Task[] pointsByX = new Task[N];
            for (int i = 0; i < N; i++)
                pointsByX[i] = points.get(i);
            Arrays.sort(pointsByX, (a, b) -> Long.compare(a.occurrence(), b.occurrence()));

            // check for coincident points
            /*for (int i = 0; i < N-1; i++) {
                if (pointsByX[i].equals(pointsByX[i+1])) {
                    bestDistance = 0.0;
                    best1 = pointsByX[i];
                    best2 = pointsByX[i+1];
                    return;
                }
            }*/

            // sort by y-coordinate (but not yet sorted)
            Task[] pointsByY = new Task[N];
            for (int i = 0; i < N; i++)
                pointsByY[i] = pointsByX[i];

            // auxiliary array
            Task[] aux = new Task[N];

            closest(pointsByX, pointsByY, aux, 0, N-1, now);
        }

        // find closest pair of points in pointsByX[lo..hi]
        // precondition:  pointsByX[lo..hi] and pointsByY[lo..hi] are the same sequence of points
        // precondition:  pointsByX[lo..hi] sorted by x-coordinate
        // postcondition: pointsByY[lo..hi] sorted by y-coordinate
        private double closest(Task[] pointsByX, Task[] pointsByY, Task[] aux, int lo, int hi, long now) {
            if (hi <= lo) return Double.POSITIVE_INFINITY;

            int mid = lo + (hi - lo) / 2;
            Task median = pointsByX[mid];

            // compute closest pair with both endpoints in left subarray or both in right subarray
            double delta1 = closest(pointsByX, pointsByY, aux, lo, mid, now);
            double delta2 = closest(pointsByX, pointsByY, aux, mid+1, hi, now);
            double delta = Math.min(delta1, delta2);

            // merge back so that pointsByY[lo..hi] are sorted by y-coordinate
            merge(pointsByY, aux, lo, mid, hi);

            // aux[0..M-1] = sequence of points closer than delta, sorted by y-coordinate
            int M = 0;
            for (int i = lo; i <= hi; i++) {
                if (Math.abs(pointsByY[i].occurrence() - median.occurrence()) < delta)
                    aux[M++] = pointsByY[i];
            }

            // compare each point to its neighbors with y-coordinate closer than delta
            for (int i = 0; i < M; i++) {
                // a geometric packing argument shows that this loop iterates at most 7 times
                for (int j = i+1; (j < M) && (aux[j].freq() - aux[i].freq() < delta); j++) {
                    double distance = Revection.distance(aux[i], aux[j], now);
                    if (distance < delta) {
                        delta = distance;
                        if (distance < bestDistance) {
                            bestDistance = delta;
                            best1 = aux[i];
                            best2 = aux[j];
                            // StdOut.println("better distance = " + delta + " from " + best1 + " to " + best2);
                        }
                    }
                }
            }
            return delta;
        }

        /**
         * Returns one of the points in the closest pair of points.
         *
         * @return one of the two points in the closest pair of points;
         *         <tt>null</tt> if no such point (because there are fewer than 2 points)
         */
        public Task either() {
            return best1;
        }

        /**
         * Returns the other point in the closest pair of points.
         *
         * @return the other point in the closest pair of points
         *         <tt>null</tt> if no such point (because there are fewer than 2 points)
         */
        public Task other() {
            return best2;
        }

        /**
         * Returns the Eucliden distance between the closest pair of points.
         *
         * @return the Euclidean distance between the closest pair of points
         *         <tt>Double.POSITIVE_INFINITY</tt> if no such pair of points
         *         exist (because there are fewer than 2 points)
         */
        public double distance() {
            return bestDistance;
        }

        // is v < w ?
        private static boolean less(Comparable v, Comparable w) {
            return v.compareTo(w) < 0;
        }

        // stably merge a[lo .. mid] with a[mid+1 ..hi] using aux[lo .. hi]
        // precondition: a[lo .. mid] and a[mid+1 .. hi] are sorted subarrays
        private static void merge(Comparable[] a, Comparable[] aux, int lo, int mid, int hi) {
            // copy to aux[]
            for (int k = lo; k <= hi; k++) {
                aux[k] = a[k];
            }

            // merge back to a[]
            int i = lo, j = mid+1;
            for (int k = lo; k <= hi; k++) {
                if      (i > mid)              a[k] = aux[j++];
                else if (j > hi)               a[k] = aux[i++];
                else if (less(aux[j], aux[i])) a[k] = aux[j++];
                else                           a[k] = aux[i++];
            }
        }



//        /**
//         * Unit tests the <tt>ClosestPair</tt> data type.
//         * Reads in an integer <tt>N</tt> and <tt>N</tt> points (specified by
//         * their <em>x</em>- and <em>y</em>-coordinates) from standard input;
//         * computes a closest pair of points; and prints the pair to standard
//         * output.
//         */
//        public static void main(String[] args) {
//            int N = StdIn.readInt();
//            Task[] points = new Task[N];
//            for (int i = 0; i < N; i++) {
//                double x = StdIn.readDouble();
//                double y = StdIn.readDouble();
//                points[i] = new Task(x, y);
//            }
//            ClosestPair closest = new ClosestPair(points);
//            StdOut.println(closest.distance() + " from " + closest.either() + " to " + closest.other());
//        }

    }


}
