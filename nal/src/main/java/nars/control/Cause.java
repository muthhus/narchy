package nars.control;

import com.google.common.util.concurrent.AtomicDouble;
import jcog.Util;
import jcog.list.FasterList;
import nars.Param;
import nars.Task;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.impl.list.mutable.primitive.ShortArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Cause extends AtomicDouble /* value */ {
    public final short id;
    public final Object x;

    float next = 0;

    public Cause(short id, Object x) {
        super(0);
        this.id = id;
        this.x = x;
    }

    @Override
    public String toString() {
        return x + "Cause[" + id + "]=" + super.toString();
    }

    public static short[] zip(@Nullable FasterList<Task> e) {
        return zip(e, Param.CAUSE_CAPACITY);
    }
    public static short[] zip(@Nullable Task... e) {
        return zip(new FasterList(e)); //HACK
    }

    static short[] zip(@NotNull List<? extends Task> s, int maxLen) {

        int ss = s.size();
        if (ss == 1) {
            return s.get(0).cause();
        }

        ShortArrayList l = new ShortArrayList(maxLen);
        int ls = 0;
        int n = 1;
        boolean remain;
        main: do {
            remain = false;
            for (int i = 0, sSize = s.size(); i < sSize; i++) {
                Task x = s.get(i);
                short[] c = x.cause();
                int cl = c.length;
                if (cl >= n) {
                    l.add(c[cl - n]);
                    if (++ls >= maxLen)
                        break main;
                    remain |= (cl >= (n + 1));
                }
            }
            n++;
        } while (remain);
        if (ls == 0)
            return ArrayUtils.EMPTY_SHORT_ARRAY; //shared

        short[] ll = l.toArray();
        ArrayUtils.reverse(ll);
        assert(ll.length <= maxLen);
        return ll;
    }

    public void apply(float v) {
        addAndGet(v);
    }

    public void commit(float decayRate) {
        float n = (float) this.getAndSet(0);
        this.next = (Util.clamp((next * decayRate + n), -1f, +2f));
    }

    public float value() {
        return next;
    }
}