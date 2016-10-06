package nars.nal.meta;

import nars.$;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static nars.nal.meta.op.SubtermPathCondition.nonCommutivePathTo;

/**
 * contains a pair of paths to access subterms in 2 terms
 */
public class TaskBeliefSubterms {
    public final int a;
    public final byte[] aPath;
    public final int b;
    public final byte[] bPath;

    public static TaskBeliefSubterms withinNonCommutive(@NotNull Term task, @NotNull Term belief, @NotNull Term arg1, @NotNull Term arg2) {
        //locate an occurrence of arg1
        int t1 = 0;
        byte[] p1 = nonCommutivePathTo(task, arg1);
        if (p1 == null) {
            t1 = 1;
            p1 = nonCommutivePathTo(belief, arg1);
        }
        if (p1 != null) {

            //locate an occurrence of arg2
            int t2 = 0;
            byte[] p2 = nonCommutivePathTo(task, arg2);
            if (p2 == null) {
                t2 = 1;
                p2 = nonCommutivePathTo(belief, arg2);
            }

            if (p2 != null) {
                return new TaskBeliefSubterms(t1, p1, t2, p2);
            }
        }
        return null;
    }
//    public static List<TaskBeliefSubterms> withinAll(@NotNull Term task, @NotNull Term belief, @NotNull Term arg1, @NotNull Term arg2) {
//        List<TaskBeliefSubterms> l = $.newArrayList();
//
//        byte[] p1, p2;
//        int t1, t2;
//
//        //locate an occurrence of arg1
//        if (task.op().commutative) {
//
//        } else {
//            p1 = task.pathTo(arg1);
//            if (p1 == null) {
//                t1 = 1;
//                p1 = task.pathTo(arg2);
//            }
//            l.add(new TaskBeliefSubterms(p1, ))
//
//        }
//        int t1 = 0;
//        byte[] p1 = nonCommutivePathTo(task, arg1);
//        if (p1 == null) {
//            t1 = 1;
//            p1 = nonCommutivePathTo(belief, arg1);
//        }
//        if (p1 != null) {
//
//            //locate an occurrence of arg2
//            int t2 = 0;
//            byte[] p2 = nonCommutivePathTo(task, arg2);
//            if (p2 == null) {
//                t2 = 1;
//                p2 = nonCommutivePathTo(belief, arg2);
//            }
//
//            if (p2 != null) {
//                return new TaskBeliefSubterms(t1, p1, t2, p2);
//            }
//        }
//        return null;
//    }

    public TaskBeliefSubterms(int a, byte[] aPath, int b, byte[] bPath) {
        this.a = a;
        this.aPath = aPath;
        this.b = b;
        this.bPath = bPath;
    }

}
