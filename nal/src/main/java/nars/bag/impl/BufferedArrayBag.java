package nars.bag.impl;

import nars.bag.BLink;
import nars.bag.Bag;
import nars.budget.BudgetMerge;
import nars.budget.Budgeted;
import nars.task.flow.BudgetedSet;
import nars.util.data.sorted.SortedIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Created by me on 2/24/16.
 */
public class BufferedArrayBag<X> extends ArrayBag<X> {

    @NotNull
    final BudgetedSet<BLink<X>> pending;

//    public BufferedArrayBag(int cap) {
//
//
//    }
    public BufferedArrayBag(@NotNull SortedIndex<BLink<X>> items) {
        super(items);
        pending = new BudgetedSet(BudgetMerge.plusDQBlend, new LinkedHashMap(), BLink[]::new);
    }

    @Override
    public BLink put(X i, @NotNull Budgeted b, float scale) {
        return pending.put(new BLink(i, b));
    }

    @Nullable
    private final Consumer<BLink<X>[]> flushAction = (BLink<X>[] b) -> {
        for (BLink<X> c : b) {
            if (c == null) break;
            super.put(c.id, c, 1f);
        }
    };

    @Nullable
    @Override
    public BLink<X> sample() {
        commit(true);
        return super.sample();
    }

    @NotNull
    @Override
    public Bag<X> filter(@NotNull Predicate<BLink<? extends X>> forEachIfFalseThenRemove) {
        commit(false);
        return super.filter(forEachIfFalseThenRemove);
    }

    @NotNull
    @Override
    public Bag<X> sample(int n, Consumer<? super BLink<X>> target) {
        commit(false);
        return super.sample(n, target);
    }

    @Override
    public final Bag<X> commit() {
        commit(true);
        return null;
    }

    /** hard commit will re-sort everything (due to changing forgetting rates this has to be iteratively evaluated)
     *  while soft commit (more efficient) will only sort if pendings were merged (safe within approximately the
     *  same cycle range).
      * @param hard
     */
    private final void commit(boolean hard) {
        BudgetedSet<BLink<X>> p = this.pending;
        boolean added = !p.isEmpty();
        if (added) {
            p.flushAll(flushAction);
        }

        if (added || hard)
            super.commit();
    }

    @Override
    public void forEach(int max, @NotNull Consumer<? super BLink<X>> action) {
        commit();
        super.forEach(max, action);
    }

    @Override
    public @NotNull Bag<X> forEachThen(@NotNull Consumer<BLink<? extends X>> each) {
        commit();
        return super.forEachThen(each);
    }


    @Override
    public @NotNull Iterator<BLink<X>> iterator() {
        commit(true);
        return super.iterator();
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
