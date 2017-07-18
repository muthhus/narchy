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

/**
 * represents a causal influence and tracks its
 * positive and negative gain (separately).  this is thread safe
 * so multiple threads can safely affect the accumulators. it must be commited
 * periodically (by a single thread, ostensibly) to apply the accumulated values
 * and calculate the values
 * as reported by the value() function which represents the effective
 * positive/negative balance that has been accumulated. a decay function
 * applies forgetting, and this is applied at commit time by separate
 * positive and negative decay rates.  the value is clamped to a range
 * (ex: 0..+1) so it doesn't explode.
 */
public class Cause {

    /** pos,neg range limit */
    final static float LIMIT = 1;
    final static float EPSILON = 0.0000001f;

    public final short id;
    public final Object x;

    final AtomicDouble posAcc = new AtomicDouble(); //accumulating
    float pos = 0; //current value

    final AtomicDouble negAcc = new AtomicDouble(); //accumulating
    float neg = 0; //current value

    /** summary */
    private float value;

    public Cause(short id, Object x) {
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
            return ArrayUtils.EMPTY_SHORT_ARRAY;

        short[] ll = l.toArray();
        ArrayUtils.reverse(ll);
        assert(ll.length <= maxLen);
        return ll;
    }

    public void apply(float v) {
        if (v >= EPSILON) {
            posAcc.addAndGet(v);
        } else if (v <= -EPSILON) {
            negAcc.addAndGet(-v);
        }
    }

    public void commit(float posDecay, float negDecay) {
        this.pos = decay(pos, posAcc, posDecay);
        this.neg = decay(neg, negAcc, negDecay);
        this.value = value(pos, neg);
    }

    /** calculate the value scalar  from the distinctly tracked positive and negative values;
     * any function could be used here. for example:
     *      simplest:           pos - neg
     *      linear combination: x * pos - y * neg
     *      quadratic:          pos*pos - neg*neg
     *
     * pos and neg will always be positive.
     * */
    public float value(float pos, float neg) {
        return pos - neg;
        //return pos * 2 - neg;
        //return Util.tanhFast( pos ) - Util.tanhFast( neg );
    }

    static float decay(float cur, AtomicDouble acc, float decay) {
        return Util.clamp( (float)((cur * decay) + acc.getAndSet(0)), 0, +LIMIT);
    }

    public float value() {
        return value;
    }
}
