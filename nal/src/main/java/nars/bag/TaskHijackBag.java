package nars.bag;

import jcog.bag.impl.hijack.PriorityHijackBag;
import jcog.pri.Pri;
import jcog.pri.op.PriForget;
import nars.NAR;
import nars.Task;
import nars.concept.BaseConcept;
import nars.control.Activate;
import nars.table.TaskTable;
import nars.task.NALTask;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.function.Consumer;

/**
 * Created by me on 2/17/17.
 */
public class TaskHijackBag extends PriorityHijackBag<Task, Task> implements TaskTable {

    public TaskHijackBag(int reprobes) {
        super(reprobes);
    }

    @Override
    protected Task merge(@NotNull Task existing, @NotNull Task incoming, @Nullable MutableFloat overflowing) {
        float inc = incoming.priElseZero();
        float before = existing.priElseZero();
        existing.priMax(incoming.priElseZero());
        overflowing.add(inc - (existing.priElseZero() /* after */ - before));
        ((NALTask)existing).merge(incoming);
        return existing;
    }


    @NotNull
    @Override
    public Task key(@NotNull Task value) {
        return value;
    }

    @Override
    protected Consumer<Task> forget(float avgToBeRemoved) {
        return new PriForget<>(avgToBeRemoved);
    }


    @Override
    public Iterator<Task> taskIterator() {
        return iterator();
    }


    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public void forEachTask(Consumer<? super Task> x) {
        forEachKey(x);
    }

    @Override
    public boolean removeTask(Task x) {
        return remove(x) != null;
    }

    @Override
    public void add(@NotNull Task x, BaseConcept c, NAR n) {

        float activation = x.priSafe(0);


        MutableFloat oo = new MutableFloat();
        @Nullable Task y = put(x, oo);
        if (y == null) {
            //not inserted
            return;
        } else {
            if (y!=x) {
                //fully inserted or merged with existing item, and activate only the absorbed amount
                activation -= oo.floatValue();
                x.delete();
                if (activation < Pri.EPSILON) {
                    return; //no significant change
                }
            }
        }

        Activate.activate(y, activation, n);
    }


//    public static void flatForget(TaskHijackBag b, @NotNull NAR n) {
//        double p = b.pressure.get();
//        int s = b.size();
//
//        float ideal = b.size() * b.temperature();
//        if (p > ideal/2f) {
//            if (b.pressure.compareAndSet(p, 0)) {
//
//                b.commit(null); //precommit to get accurate mass
//                float mass = b.mass;
//
//                float over = (float) ((p + mass) - ideal);
//                float overEach = over / s;
//                if (overEach >= Param.BUDGET_EPSILON) {
//                    long now = n.time();
//                    b.commit(x -> {
//                        //long s = x.start();
//                        long e = x.end();
//                        if (e < now) {
//                            //forget old tasks
//                            //x.budget().priMult(1f - ((1f - x.qua()) / s));
//                            x.budget().priSub(overEach * (1f - x.qua()));
//                        }
//                    });
//                }
//            }
//
//        }
//    }


}
