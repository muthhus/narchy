package nars.table;

import com.google.common.collect.Iterators;
import nars.NAR;
import nars.Task;
import nars.bag.impl.HijackBag;
import nars.budget.BLink;
import nars.budget.BudgetMerge;
import nars.budget.DependentBLink;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by me on 2/16/17.
 */
public class HijackQuestionTable extends HijackBag<Task> implements QuestionTable {

    public HijackQuestionTable(int cap, int reprobes, BudgetMerge merge, Random random) {
        super(cap, reprobes, merge, random);
    }


    @Override
    public Iterator<Task> taskIterator() {
        return Iterators.transform(iterator(), Supplier::get);
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

        boolean present = contains(t);

        commit();

        BLink inserted = put(new DependentBLink<Task>(t, t.priSafe(0), t.qua()));
        if (inserted!=null) {
            return !present ? t : null;
        }

        return null;
    }

    @Override
    public void capacity(int newCapacity, NAR nar) {
        capacity(newCapacity);
    }

    @Override
    public boolean isEmpty() {
        return size()==0;
    }

}
