package astar;

/**
 * Interface of a search node.
 */
public interface Solution {

    //"tentative" local, mutable, cost from the start node
    double g();

    //set "tentative" g
    void setG(double g);

    // get parent of node in a path
    Solution parent();

    //set parent
    void setParent(Solution parent);

    boolean equals(Object other);

    int hashCode();

    boolean goalOf(Solution other);

}

