package nars.truth;

import nars.$;
import nars.Param;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;

import static nars.truth.TruthFunctions.w2c;

/** thread-safe truth accumulator/integrator */
public class TruthAccumulator extends AtomicReference<double[]> {

    public TruthAccumulator() {
        commit();
    }

    @Nullable public Truth commitAverage() {
        return truth(commit(), false);
    }
    @Nullable public Truth commitSum() {
        return truth(commit(), true);
    }

    protected double[] commit() {
        return getAndSet(new double[3]);
    }

    public Truth peekSum() {
        return truth(get(), true);
    }
    @Nullable public Truth peekAverage() {
        return truth(get(), false);
    }

    @Nullable
    protected static Truth truth(double[] fc, boolean sumOrAverage) {
        if (fc == null)
            return null;
        int n = (int)fc[2];

        double e = fc[1];
        float c = w2c((sumOrAverage) ? ((float)e) : ((float)e)/n);
        if (c <= Param.TRUTH_EPSILON)
            return null;

        return $.t((float)(fc[0]/e), c);
    }

    public void add(@Nullable Truth t) {
        double fe, e;
        if (t == null) {
            e = fe = 0; //just record a zero
        } else {
            double f = t.freq();
            e = t.evi();
            fe = f * e;
        }
        getAndUpdate(fc->{
            fc[0] += fe;
            fc[1] += e;
            fc[2] += 1;
            return fc;
        });
    }



}
