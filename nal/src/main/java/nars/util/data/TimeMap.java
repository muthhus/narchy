package nars.util.data;

import jcog.tree.rtree.HyperRect;
import jcog.tree.rtree.RTree;
import jcog.tree.rtree.rect.RectDouble2D;
import nars.NAR;
import nars.Task;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Function;


public class TimeMap extends RTree<Task> implements Consumer<Task>, Function<Task,HyperRect> {

    public TimeMap() {

    }

    public TimeMap(@NotNull NAR n) {
        this();
        n.forEachConceptTask(this, true, true, false, false);
    }

    @Override
    public HyperRect apply(Task task) {
        //return new RectLong1D(task.start(), task.end());
        int h = task.hashCode();
        return new RectDouble2D(task.start(), task.end(), h, h);
    }

    @Override
    public void accept(@NotNull Task task) {
        if (!task.isEternal()) {
            add(task);
        }
    }

//    public void print() {
//        out.println(nar.time() + ": " + "Total tasks: " + size() + '\t' + keySetSorted().toString());
//    }


}


//package nars.util.data;
//
//        import jcog.time.IntervalTree;
//        import nars.NAR;
//        import nars.Task;
//        import org.jetbrains.annotations.NotNull;
//
//        import java.util.function.Consumer;
//
//        import static java.lang.System.out;
//
//
//public class TimeMap0 extends IntervalTree<Long, Task> implements Consumer<Task> {
//
//    @NotNull
//    private final NAR nar;
//
//    public TimeMap0(@NotNull NAR n) {
//        this.nar = n;
//        n.forEachTask(this, true, true, false, false);
//    }
//
//    @Override
//    public void accept(@NotNull Task task) {
//        if (!task.isEternal()) {
//            put(task.start(), task);
//        }
//    }
//
//    public void print() {
//        out.println(nar.time() + ": " + "Total tasks: " + size() + '\t' + keySetSorted().toString());
//    }
//
//
//}
