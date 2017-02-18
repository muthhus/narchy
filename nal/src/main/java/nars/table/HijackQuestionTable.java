package nars.table;

import nars.NAR;
import nars.Task;
import nars.bag.impl.TaskHijackBag;
import nars.budget.BudgetMerge;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Created by me on 2/16/17.
 */
public class HijackQuestionTable extends TaskHijackBag implements QuestionTable {

    private long lastCommitTime = Long.MIN_VALUE;

    public HijackQuestionTable(int cap, int reprobes, BudgetMerge merge, Random random) {
        super(reprobes, merge, random);

        capacity(cap);
    }

    @Override
    public Iterator<Task> taskIterator() {
        return iterator();
    }

    @Override
    public void forEachTask(Consumer<? super Task> x) {
        forEachKey(x);
    }

    @Override
    public boolean removeTask(Task x) {
        return remove(x)!=null;
    }

    @Override
    public @Nullable Task add(@NotNull Task t, @NotNull BeliefTable answers, @NotNull NAR n) {

        long now = n.time();
        if (now != lastCommitTime) {
            commit();
            lastCommitTime = now;
        }

        Task inserted = put(t);
        if (inserted!=null) {
            //signal successful insert when inserted item is what is inserted, not a pre-existing duplicate
            return (inserted == t) ? t : null;
        } else {
            return null;
        }
    }

    @Override
    public void capacity(int newCapacity, NAR nar) {
        setCapacity(newCapacity); //hijackbag
        capacity(newCapacity); //question table
    }

    @Override
    public boolean isEmpty() {
        return size()==0;
    }

}
