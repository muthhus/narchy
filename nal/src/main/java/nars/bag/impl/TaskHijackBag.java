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

import static nars.time.Tense.ETERNAL;

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
    protected Consumer<Task> forget(float rate) {
        return new Forget(rate);
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


    long lastForget = ETERNAL;

    public Task add(@NotNull Task t, @NotNull NAR n) {

        //new Forget( Util.unitize(1f/capacity() + forgetRate) )

        long now = n.time();
        int dur = n.dur();
        if (lastForget + dur < now) {
            update(x -> {
                long s = x.start();
                long e = x.end();
                if (e < now) {
                    //forget old tasks
                    x.budget().priMult( x.qua() );
                }
            });
            lastForget = now;
        }


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


}
