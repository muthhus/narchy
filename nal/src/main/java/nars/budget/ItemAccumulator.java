package nars.budget;

import nars.bag.BLink;
import nars.bag.impl.ArrayBag;
import nars.util.ArraySortedIndex;
import nars.util.data.sorted.SortedIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;

/** priority queue which merges equal tasks and accumulates their budget.
 * stores the highest item in the last position, and lowest item in the first.
 * one will generally only need to use these methods:
 *      --limit(n) - until size <= n: remove lowest items
 *      --next(n, consumer(n)) - visit N highest items
 *
 * */
public class ItemAccumulator<V extends Budgeted> {


    @NotNull
    private final MyArrayBag arrayBag;

    public ItemAccumulator(int capacity, BudgetMerge merge) {
        arrayBag = new MyArrayBag(new BLinkArraySortedIndex<>(capacity));
        arrayBag.merge(merge);
    }


    public void print(PrintStream out) {
        arrayBag.items.print(out);
    }

    @NotNull
    public ArrayBag<V> bag() {
        return arrayBag;
    }

    private static class BLinkArraySortedIndex<V extends Budgeted> extends ArraySortedIndex<BLink<V>> {

        public BLinkArraySortedIndex(int capacity) {
            super(capacity);
        }

        @Override
        public float score(@NotNull BLink<V> v) {
            return v.pri();
        }
    }

    final class MyArrayBag extends ArrayBag<V> {
        public MyArrayBag(@NotNull SortedIndex<BLink<V>> items) {
            super(items);
        }

        @Override
        public final BLink<V> sample() {
            return items.getFirst();
        }

        @Nullable
        @Override
        public BLink<V> pop() {
            return removeHighest();
        }

        @Override
        public void update(@NotNull BLink<V> v) {
            super.update(v);
            v.get().budget().set(v); //TODO replace instance's budget on insert so this copy isnt necessary
        }

//        @Override
//        public final boolean contains(V t) {
//            return containsKey(t);
//        }
    }



}
