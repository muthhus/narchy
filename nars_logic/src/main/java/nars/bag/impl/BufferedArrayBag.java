package nars.bag.impl;

import nars.Global;
import nars.bag.BLink;
import nars.bag.Bag;
import nars.budget.BudgetMerge;
import nars.budget.Budgeted;
import nars.task.flow.BudgetedSet;
import nars.task.flow.SetTaskPerception;
import nars.util.data.sorted.SortedIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Created by me on 2/24/16.
 */
public class BufferedArrayBag<X> extends ArrayBag<X> {

    final BudgetedSet<BLink<X>> pending;

//    public BufferedArrayBag(int cap) {
//
//
//    }
    public BufferedArrayBag(@NotNull SortedIndex<BLink<X>> items) {
        super(items);
        pending = new BudgetedSet(BudgetMerge.plusDQBlend);
    }

    @Override
    public BLink put(X i, Budgeted b, float scale) {
        return pending.put(new BLink(i, b));
    }

    private final Consumer<BLink<X>> flushAction = (BLink<X> b) -> {
        super.put(b.id, b, 1f);
    };

    @Nullable
    @Override
    public BLink<X> sample() {
        commit(true);
        return super.sample();
    }

    @NotNull
    @Override
    public Bag<X> filter(Predicate<BLink<? extends X>> forEachIfFalseThenRemove) {
        commit(false);
        return super.filter(forEachIfFalseThenRemove);
    }

    @Override
    public final void commit() {
        commit(true);
    }

    /** hard commit will re-sort everything (due to changing forgetting rates this has to be iteratively evaluated)
     *  while soft commit (more efficient) will only sort if pendings were merged (safe within approximately the
     *  same cycle range).
      * @param hard
     */
    private final void commit(boolean hard) {
        BudgetedSet<BLink<X>> p = this.pending;
        if (!p.isEmpty()) {
            p.flush(flushAction, BLink[]::new);
        }
        super.commit();
    }

    @Override
    public void forEach(int max, Consumer<? super BLink<X>> action) {
        commit();
        super.forEach(max, action);
    }

    @Override
    public @NotNull Bag<X> forEachThen(Consumer<BLink<? extends X>> each) {
        commit();
        return super.forEachThen(each);
    }

    @Override
    public int size() {
        commit(false);
        return super.size();
    }

    @Override
    public boolean isEmpty() {
        return size()==0;
    }
}
