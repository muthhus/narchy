package alice.tuprolog;

/**
 * Identifier of single subGoal during the demo.
 * @author Alex Benini
 *
 */
public class SubGoal {
    
    public final SubGoalTree root;
    public final int index;
    public final SubGoal parent;
    
    SubGoal(SubGoal parent, SubGoalTree root, int index) {
        this.parent = parent;
        this.root = root;
        this.index = index;
    }


    public String toString() {
        return root.get(index).toString();
    }

}