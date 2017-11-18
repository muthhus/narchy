package astar.impl;

public interface IOpenSet<F> {

    void add(F node);

    void remove(F node);

    F poll();

    //returns node if present otherwise null
    F getNode(F node);

    int size();

}
