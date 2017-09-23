package nars.bag;

import jcog.bag.impl.ArrayBag;
import jcog.bag.impl.PLinkArrayBag;
import jcog.learn.gng.NeuralGasNet;
import jcog.learn.gng.impl.Centroid;
import jcog.pri.PLink;
import jcog.pri.op.PriForget;
import jcog.pri.op.PriMerge;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * clusterjunctioning
 * TODO abstract into general purpose "Cluster of Bags" class
 */
public abstract class BagClustering<K,X>  {



    /** how to interpret the bag items as vector space data */
    abstract public static class Dimensionalize<X> {

        /** TODO allow dynamic change */
        public final short clusters;

        public final int dims;

        protected Dimensionalize(short clusters, int dims) {
            this.clusters = clusters;
            this.dims = dims;
        }

        abstract public double[] coord(/*@NotNull*/ X t);

        abstract public double distanceSq(double[] x, double[] y);

    }

    public static class BagCentroid extends Centroid {

        BagCentroid(int id, int dimensions) {
            super(id, dimensions);
        }
    }






    /**
     * priotized vector link
     *      a) reference to an item
     *      b) priority
     *      c) vector coordinates
     *      d) current centroid id(s)
     */
    public final class VLink<X> extends PLink<X> {

        /**
         * feature vector representing the item as learned by clusterer
         */
        @NotNull
        public final double[] coord;

        /**
         * current centroid
         */
        int centroid;

        public VLink(@NotNull X t, float pri, Dimensionalize d) {
            super(t, pri);
            this.coord = d.coord(t);
        }


        @NotNull
        @Override
        public String toString() {
            return get() + "<<" + Arrays.toString(coord) + '|' + id + ">>";
        }

        @Override
        public boolean delete() {
            centroid = -1;
            return super.delete();
        }

    }


    /** TODO make this an abstract class or interface for pluggable Clustering implementations. gasnet is just the first for now */
    public class Clusters extends NeuralGasNet {

        public Clusters(int dimension, int maxNodes) {
            super(dimension, maxNodes);
        }

        @NotNull
        @Override
        public Centroid newNode(int i, int dims) {
            return new Centroid(i, dims);
        }

    }
    
    public final ArrayBag<X, VLink<X>> bag;

    private final Dimensionalize model;

    private final Clusters net;


    protected BagClustering(Dimensionalize<X> model, int initialCap) {

        this.model = model;

        PriMerge merge = PriMerge.max;
        this.bag = new ArrayBag<>(merge, new ConcurrentHashMap<>(initialCap)) {

            @Nullable
            @Override
            public X key(@NotNull VLink<X> x) {
                return x.id;
            }

            @Override
            public void onAdd(@NotNull VLink<X> x) {
                synchronized (net) {
                    learn(x);
                }
            }

            @Override
            public void onRemove(@NotNull VLink<X> value) {
                value.delete();
            }

            @Nullable
            @Override
            public Consumer<VLink<X>> forget(float temperature) {
                return new PriForget<>(temperature) {
                    @Override
                    public void accept(@NotNull VLink<X> b) {
                        super.accept(b);
                        learn(b);
                    }
                };
            }
        };
        bag.setCapacity(initialCap);

        this.net = new Clusters(model.dims, model.clusters) {
            @Override
            public @NotNull Centroid newNode(int i, int dims) {
                return new BagNode(i, dims);
            }
        };
    }


    final AtomicBoolean busy = new AtomicBoolean(false);

    protected boolean update() {

        if (busy.compareAndSet(false, true)) {

            synchronized (net) {
                bag.commit();
                net.compact();
            }

            busy.set(false);
            return true;
        } else {
            return false;
        }

    }

    private void learn(VLink<X> x) {
        x.centroid = net.put(x.coord).id;
    }

    public void clear() {
        synchronized (bag) {
            bag.clear();
            synchronized (net) {
                net.clear();
            }
        }
    }

    public void put(X x, float pri) {
        bag.putAsync(new VLink<X>(x, pri, model)); //TODO defer vectorization until after accepted
    }

    public void remove(X x) {
        bag.remove(x);
    }

    /** returns NaN if either or both of the items are not present */
    public double distance(X x, X y) {
        assert(!x.equals(y));
        @Nullable VLink<X> xx = bag.get(x);
        if (xx!=null && xx.centroid>=0) {
            @Nullable VLink<X> yy = bag.get(y);
            if (yy!=null && yy.centroid>=0) {
                return model.distanceSq(xx.coord, yy.coord);
            }
        }
        return Double.NaN;
    }

    public Stream<VLink<X>> neighbors(X x) {
        @Nullable VLink<X> link = bag.get(x);
        if (link!=null) {
            int centroid = link.centroid;
            if (centroid>=0) {
                Centroid[] nodes = net.node;
                if (centroid < nodes.length) //in case of resize
                    return bag.stream().filter(y -> y.centroid == centroid);
            }
        }
        return Stream.empty();
    }

    private class BagNode extends Centroid {
        public BagNode(int i, int dims) {
            super(i, dims);
        }

        @Override
        public double distanceSq(double[] x) {
            return model.distanceSq(getDataRef(), x);
        }
    }


}
