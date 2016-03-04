package alice.tuprolog;

public final class SubGoalElement extends AbstractSubGoalTree {
    
    public final Term term;
    
    public SubGoalElement(Term t) {
        term = t;
    }

    @Override
    public boolean isLeaf() { return true; }
    @Override
    public boolean isRoot() { return false; }
    
    
    public String toString() {
        return term.toString();
    }
}