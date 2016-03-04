package alice.tuprolog;

public final class SubGoalElement extends AbstractSubGoalTree {
    
    public final PTerm term;
    
    public SubGoalElement(PTerm t) {
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