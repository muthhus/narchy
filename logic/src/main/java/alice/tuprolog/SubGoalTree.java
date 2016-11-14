package alice.tuprolog;

import nars.util.list.FasterList;

import java.util.Iterator;
import java.util.List;


public final class SubGoalTree extends FasterList<AbstractSubGoalTree> implements AbstractSubGoalTree, Iterable<AbstractSubGoalTree> {

    public SubGoalTree() {
        super();
    }

    public SubGoalTree(List<AbstractSubGoalTree> terms) {
        super(terms);
    }

    public SubGoalTree addChild() {
        SubGoalTree r = new SubGoalTree();
        add(r);
        return r;
    }


    @Override
    public boolean isLeaf() { return false; }
    @Override
    public boolean isRoot() { return true; }
    
    public String toString() {
        String result = " [ ";
        Iterator<AbstractSubGoalTree> i = iterator();
        if (i.hasNext())
            result += i.next().toString();
        while (i.hasNext()) {
            result += " , " + i.next().toString();
        }
        return result + " ] ";
    }

//    public boolean removeChild(int i) {
//        try {
//            remove(i);
//            return true;
//        } catch (Exception e) {
//            return false;
//        }
//    }

    public SubGoalTree copy(){
        return new SubGoalTree(this);
    }
}
