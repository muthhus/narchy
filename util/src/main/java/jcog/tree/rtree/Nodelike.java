package jcog.tree.rtree;

public interface Nodelike<T> {

    boolean contains(T t, HyperRegion b, Spatialization<T> model);

}
