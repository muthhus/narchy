package jcog.tree.rtree;

import jcog.tree.rtree.split.AxialSplitLeaf;
import jcog.tree.rtree.split.LinearSplitLeaf;
import jcog.tree.rtree.split.QuadraticSplitLeaf;

import java.util.function.Function;

public class RTreeModel<T> {

    public final Split<T> split;
    public final Function<T, HyperRect> builder;
    public final short max;       // max entries per node
    public final short min;       // least number of entries per node

    public RTreeModel(@Deprecated final Function<T, HyperRect> builder, DefaultSplits split, final int min, final int max) {
        this(builder, split.get(), min, max);
    }

    public RTreeModel(@Deprecated final Function<T, HyperRect> builder, final Split<T> split, final int min, final int max) {
        this.max = (short) max;
        this.min = (short) min;
        this.builder = builder;
        this.split = split;
    }

    public Node<T> newLeaf() {
        return new Leaf(max);
    }

    public Branch<T> newBranch() {
        return new Branch<>(max);
    }

    public Node<T> split(T t, Leaf<T> leaf) {
        return split.split(t, leaf, this);
    }

    /**
     * Different methods for splitting nodes in an RTree.
     */
    public enum DefaultSplits {
        AXIAL {
            @Override
            public <T> Split<T> get() {
                return new AxialSplitLeaf<>();
            }
        },
        LINEAR {
            @Override
            public <T> Split<T> get() {
                return new LinearSplitLeaf<>();
            }
        },
        QUADRATIC {
            @Override
            public <T> Split<T> get() {
                return new QuadraticSplitLeaf<>();
            }
        };

        abstract public <T> Split<T> get();

    }
}
