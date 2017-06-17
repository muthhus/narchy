package jcog.tree.rtree;

import java.util.function.Function;

public class RTreeModel<T> {
    public final RTree.Split splitType;
    public final Function<T, HyperRect> builder;
    public final short max;       // max entries per node
    public final short min;       // least number of entries per node

    public RTreeModel(@Deprecated final Function<T, HyperRect> builder, final RTree.Split splitType, final int min, final int max) {
        this.max = (short) max;
        this.min = (short) min;
        this.builder = builder;
        this.splitType = splitType;
    }

    public Node<T> newLeaf() {
        return splitType.newLeaf(max);
    }

    public Branch<T> newBranch() {
        return new Branch<>(max);
    }
}
