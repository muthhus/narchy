/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nars.concept.util;

import org.apache.commons.math3.exception.ConvergenceException;
import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.apache.commons.math3.exception.NumberIsTooSmallException;
import org.apache.commons.math3.exception.util.LocalizedFormats;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.ml.distance.DistanceMeasure;
import org.apache.commons.math3.ml.distance.EuclideanDistance;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.stat.descriptive.moment.Variance;
import org.apache.commons.math3.util.MathUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * MODIFIED
 * Clustering algorithm based on David Arthur and Sergei Vassilvitski k-means++ algorithm.
 * @param <T> type of the points to cluster
 * @see <a href="http://en.wikipedia.org/wiki/K-means%2B%2B">K-means++ (wikipedia)</a>
 * @since 3.2
 */
abstract public class BeliefClusterer<T>  {

    private final DistanceMeasure measure;

    public DistanceMeasure getDistanceMeasure() {
        return this.measure;
    }

    abstract protected double distance(T p1, double[] p2);

    /** Strategies to use for replacing an empty cluster. */
    public enum EmptyClusterStrategy {

        /** Split the cluster with largest distance variance. */
        LARGEST_VARIANCE,

        /** Split the cluster with largest number of points. */
        LARGEST_POINTS_NUMBER,

        /** Create a cluster around the point farthest from its centroid. */
        FARTHEST_POINT,

        /** Generate an error. */
        ERROR

    }

    /** The number of clusters. */
    private final int k;

    /** The maximum number of iterations. */
    private final int maxIterations;

    /** Random generator for choosing initial centers. */
    private final Random random;

    /** Selected strategy for empty clusters. */
    private final EmptyClusterStrategy emptyStrategy;

    /** Build a clusterer.
     * <p>
     * The default strategy for handling empty clusters that may appear during
     * algorithm iterations is to split the cluster with largest distance variance.
     * <p>
     * The euclidean distance will be used as default distance measure.
     *
     * @param k the number of clusters to split the data into
     */
    public BeliefClusterer(final int k) {
        this(k, -1);
    }

    /** Build a clusterer.
     * <p>
     * The default strategy for handling empty clusters that may appear during
     * algorithm iterations is to split the cluster with largest distance variance.
     * <p>
     * The euclidean distance will be used as default distance measure.
     *
     * @param k the number of clusters to split the data into
     * @param maxIterations the maximum number of iterations to run the algorithm for.
     *   If negative, no maximum will be used.
     */
    public BeliefClusterer(final int k, final int maxIterations) {
        this(k, maxIterations, new EuclideanDistance());
    }

    /** Build a clusterer.
     * <p>
     * The default strategy for handling empty clusters that may appear during
     * algorithm iterations is to split the cluster with largest distance variance.
     *
     * @param k the number of clusters to split the data into
     * @param maxIterations the maximum number of iterations to run the algorithm for.
     *   If negative, no maximum will be used.
     * @param measure the distance measure to use
     */
    public BeliefClusterer(final int k, final int maxIterations, final DistanceMeasure measure) {
        this(k, maxIterations, measure, new JDKRandomGenerator());
    }

    /** Build a clusterer.
     * <p>
     * The default strategy for handling empty clusters that may appear during
     * algorithm iterations is to split the cluster with largest distance variance.
     *
     * @param k the number of clusters to split the data into
     * @param maxIterations the maximum number of iterations to run the algorithm for.
     *   If negative, no maximum will be used.
     * @param measure the distance measure to use
     * @param random random generator to use for choosing initial centers
     */
    public BeliefClusterer(final int k, final int maxIterations,
                           final DistanceMeasure measure,
                           final Random random) {
        this(k, maxIterations, measure, random, EmptyClusterStrategy.LARGEST_VARIANCE);
    }

    /** Build a clusterer.
     *
     * @param k the number of clusters to split the data into
     * @param maxIterations the maximum number of iterations to run the algorithm for.
     *   If negative, no maximum will be used.
     * @param measure the distance measure to use
     * @param random random generator to use for choosing initial centers
     * @param emptyStrategy strategy to use for handling empty clusters that
     * may appear during algorithm iterations
     */
    public BeliefClusterer(final int k, final int maxIterations,
                           final DistanceMeasure measure,
                           final Random random,
                           final EmptyClusterStrategy emptyStrategy) {

        this.measure = measure;
        this.k             = k;
        this.maxIterations = maxIterations;
        this.random        = random;
        this.emptyStrategy = emptyStrategy;
    }

    /**
     * Return the number of clusters this instance will use.
     * @return the number of clusters
     */
    public int getK() {
        return k;
    }

    /**
     * Returns the maximum number of iterations this instance will use.
     * @return the maximum number of iterations, or -1 if no maximum is set
     */
    public int getMaxIterations() {
        return maxIterations;
    }

    /**
     * Returns the random generator this instance will use.
     * @return the random generator
     */
    public Random getRandomGenerator() {
        return random;
    }

    public final class Cluster  {

        final ArrayRealVector pos;
        private final List<T> points = new ArrayList();

        public Cluster(int dim) {
            this.pos = new ArrayRealVector(dim);
        }

        @NotNull
        @Override
        public String toString() {
            return "<" +
                    pos +
                    "={" + points +
                    "}>";
        }

        public Cluster(ArrayRealVector pos) {
            this.pos = pos;
        }

        public Cluster(T firstPoint) {
            this.pos = new ArrayRealVector(p(firstPoint), false);
            addPoint(firstPoint);
        }

        public void addPoint(T point) {
            this.points.add(point);
        }

        @NotNull
        public List<T> getPoints() {
            return this.points;
        }


        public T getWeakest(Comparator<? super T> c) {

            if (this.points.isEmpty()) return null;

            Collections.sort(points, c);

            //TODO this is meaningless until the points are sorted:
            return points.get(points.size()-1);
        }
    }

    /** gets the vector point for t */
    @NotNull
    abstract public ArrayRealVector p(T t);

    /**
     * Returns the {@link EmptyClusterStrategy} used by this instance.
     * @return the {@link EmptyClusterStrategy}
     */
    public EmptyClusterStrategy getEmptyClusterStrategy() {
        return emptyStrategy;
    }

    /**
     * Runs the K-means++ clustering algorithm.
     *
     * @param points the points to cluster
     * @return a list of clusters containing the points
     * @throws MathIllegalArgumentException if the data points are null or the number
     *     of clusters is larger than the number of data points
     * @throws ConvergenceException if an empty cluster is encountered and the
     * {@link #emptyStrategy} is set to {@code ERROR}
     */
    @NotNull
    public List<Cluster> cluster(@NotNull final Collection<T> points)
        throws MathIllegalArgumentException, ConvergenceException {

        // sanity checks
        MathUtils.checkNotNull(points);

        // number of clusters has to be smaller or equal the number of data points
        if (points.size() < k) {
            throw new NumberIsTooSmallException(points.size(), k, false);
        }

        // create the initial clusters
        List<Cluster> clusters = chooseInitialCenters(points);

        // create an array containing the latest assignment of a point to a cluster
        // no need to initialize the array, as it will be filled with the first assignment
        int[] assignments = new int[points.size()];
        assignPointsToClusters(clusters, points, assignments);

        // iterate through updating the centers until we're done
        final int max = (maxIterations < 0) ? Integer.MAX_VALUE : maxIterations;
        for (int count = 0; count < max; count++) {
            boolean emptyCluster = false;
            List<Cluster> newClusters = new ArrayList<Cluster>();
            for (final Cluster cluster : clusters) {
                final ArrayRealVector newCenter;
                if (cluster.getPoints().isEmpty()) {
                    switch (emptyStrategy) {
                        case LARGEST_VARIANCE :
                            newCenter = getPointFromLargestVarianceCluster(clusters);
                            break;
                        case LARGEST_POINTS_NUMBER :
                            newCenter = getPointFromLargestNumberCluster(clusters);
                            break;
                        case FARTHEST_POINT :
                            newCenter = getFarthestPoint(clusters);
                            break;
                        default :
                            throw new ConvergenceException(LocalizedFormats.EMPTY_CLUSTER_IN_K_MEANS);
                    }
                    emptyCluster = true;
                } else {
                    newCenter = centroidOf(cluster.getPoints(), getDimensions());
                }
                newClusters.add(new Cluster(newCenter));
            }
            int changes = assignPointsToClusters(newClusters, points, assignments);
            clusters = newClusters;

            // if there were no more changes in the point-to-cluster assignment
            // and there are no empty clusters left, return the current clusters
            if (changes == 0 && !emptyCluster) {
                return clusters;
            }
        }
        return clusters;
    }

    protected abstract int getDimensions();

    /**
     * Adds the given points to the closest {@link Cluster}.
     *
     * @param clusters the {@link Cluster}s to add the points to
     * @param points the points to add to the given {@link Cluster}s
     * @param assignments points assignments to clusters
     * @return the number of points assigned to different clusters as the iteration before
     */
    private int assignPointsToClusters(@NotNull final List<Cluster> clusters,
                                       @NotNull final Collection<T> points,
                                       final int[] assignments) {
        int assignedDifferently = 0;
        int pointIndex = 0;
        for (final T p : points) {
            int clusterIndex = getNearestCluster(clusters, p);
            if (clusterIndex != assignments[pointIndex]) {
                assignedDifferently++;
            }

            Cluster cluster = clusters.get(clusterIndex);
            cluster.addPoint(p);
            assignments[pointIndex++] = clusterIndex;
        }

        return assignedDifferently;
    }

    /**
     * Use K-means++ to choose the initial centers.
     *
     * @param points the points to choose the initial centers from
     * @return the initial centers
     */
    @NotNull
    private List<Cluster> chooseInitialCenters(@NotNull final Collection<T> points) {

        // Convert to list for indexed access. Make it unmodifiable, since removal of items
        // would screw up the logic of this method.
        final List<T> pointList = new ArrayList<T> (points);

        // The number of points in the list.
        final int numPoints = pointList.size();

        // Set the corresponding element in this array to indicate when
        // elements of pointList are no longer available.
        final boolean[] taken = new boolean[numPoints];

        // The resulting list of initial centers.
        final List<Cluster> resultSet = new ArrayList<>();

        // Choose one center uniformly at random from among the data points.
        final int firstPointIndex = random.nextInt(numPoints);

        final T firstPoint = pointList.get(firstPointIndex);

        resultSet.add(new Cluster(firstPoint));

        // Must mark it as taken
        taken[firstPointIndex] = true;

        // To keep track of the minimum distance squared of elements of
        // pointList to elements of resultSet.
        final double[] minDistSquared = new double[numPoints];

        // Initialize the elements.  Since the only point in resultSet is firstPoint,
        // this is very easy.
        for (int i = 0; i < numPoints; i++) {
            if (i != firstPointIndex) { // That point isn't considered
                double d = distance(firstPoint, p(pointList.get(i)).getDataRef());
                minDistSquared[i] = d*d;
            }
        }

        while (resultSet.size() < k) {

            // Sum up the squared distances for the points in pointList not
            // already taken.
            double distSqSum = 0.0;

            for (int i = 0; i < numPoints; i++) {
                if (!taken[i]) {
                    distSqSum += minDistSquared[i];
                }
            }

            // Add one new data point as a center. Each point x is chosen with
            // probability proportional to D(x)2
            final double r = random.nextDouble() * distSqSum;

            // The index of the next point to be added to the resultSet.
            int nextPointIndex = -1;

            // Sum through the squared min distances again, stopping when
            // sum >= r.
            double sum = 0.0;
            for (int i = 0; i < numPoints; i++) {
                if (!taken[i]) {
                    sum += minDistSquared[i];
                    if (sum >= r) {
                        nextPointIndex = i;
                        break;
                    }
                }
            }

            // If it's not set to >= 0, the point wasn't found in the previous
            // for loop, probably because distances are extremely small.  Just pick
            // the last available point.
            if (nextPointIndex == -1) {
                for (int i = numPoints - 1; i >= 0; i--) {
                    if (!taken[i]) {
                        nextPointIndex = i;
                        break;
                    }
                }
            }

            // We found one.
            if (nextPointIndex >= 0) {

                final T p = pointList.get(nextPointIndex);

                resultSet.add(new Cluster (p));

                // Mark it as taken.
                taken[nextPointIndex] = true;

                if (resultSet.size() < k) {
                    // Now update elements of minDistSquared.  We only have to compute
                    // the distance to the new center to do this.
                    for (int j = 0; j < numPoints; j++) {
                        // Only have to worry about the points still not taken.
                        if (!taken[j]) {
                            double d = distance(p, p(pointList.get(j)).getDataRef());
                            double d2 = d * d;
                            if (d2 < minDistSquared[j]) {
                                minDistSquared[j] = d2;
                            }
                        }
                    }
                }

            } else {
                // None found --
                // Break from the while loop to prevent
                // an infinite loop.
                break;
            }
        }

        return resultSet;
    }

    /**
     * Get a random point from the {@link Cluster} with the largest distance variance.
     *
     * @param clusters the {@link Cluster}s to search
     * @return a random point from the selected cluster
     * @throws ConvergenceException if clusters are all empty
     */
    @NotNull
    private ArrayRealVector getPointFromLargestVarianceCluster(@NotNull final Collection<Cluster> clusters)
            throws ConvergenceException {

        double maxVariance = Double.NEGATIVE_INFINITY;
        Cluster selected = null;
        for (final Cluster cluster : clusters) {
            if (!cluster.getPoints().isEmpty()) {

                // compute the distance variance of the current cluster
                final ArrayRealVector center = cluster.pos;
                final Variance stat = new Variance();
                for (final T point : cluster.getPoints()) {
                    stat.increment(distance(point, center.getDataRef()));
                }
                final double variance = stat.getResult();

                // select the cluster with the largest variance
                if (variance > maxVariance) {
                    maxVariance = variance;
                    selected = cluster;
                }

            }
        }

        // did we find at least one non-empty cluster ?
        if (selected == null) {
            throw new ConvergenceException(LocalizedFormats.EMPTY_CLUSTER_IN_K_MEANS);
        }

        // extract a random point from the cluster
        final List<T> selectedPoints = selected.getPoints();
        return p(selectedPoints.remove(random.nextInt(selectedPoints.size())));

    }

    /**
     * Get a random point from the {@link Cluster} with the largest number of points
     *
     * @param clusters the {@link Cluster}s to search
     * @return a random point from the selected cluster
     * @throws ConvergenceException if clusters are all empty
     */
    @NotNull
    private ArrayRealVector getPointFromLargestNumberCluster(@NotNull final Collection<? extends Cluster> clusters)
            throws ConvergenceException {

        int maxNumber = 0;
        Cluster selected = null;
        for (final Cluster cluster : clusters) {

            // get the number of points of the current cluster
            final int number = cluster.getPoints().size();

            // select the cluster with the largest number of points
            if (number > maxNumber) {
                maxNumber = number;
                selected = cluster;
            }

        }

        // did we find at least one non-empty cluster ?
        if (selected == null) {
            throw new ConvergenceException(LocalizedFormats.EMPTY_CLUSTER_IN_K_MEANS);
        }

        // extract a random point from the cluster
        final List<T> selectedPoints = selected.getPoints();
        return p(selectedPoints.remove(random.nextInt(selectedPoints.size())));

    }

    /**
     * Get the point farthest to its cluster center
     *
     * @param clusters the {@link Cluster}s to search
     * @return point farthest to its cluster center
     * @throws ConvergenceException if clusters are all empty
     */
    @NotNull
    private ArrayRealVector getFarthestPoint(@NotNull final Collection<Cluster> clusters) throws ConvergenceException {

        double maxDistance = Double.NEGATIVE_INFINITY;
        Cluster selectedCluster = null;
        int selectedPoint = -1;
        for (final Cluster cluster : clusters) {

            // get the farthest point
            final ArrayRealVector center = cluster.pos;
            final List<T> points = cluster.getPoints();
            for (int i = 0; i < points.size(); ++i) {
                final double distance = distance(points.get(i), center.getDataRef());
                if (distance > maxDistance) {
                    maxDistance     = distance;
                    selectedCluster = cluster;
                    selectedPoint   = i;
                }
            }

        }

        // did we find at least one non-empty cluster ?
        if (selectedCluster == null) {
            throw new ConvergenceException(LocalizedFormats.EMPTY_CLUSTER_IN_K_MEANS);
        }

        return p(selectedCluster.getPoints().remove(selectedPoint));

    }

    /**
     * Returns the nearest {@link Cluster} to the given point
     *
     * @param clusters the {@link Cluster}s to search
     * @param point the point to find the nearest {@link Cluster} for
     * @return the index of the nearest {@link Cluster} to the given point
     */
    private int getNearestCluster(@NotNull final Collection<Cluster> clusters, final T point) {
        double minDistance = Double.MAX_VALUE;
        int clusterIndex = 0;
        int minCluster = 0;
        for (final Cluster c : clusters) {
            final double distance = distance(point, c.pos.getDataRef());
            if (distance < minDistance) {
                minDistance = distance;
                minCluster = clusterIndex;
            }
            clusterIndex++;
        }
        return minCluster;
    }

    /**
     * Computes the centroid for a set of points.
     *
     * @param points the set of points
     * @param dimension the point dimension
     * @return the computed centroid for the set of points
     */
    @NotNull
    private ArrayRealVector centroidOf(@NotNull final Collection<T> points, final int dimension) {
        final double[] centroid = new double[dimension];

        for (final T p : points) {
            final double[] point = p(p).getDataRef();
            for (int i = 0; i < dimension; i++) {
                centroid[i] += point[i];
            }
        }

        int ps = points.size();
        for (int i = 0; i < dimension; i++) {
            centroid[i] /= ps;
        }
        return new ArrayRealVector(centroid);
    }

}

//            BeliefClusterer<Task> bc = new BeliefClusterer<Task>(tt.size()/3) {
//
//                protected float occ(long occ) {
//                    return ((occ - minT) / (maxT-minT));
//                }
//
//                @Override
//                protected double distance(Task a, double[] b) {
//                    float docc = (float)Math.abs(occ(a.getOccurrenceTime()) - b[0]);
//                    float dfreq = (float)Math.abs(a.getFrequency() - b[1]);
//
//                    //return docc + dfreq; //TODO euclidean?
//                    return Math.sqrt( docc*docc + dfreq*dfreq );
//                }
//
//                @Override
//                public ArrayRealVector p(Task task) {
//                    return new ArrayRealVector(
//                        new double[] { occ(task.getOccurrenceTime()), task.getFrequency() },
//                        false
//                    );
//                }
//
//                @Override
//                protected int getDimensions() {
//                    return 2;
//                }
//            };
//
//
//            final Comparator<? super Task> r = new Comparator<Task>() {
//                @Override
//                public int compare(Task o1, Task o2) {
//                    return Float.compare(
//                            o2.getConfidence()* o2.getOriginality(),
//                            o1.getConfidence()* o1.getOriginality()
//                    );
//                }
//            };
//
//            List<BeliefClusterer<Task>.Cluster> gt = bc.cluster(tt);
//            for (BeliefClusterer<Task>.Cluster cl : gt) {
//                Task w = cl.getWeakest(r);
//                //System.out.println("removed " + w + " remains: " + cl.getPoints());
//                if (w!=null) {
//                    //concept.getBeliefs().remove(w);
//                }
//            }
//            //System.out.println(gt);
