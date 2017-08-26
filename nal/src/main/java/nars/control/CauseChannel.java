package nars.control;

import jcog.Util;
import jcog.math.AtomicSummaryStatistics;
import jcog.pri.Priority;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

/** metered and mixable extension of Cause base class */
public class CauseChannel<X extends Priority> extends Cause<X> implements Consumer<X> {

    /** linear gain control */
    public float bias, amplitude = 1;

    /** in-bound traffic statistics */
    public final AtomicSummaryStatistics traffic = new AtomicSummaryStatistics();

    final Consumer<X> target;

    public CauseChannel(short id, Object idObj, Consumer<X> target) {
        super(id, idObj);
        this.target = target;
    }

    @Override
    public String toString() {
        return name.toString();
    }

    public final void input(Iterator<? extends X> xx) { xx.forEachRemaining( this ); }

    public final void input(Iterable<? extends X> xx) {
        xx.forEach( this );
    }

    public final void input(Stream<X> x) {
        x.forEach( this );
    }

    public final void input(X... x) {
        for (X p : x)
            input(p);
    }

    public final void input(X x) {
        accept(x);
    }

    @Override
    public void accept(@Nullable X x) {
        if (x == null) return;

        float p = x.pri();
        if (p!=p) return; //deleted

        traffic.accept(p);

        if (bias!=0 || amplitude!=1) {
            x.setPri(bias + p * amplitude);
        }

        target.accept(x);
    }

    public CauseChannel set(float bias, float amplitude) {
        return amplitude(amplitude).bias(bias);
    }
    public CauseChannel bias(float bias) {
        this.bias = bias;
        return this;
    }
    public CauseChannel amplitude(float amp) {
        this.amplitude = amp;
        return this;
    }

    public float factor() {
        return Util.tanhFast(value());
    }
    public float gain() {
         return factor()+1;
    }
}
