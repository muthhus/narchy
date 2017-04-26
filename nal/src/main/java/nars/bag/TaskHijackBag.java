package nars.bag;

import jcog.bag.impl.hijack.PriorityHijackBag;
import jcog.pri.PForget;
import jcog.pri.PriMerge;
import nars.Task;
import nars.table.TaskTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.function.Consumer;

/**
 * Created by me on 2/17/17.
 */
public class TaskHijackBag extends PriorityHijackBag<Task,Task> implements TaskTable {

    public TaskHijackBag(int reprobes) {
        super(reprobes);
    }

    @Override
    public void onRemoved(@NotNull Task t) {
        t.delete();
    }

    @Override
    protected Task merge(@Nullable Task existing, @NotNull Task incoming, float scaleIgnored) {
        if (existing!=null) {
            Task next;

            //prefer the existing task unless the newer has a grown start/stop range
            //  (which is possible from an input task which has grown in timespan)
            if (incoming.isInput() && (incoming.start() < existing.start() || incoming.end() > existing.end())) {
                PriMerge.max(incoming, existing);
                next = incoming; //use the newer task
            } else {
                PriMerge.max(existing, incoming);
                next = existing; //use the existing
            }
            return next;
        } else {
            return incoming;
        }
    }

    @NotNull
    @Override
    public Task key(Task value) {
        return value;
    }

    @Override
    protected Consumer<Task> forget(float avgToBeRemoved) {
        return new PForget<>(avgToBeRemoved);
    }


    @Override
    public Iterator<Task> taskIterator() {
        return iterator();
    }


    @Override
    public boolean isEmpty() {
        return size()==0;
    }

    @Override
    public void forEachTask(Consumer<? super Task> x) {
        forEachKey(x);
    }

    @Override
    public boolean removeTask(Task x) {
        if (remove(x)!=null) {
            return true;
        }
        return false;
    }

    public Task add(@NotNull Task t) {

        commit();

        Task x = put(t);
        return x;
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
