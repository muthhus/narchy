package jcog.math;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.eclipse.collections.api.block.procedure.primitive.FloatObjectProcedure;
import org.eclipse.collections.impl.bag.mutable.HashBag;

import java.util.function.Predicate;

/**
 * Created by me on 1/12/17.
 */
public class MultiStatistics<X> implements FloatObjectProcedure<X> {


    public static class Condition<X> extends RecycledSummaryStatistics {
        public final Predicate<X> filter;
        public final String id;
        public final HashBag<X> uniques;

        public Condition(String id, Predicate<X> filter) {
            super();
            this.id = id;
            this.filter = filter;
            this.uniques = new HashBag<>();
        }

        @Override
        public String toString() {
            return id + ": #" + getN() +":" + getMin() + ".." + getMax() + ", avg=" + getMean() + ", sum=" + getSum() + ":" + uniques;
        }

        public void accept(X parameter, float v) {
            if (filter.test(parameter)) {
                accept(v);
                uniques.add(parameter);
            }
        }
    }

    public final Iterable<Condition<X>> cond;

    public MultiStatistics(Condition<X>... cc) {
        this(Lists.newArrayList(cc));
    }

    public MultiStatistics(Iterable<Condition<X>> cond) {
        this.cond = Iterables.concat(
                cond, Lists.newArrayList(
                    new Condition<X>("*", x -> true)
                )
        );
    }

    @Override
    public void value(float v, X parameter) {
        for (Condition<X> cc : cond) {
            cc.accept(parameter, v);
        }
    }

    @Override
    public String toString() {
        return Joiner.on('\n').join(cond);
    }
}
