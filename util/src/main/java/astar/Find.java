package astar;


/**
 * Implements trivial functions for a search node.
 */
public abstract class Find<X> implements Solution {

    private double g = 0.0;
    public final X id;
    private Solution parent;

    public Find(X id) {
        this.id = id;
    }

    // get parent of node in a path
    public Solution parent() {
        return this.parent;
    }

    //set parent
    public void setParent(Solution parent) {
        this.parent = parent;
    }

    public boolean equals(Object other) {
        if (this == other) return true;
        if (other instanceof Find) {
            Find otherNode = (Find) other;
            return (this.id.equals(otherNode.id));
        }
        return false;
    }

    public int hashCode() {
        return this.id.hashCode();
    }

    public String toString() {
        return this.id.toString();
    }

    @Override
    public boolean goalOf(Solution other) {
        return equals(other);
    }

    //"tentative" g, cost from the start node
    @Override
    public double g() {
        return this.g;
    }

    //set "tentative" g
    @Override
    public void setG(double g) {
        this.g = g;
    }

}

