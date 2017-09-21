package nars.control;

import jcog.Util;
import jcog.math.RecycledSummaryStatistics;
import nars.Task;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.ShortIterable;
import org.eclipse.collections.api.block.predicate.primitive.ShortPredicate;
import org.eclipse.collections.api.set.primitive.ShortSet;
import org.eclipse.collections.impl.list.mutable.primitive.ShortArrayList;
import org.eclipse.collections.impl.set.mutable.primitive.ShortHashSet;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

import static nars.Param.CAUSE_CAPACITY;

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

    /** current scalar utility estimate for this cause's support of the current MetaGoal's.
     *  may be positive or negative, and is in relation to other cause's values
     */
    private float value;

    /** the value measured contributed by its effect on each MetaGoal.
     *  the index corresponds to the ordinal of MetaGoal enum entries.
     *  these values are used in determining the scalar 'value' field on each update. */
    public final Traffic[] goalValue;


    protected float valuePreNorm;

    /** flag indicating whether the value should be included in aggregations that adjust priority of items */
    public boolean valuePrioritizes = true;


    /** scalar value representing the contribution of this cause to the overall valuation of a potential input that involves it */
    public float value() {
        //assert(v==v && v >= -1f && v <= +1f);
        return value;
    }

    /** 0..+1 */
    public float amp() {
        return (value+1)/2;
    }

    /** 0..+2 */
    public float gain() {
         return value+1;
    }

    /** convenience procedure to set value to zero */
    public void setValueZero() {
        value = 0;
    }

    public void setValue(float nextValue) {
        assert(nextValue==nextValue && nextValue >= -1f && nextValue <= +1f);
        value = nextValue;
    }


    /** internally assigned id */
    public final short id;

    public final Object name;

    public Cause(short id) {
        this(id, null);
    }

    public Cause(short id, @Nullable Object name) {
        this.id = id;
        this.name = name!=null ? name : id;
        goalValue = new Traffic[MetaGoal.values().length];
        for (int i = 0; i < goalValue.length; i++) {
            goalValue[i] = new Traffic();
        }
    }

    @Override
    public String toString() {
        return name + "[" + id + "]=" + super.toString();
    }


    public static short[] zip(@Nullable Task... e) {
        switch (e.length) {
            case 0: throw new NullPointerException();
            case 1: return e[0].cause();
            case 2: return zip(CAUSE_CAPACITY, e[0].cause(), e[1].cause());
            default:
                return zip(CAUSE_CAPACITY, Util.map(Task::cause, short[][]::new, e)); //HACK
        }
    }

//    public static short[] append(int maxLen, short[] src, short[] add) {
//        int addLen = add.length;
//        if (addLen == 0) return src;
//
//        int srcLen = src.length;
//        if (srcLen + addLen < maxLen) {
//            return ArrayUtils.addAll(src, add);
//        } else {
//            if (addLen >= srcLen) {
//                return zip(maxLen, ()->src, ()->add);
//            } else {
//                short[] dst = new short[maxLen];
//                int mid = maxLen - addLen;
//                System.arraycopy(src, srcLen - mid, dst, 0, mid);
//                System.arraycopy(add, 0, dst, mid, addLen);
//                return dst;
//            }
//        }
//    }



    public static short[] zip(int maxLen, Supplier<short[]>[] s) {
        if (s.length == 1) {
            return s[0].get();
        }
        return zip(maxLen, Util.map(Supplier::get, short[][]::new, s));
    }

    public static short[] zip(int maxLen, short[]... s) {

        int ss = s.length;

        int totalItems = 0;
        short[] lastNonEmpty = null;
        int nonEmpties = 0;
        for (short[] t : s) {
            int tl = t.length;
            totalItems += tl;
            if (tl > 0) {
                lastNonEmpty = t;
                nonEmpties++;
            }
        }
        if (nonEmpties==1)
            return lastNonEmpty;
        if (totalItems == 0)
            return ArrayUtils.EMPTY_SHORT_ARRAY;

        boolean enough = (totalItems < maxLen);
        ShortIterable l;
        ShortPredicate adder;
        if (enough) {
            AwesomeShortArrayList ll = new AwesomeShortArrayList(totalItems);
            l = ll;
            adder = ll::add;
        } else {
            ShortHashSet ll = new ShortHashSet(maxLen);
            l = ll;
            adder = ll::add;
        }



        int ls = 0;
        int n = 0;
        int done;
        main: do {
            done = 0;
            for (int i = 0; i < ss; i++) {
                short[] c = s[i];
                if (n < c.length) {
                    if (adder.accept(c[n])) {
                        if (++ls >= maxLen)
                            break main;
                    }
                } else {
                    done++;
                }
            }
            n++;
        } while (done < ss);

        assert(ls > 0);
        short[] ll = l.toArray();
        assert(ll.length == ls);
        return ll;
    }

    /** learn the utility of this cause with regard to a goal. */
    public void learn(MetaGoal p, float v) {
        p.learn(goalValue, v);
    }


    void commit(RecycledSummaryStatistics[] valueSummary) {
        for (int i = 0, purposeLength = goalValue.length; i < purposeLength; i++) {
            Traffic p = goalValue[i];
            p.commit();
            valueSummary[i].accept(p.current);
        }
    }

    static class AwesomeShortArrayList extends ShortArrayList {

        public AwesomeShortArrayList(int cap) {
            super(cap);
        }

        @Override public short[] toArray() {
            if (this.size() == items.length)
                return items;
            else
                return super.toArray();
        }

    }

}
//    /** calculate the value scalar  from the distinctly tracked positive and negative values;
//     * any function could be used here. for example:
//     *      simplest:           pos - neg
//     *      linear combination: x * pos - y * neg
//     *      quadratic:          pos*pos - neg*neg
//     *
//     * pos and neg will always be positive.
//     * */
//    public float value(float pos, float neg) {
//        return pos - neg;
//        //return pos * 2 - neg;
//        //return Util.tanhFast( pos ) - Util.tanhFast( neg );
//    }