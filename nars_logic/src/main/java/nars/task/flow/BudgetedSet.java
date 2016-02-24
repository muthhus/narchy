package nars.task.flow;

import com.gs.collections.api.block.function.primitive.IntToObjectFunction;
import nars.budget.BudgetMerge;
import nars.budget.Budgeted;
import nars.util.data.list.FasterList;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Created by me on 2/24/16.
 */
public class BudgetedSet<B extends Budgeted> {

    final Map<B, B> table;
    final BudgetMerge merge;

    public BudgetedSet(BudgetMerge merge) {
        this(merge, new LinkedHashMap<>());
    }

    public BudgetedSet(BudgetMerge merge, Map<B, B> table) {
        this.merge = merge;
        this.table = table;
    }

    public B put(B t) {
        if (t.isDeleted()) {
            throw new RuntimeException("Deleted: " + t);
        }
        B existing = table.put(t, t);
        if ((existing != null) && (existing != t) && (!existing.isDeleted())) {
            merge.merge(t.budget(), existing.budget(), 1f);
        }
        return existing;
    }

    public boolean contains(B b) {
        return table.containsKey(b);
    }

    public boolean flush(Consumer<B> each, IntToObjectFunction<B[]> tmpArrayBuilder) {
        Map<B, B> t = this.table;
        int size = t.size();
        if (size > 0) {

            B[] a = tmpArrayBuilder.valueOf(size);

            FasterList l = new FasterList(0, a);
            l.addAll(t.keySet());
            t.clear();

            for (B b : a) {
                each.accept(b);
            }

            return true;
        }
        return false;
    }


    public void clear() {
        table.clear();
    }

    public boolean isEmpty() {

        return table.isEmpty();
    }

}
