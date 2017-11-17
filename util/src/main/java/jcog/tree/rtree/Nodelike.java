package jcog.tree.rtree;

public interface Nodelike<T> {

    boolean contains(T t, Spatialization<T> model);
}
