package jcog.tree.rtree;

public interface Nodelike<T> {
    /**
     * when a child adds or removes an item, the amount is propagated up the tree
     */
    void reportSizeDelta(int i);

    boolean contains(T t, Spatialization<T> model);
}
