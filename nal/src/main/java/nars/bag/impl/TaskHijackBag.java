package nars.bag.impl;

import nars.NAR;
import nars.Task;
import nars.attention.Forget;
import nars.budget.BudgetMerge;
import nars.table.TaskTable;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Created by me on 2/17/17.
 */
public class TaskHijackBag extends BudgetHijackBag<Task,Task> implements TaskTable {


    public TaskHijackBag(int reprobes, BudgetMerge merge, Random random) {
        super(random, merge, reprobes);
    }

    @Override
    public void forEach(int max, @NotNull Consumer<? super Task> action) {
        super.forEach(max, action);
    }

//    @NotNull
//    @Override
//    public HijackBag<Task, Task> commit() {
//        //BLinkHijackBag.flatForget(this);
//        //return this;
//    }

    @Override
    public void onRemoved(@NotNull Task value) {
        value.delete();
    }

    @Override
    public float pri(@NotNull Task key) {
        return key.priSafe(0);
    }

    @NotNull
    @Override
    public Task key(Task value) {
        return value;
    }

    @Override
    protected Consumer<Task> forget(float avgToBeRemoved) {
        return new Forget(avgToBeRemoved);
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
        return remove(x)!=null;
    }


    //long lastForget = ETERNAL;

    public Task add(@NotNull Task t, @NotNull NAR n) {

        BLinkHijackBag.flatForget(this );

        //new Forget( Util.unitize(1f/capacity() + forgetRate) )

//        int dur = n.dur();
//        /*if (lastForget + dur < now)*/ {
//            lastForget = now;
//        }


        return put(t);

        //commit();

//        if (inserted!=null) {
//            //signal successful insert when inserted item is what is inserted, not a pre-existing duplicate
//            if (inserted.equals(t)) {
//                //merged budget with an existing but unique instance
//                return inserted.isInput() ? t : null; //ignore duplicate derivations
//            }
//        }
//
//        //failed insert
//        return null;
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
