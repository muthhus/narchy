package nars.task.flow;

import com.gs.collections.api.block.function.primitive.IntToObjectFunction;
import com.gs.collections.impl.map.mutable.ConcurrentHashMapUnsafe;
import nars.budget.Budgeted;
import nars.budget.merge.BudgetMerge;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Consumer;

/**
 * for use by single=thread at a time only
 */
public class BudgetedSet<B extends Budgeted> {

    final Map<B, B> table;
    final BudgetMerge merge;
    final IntToObjectFunction<B[]> arrayer; //sharing this is what makes it valid only for single thread case
    B[] buffer;

    public BudgetedSet(BudgetMerge merge, IntToObjectFunction<B[]> tmpArrayBuilder) {
        this(merge, new ConcurrentHashMapUnsafe<>(), tmpArrayBuilder);
    }

    public BudgetedSet(BudgetMerge merge, Map<B, B> table, IntToObjectFunction<B[]> tmpArrayBuilder) {
        this.merge = merge;
        this.table = table;
        this.arrayer = tmpArrayBuilder;
    }

    /** returns any overflow priority */
    public float put(@NotNull B t) {
        assert(!t.isDeleted()); //throw new RuntimeException("Deleted: " + t);

        B existing = table.put(t, t);
        float overflow;
        if ((existing != null) && (existing != t) && (!existing.isDeleted())) {
            //merge the existing budget into the new entry that was inserted
            overflow = merge.merge(t.budget(), existing, 1f);
        } else {
            overflow = 0;
        }

        return overflow;
    }

    public boolean contains(B b) {
        return table.containsKey(b);
    }



    public B[] flushArray() {
        Map<B, B> t = this.table;

        int s = t.size();

        B[] buffer = this.buffer;

        if (s == 0)
            return buffer;

        //flush(each, t, s,
        buffer =
                ((buffer == null) || (buffer.length < s)) ?
                        //alloc new buffer
                        (this.buffer = arrayer.valueOf(s)) :
                        //use existing
                        buffer;

        t.keySet().toArray(buffer);
        t.clear();

        if (s < buffer.length)
            buffer[s] = null; //null terminator

        return buffer;
    }

    public void flush(@NotNull Consumer<B> each) {
        B[] x = flushArray();
        if (x != null) {
            for (B o : buffer) {
                if ( o == null) break;
                each.accept(o);
            }
        }
    }

    /** each will be a null-terminated array */
    public void flushAll(@NotNull Consumer<B[]> each) {
        B[] x = flushArray();
        if (x != null)
            each.accept(x);
    }



    public void clear() {
        table.clear();
    }

    public boolean isEmpty() {

        return table.isEmpty();
    }


}
