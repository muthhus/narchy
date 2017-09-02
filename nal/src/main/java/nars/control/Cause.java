package nars.control;

import com.google.common.primitives.Doubles;
import com.google.common.util.concurrent.AtomicDouble;
import jcog.Util;
import jcog.list.FasterList;
import jcog.math.RecycledSummaryStatistics;
import nars.Task;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.impl.list.mutable.primitive.ShortArrayList;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
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
public class Cause<X> {

    private float value;

        /** scalar value representing the contribution of this cause to the overall valuation of a potential input that involves it */
    public float value() {
        return value;
    }

    /** value and momentum indices correspond to the possible values in Purpose enum */
    public static void update(FasterList<Cause> causes, float[] value, RecycledSummaryStatistics[] summary) {

        for (RecycledSummaryStatistics r : summary) {
            //double m = r.getMax();
            r.clear();
            //r.setMax(m * 0.9f);
        }

        for (int i = 0, causesSize = causes.size(); i < causesSize; i++) {
            causes.get(i).commit(summary);
        }

        final float LIMIT = +4f;
        final float momentum = 0.95f;

        int p = value.length;
        for (int i = 0, causesSize = causes.size(); i < causesSize; i++) {
            Cause c = causes.get(i);
            float v = 0;
            for (int j = 0; j < p; j++) {
                float y = c.purpose[j].current;
                v += value[j] * RecycledSummaryStatistics.norm( y, 0, summary[j].getMax() );
            }


            float nextValue =
                    Util.clamp(c.value * momentum + v, -LIMIT, LIMIT);
                    //smooth(c.value, v, 0.5f);

            c.setValue(
                    nextValue
            );
        }



//        System.out.println("WORST");
//        causes.stream().map(x -> PrimitiveTuples.pair(x, x.value())).sorted(
//                (x,y) -> Doubles.compare(x.getTwo(), y.getTwo())
//        ).limit(20).forEach(x -> {
//            System.out.println("\t" + x);
//        });
//        System.out.println();

    }

//    public static DurService updates(NAR nar) {
//        return new DurService(nar) {
//            @Override protected void run(NAR nar) {
//                update(nar.causes, nar.value, nar.valueSummary, nar.valueMomentum);
//            }
//        };
//    }


    public enum Purpose {
        /** neg: accepted for input */
        Input,

        /** pos: activated in concept to some degree */
        Process,

        /** pos: anwers a question */
        Answer,

        /** pos: actuated a goal concept */
        Action,

        /** pos: confirmed a sensor input */
        Accurate,

        /** neg: contradicted a sensor input */
        Inaccurate
    }

    /** the AtomicDouble this inherits holds the accumulated value which is periodically (every cycle) committed  */
    public static class Traffic extends AtomicDouble {
        /** current, ie. the last commited value */
        public float current = 0;

        public double total = 0;

        public void commit() {
            double next = getAndSet(0);
            this.total += next;
            this.current = (float) next; //smooth(current, (float)next, momentum);
        }
    }


    public final short id;
    public final Object name;


    public final Traffic[] purpose;

    public Cause(short id, Object name) {
        this.id = id;
        this.name = name;
        purpose = new Traffic[Purpose.values().length];
        for (int i = 0; i < purpose.length; i++) {
            purpose[i] = new Traffic();
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
            case 2: return zip(CAUSE_CAPACITY, e[0]::cause, e[1]::cause);
            default:
                return zip(CAUSE_CAPACITY, Util.map((x) -> x::cause, new Supplier[e.length], e)); //HACK
        }

    }

    public static short[] append(int maxLen, short[] src, short[] add) {
        int addLen = add.length;
        if (addLen == 0) return src;

        int srcLen = src.length;
        if (srcLen + addLen < maxLen) {
            return ArrayUtils.addAll(src, add);
        } else {
            if (addLen >= srcLen) {
                return zip(maxLen, ()->src, ()->add);
            } else {
                short[] dst = new short[maxLen];
                int mid = maxLen - addLen;
                System.arraycopy(src, srcLen - mid, dst, 0, mid);
                System.arraycopy(add, 0, dst, mid, addLen);
                return dst;
            }
        }
    }

    public static short[] zip(int maxLen, Supplier<short[]>... s) {

        int ss = s.length;
        if (ss == 1) {
            return s[0].get();
        }

        ShortArrayList l = new ShortArrayList(maxLen);
        int ls = 0;
        int n = 1;
        boolean remain;
        main: do {
            remain = false;
            for (int i = 0; i < ss; i++) {
                short[] c = s[i].get();
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

    public void apply(Purpose p, float v) {
        purpose[p.ordinal()].addAndGet(v);
    }

    public void setValue(float nextValue) {
        this.value = nextValue;
    }



    void commit(RecycledSummaryStatistics[] valueSummary) {
        for (int i = 0, purposeLength = purpose.length; i < purposeLength; i++) {
            Traffic p = purpose[i];
            p.commit();
            valueSummary[i].accept(p.current);
        }
    }



    static float smooth(float cur, float next, float momentum) {
        return (float)((momentum * cur) + ((1f - momentum) * next));
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