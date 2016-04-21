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
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

import static java.lang.Math.abs;
import static java.lang.Math.sqrt;
import static nars.nal.Tense.DTERNAL;

/**
 * Revection: revision, projection, rejection
 * attempt to lossy compress temporal belief tables when they are full
 * see:
 * https://en.wikipedia.org/wiki/Closest_pair_of_points_problem
 * http://rosettacode.org/wiki/Closest-pair_problem#Java
 * http://algs4.cs.princeton.edu/99hull/ClosestPair.java.html
 */
public class Revection {

    /**
     * returns true if the full table has been compacted allowing a free space for the new input task
     */
    public static boolean revect(@NotNull Task input, @NotNull ArrayBeliefTable table, @NotNull NAR nar) {

        @NotNull ListTable<Task, Task> temporal = table.temporal;
        List<Task> tl = temporal.list();
        int n = tl.size();

        if (tl.size() <= 1)
            return false; //may be due to axiomatic belief locking the table to size=cap=1

        //find a potential pair of beliefs such that they are more similar than the most similar task to the input

        long now = nar.time();

        /*ClosestPair p = new ClosestPair(tl, now);
        Task p1 = p.best1;
        Task p2 = p.best2;*/


        //naive O(N*N) search for closest pair
        Task a = null, b = null;

        Task w = null;
        float wr = Float.POSITIVE_INFINITY;

        float bestDist = Float.POSITIVE_INFINITY;
        for (int i = 0; i < n; i++) {

            Task ii = tl.get(i);

            for (int j = i+1; j < n; j++) {

                if (i == j)
                    continue;

                Task jj = tl.get(j);
                if ((i == j) || (Stamp.overlapping(ii, jj))) {
                    continue;
                } else {
                    float d = distance(ii, jj, now, bestDist);
                    if (d < bestDist) {
                        bestDist = d;
                        a = ii;
                        b = jj;
                        continue;
                    }
                }

            }

            if (a == null) {
                //consider ii for being the weakest ranked task to remove
                float r = BeliefTable.rankTemporalByConfidence(ii, now, now, 1f, -1);
                if (r < wr) {
                    wr = r;
                    w = ii;
                }
            }
        }

        if (a!=null) {

            //merge the selected closest pair
            return revect(input, nar, temporal, now, a, b);

        } else if (w!=null) {

            //remove the weakest found
            //TODO compare with input like below?
            remove(temporal, w, nar);

            return true;
        } else {

            return true;
        }

    }

    public static boolean revect(@NotNull Task input, @NotNull NAR nar, @NotNull ListTable<Task, Task> temporal, long now, @NotNull Task a, @NotNull Task b) {


        Task recombined = combine(a, b, now);

        if (recombined == null) {

            float r1 = BeliefTable.rankEternalByOriginality(a);
            float r2 = BeliefTable.rankEternalByOriginality(b);
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
                                a : b, nar);
            }

        } else {

            remove(temporal, a, nar);
            remove(temporal, b, nar);

            nar.process(recombined);
            //temporal.put(recombined, recombined);
        }

        return true;
    }

    //return false;


    private static Task combine(@NotNull Task a, @NotNull Task b, long now) {
        //TODO proper iterpolate: truth, time, dt
        float ac = a.conf();
        float bc = b.conf();
        long newOcc = Math.round((a.occurrence() * ac + b.occurrence() * bc) / (ac + bc));

        float matchFactor = 1f;
        Truth newTruth = TruthFunctions.revision(a, b, newOcc, matchFactor, Global.TRUTH_EPSILON);
        if (newTruth == null) {
            return null;
        } else {
            long[] newEv = Stamp.zip(a, b);
            return new MutableTask(a, b, now, newOcc, newEv, newTruth, BudgetMerge.avgDQBlend).log("Revection Revision");
        }
    }

    public static float distance(@NotNull Task a, @NotNull Task b, long now, float bestSoFar) {

        //float freqWeight = 2f;
        //float confWeight = 1f;
        float truthDist = abs(a.freq() - b.freq());//*freqWeight + Math.abs(a.conf() - b.conf()) * confWeight;



        long ao = a.occurrence();
        long bo = b.occurrence();
        float tDist = abs(ao - bo);

        int adt = a.term().dt();
        int bdt = b.term().dt();
        float dtDist;
        if ((adt!=DTERNAL) && (bdt!=DTERNAL)) {
            dtDist = abs(adt - bdt);
        } else if (adt == DTERNAL && bdt == DTERNAL) {
            dtDist = 0;
        } else {
            dtDist = tDist; //use tDist again
        }

        //more time distance to now factor will cause them to seem closer together than they actually are, like a perspective collapsing to a point at the horizon
        float timeFade = (abs(now - ao) + abs(now - bo));
        return (1f + truthDist) * (1f + dtDist) * (1f + tDist / (float) sqrt(timeFade) );

        //return (fDist*fDist) * tDist / (1f + (float)Math.sqrt(ageFactor)); // * window);
        //return fDist * (float)(Math.sqrt(tDist / (1f + ageFactor))); // * window);
    }

    @Nullable
    private static Task closest(@NotNull Task input, @NotNull List<Task> list, long now) {
        float lowest = Float.POSITIVE_INFINITY;
        Task low = null;
        for (Task t : list) {
            float d = distance(input, t, now, Float.POSITIVE_INFINITY);
            if (d < lowest) {
                low = t;
                lowest = d;
            }
        }

        return low;
    }

    static void remove(@NotNull ListTable<Task, Task> temporal, @NotNull Task t, @NotNull NAR nar) {
        temporal.remove(t);
        TaskTable.removeTask(t, "Revection Remove", nar);
    }


/**
 * The <tt>ClosestPair</tt> data type computes a closest pair of points
 * in a set of <em>N</em> points in the plane and provides accessor methods
 * for getting the closest pair of points and the distance between them.
 * The distance between two points is their Euclidean distance.
 * <p>
 * This implementation uses a divide-and-conquer algorithm.
 * It runs in O(<em>N</em> log <em>N</em>) time in the worst case and uses
 * O(<em>N</em>) extra space.
 * <p>
 * See also {@link FarthestPair}.
 * <p>
 * For additional documentation, see <a href="http://algs4.cs.princeton.edu/99hull">Section 9.9</a> of
 * <i>Algorithms, 4th Edition</i> by Robert Sedgewick and Kevin Wayne.
 *
 * @author Robert Sedgewick
 * @author Kevin Wayne
 */
public static class ClosestPair {

    // closest pair of points and their Euclidean distance
    public Task best1, best2;
    private double bestDistance = Double.POSITIVE_INFINITY;

    /**
     * Computes the closest pair of points in the specified array of points.
     *
     * @param points the array of points
     * @throws NullPointerException if <tt>points</tt> is <tt>null</tt> or if any
     *                              entry in <tt>points[]</tt> is <tt>null</tt>
     */
    public ClosestPair(@NotNull List<Task> points, long now) {
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

        closest(pointsByX, pointsByY, aux, 0, N - 1, now);
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
        double delta2 = closest(pointsByX, pointsByY, aux, mid + 1, hi, now);
        double delta = Math.min(delta1, delta2);

        // merge back so that pointsByY[lo..hi] are sorted by y-coordinate
        merge(pointsByY, aux, lo, mid, hi);

        // aux[0..M-1] = sequence of points closer than delta, sorted by y-coordinate
        int M = 0;
        for (int i = lo; i <= hi; i++) {
            if (abs(pointsByY[i].occurrence() - median.occurrence()) < delta)
                aux[M++] = pointsByY[i];
        }

        // compare each point to its neighbors with y-coordinate closer than delta
        for (int i = 0; i < M; i++) {
            // a geometric packing argument shows that this loop iterates at most 7 times
            for (int j = i + 1; (j < M) && (aux[j].freq() - aux[i].freq() < delta); j++) {
                double distance = Revection.distance(aux[i], aux[j], now, Float.POSITIVE_INFINITY);
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
     * <tt>null</tt> if no such point (because there are fewer than 2 points)
     */
    public Task either() {
        return best1;
    }

    /**
     * Returns the other point in the closest pair of points.
     *
     * @return the other point in the closest pair of points
     * <tt>null</tt> if no such point (because there are fewer than 2 points)
     */
    public Task other() {
        return best2;
    }

    /**
     * Returns the Eucliden distance between the closest pair of points.
     *
     * @return the Euclidean distance between the closest pair of points
     * <tt>Double.POSITIVE_INFINITY</tt> if no such pair of points
     * exist (because there are fewer than 2 points)
     */
    public double distance() {
        return bestDistance;
    }

    // is v < w ?
    private static boolean less(@NotNull Comparable v, @NotNull Comparable w) {
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
        int i = lo, j = mid + 1;
        for (int k = lo; k <= hi; k++) {
            if (i > mid) a[k] = aux[j++];
            else if (j > hi) a[k] = aux[i++];
            else if (less(aux[j], aux[i])) a[k] = aux[j++];
            else a[k] = aux[i++];
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
