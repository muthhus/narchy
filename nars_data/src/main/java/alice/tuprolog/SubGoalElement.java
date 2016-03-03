package alice.tuprolog;

public class SubGoalElement extends AbstractSubGoalTree {
    
    private final Term term;
    
    public SubGoalElement(Term t) {
        term = t;
    }
    
    public Term getValue() {
        return term;
    }
    
    @Override
    public boolean isLeaf() { return true; }
    @Override
    public boolean isRoot() { return false; }
    
    
    public String toString() {
        return term.toString();
    }
}