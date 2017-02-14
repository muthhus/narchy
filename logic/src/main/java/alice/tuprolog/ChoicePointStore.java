package alice.tuprolog;

import java.util.ArrayList;
import java.util.List;


public class ChoicePointStore {
    
    
    private ChoicePointContext pointer;
    
    public ChoicePointStore() {
        pointer = null;
    }
    
    public void add(ChoicePointContext cpc) {
        if (pointer == null) {
            pointer = cpc;
            return;
        }
        ChoicePointContext oldCtx = pointer;
        cpc.prevChoicePointContext = oldCtx;
        pointer = cpc;
    }
    
    public void cut(ChoicePointContext pointerAfterCut) {
        pointer = pointerAfterCut;
    }
    
    /**
     * Return the correct choice-point
     */
    public ChoicePointContext fetch() {
        return (existChoicePoint()) ? pointer : null;
    }
    
    /**
	 * Return the actual choice-point store
	 * @return
	 */
    public ChoicePointContext getPointer() {
        return pointer;
    }
    
//    /**
//     * Check if a choice point exists in the store.
//     * As a side effect, removes choice points which have been already used and are now empty.
//     * @return
//     */
//    protected boolean existChoicePoint() {
//        ChoicePointContext pointer = this.pointer;
//        if (pointer == null)
//            return false;
//        do {
//            if (pointer.compatibleGoals.existCompatibleClause())
//                return true;
//        } while ((pointer = pointer.prevChoicePointContext) != null);
//        return false;
//    }
    /**
     * Check if a choice point exists in the store.
     * As a side effect, removes choice points which have been already used and are now empty.
     * @return
     */
    protected boolean existChoicePoint() {
        ChoicePointContext pointer = this.pointer;
        if (pointer == null) return false;
        do {
            if (pointer.compatibleGoals.existCompatibleClause())
                return true;
            this.pointer = pointer = pointer.prevChoicePointContext;
        } while (pointer != null);
        return false;
    }


    /**
     * Removes choice points which have been already used and are now empty.
     */
    protected void removeUnusedChoicePoints() {
        // Note: it uses the side effect of this.existChoicePoint()!
        existChoicePoint();
    }
    
    /**
     * Cut at defined depth (toDepth)
     */
//    void cut(int toDepth) {
//        while (pointer != null && pointer.executionContext.depth >= toDepth) {
//            pointer = pointer.prevChoicePointContext;
//        }
//    }
    
    public String toString(){
        return pointer + "\n";
    }
    
    /*
     * Methods for spyListeners
     */
    
    public List<ChoicePointContext> getChoicePoints() {
        ArrayList<ChoicePointContext> l = new ArrayList<>();
        ChoicePointContext t = pointer;
        while (t != null) {
            l.add(t);
            t = t.prevChoicePointContext;
        }
        return l;
    }
    
}