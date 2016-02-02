package nars.util.signal;

import com.gs.collections.api.block.function.primitive.FloatToFloatFunction;
import nars.NAR;
import nars.Symbols;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.Term;
import nars.util.data.Util;

import java.util.function.Consumer;
import java.util.function.DoubleSupplier;

/**
 * Created by me on 2/2/16.
 */
public class Sensor implements Consumer<NAR> {

    /**
     * resolution of the output freq value
     */
    static final float freqResolution = 0.05f;

    private final Term term;
    private final DoubleSupplier value;
    private final FloatToFloatFunction freq;
    private final NAR nar;
    private final float pri;
    private final float dur;
    private float conf;
    private float prevF;
    boolean inputIfSame = false;

    public Sensor(NAR n, Term t, DoubleSupplier value, FloatToFloatFunction valueToFreq) {
        this(n, t, value, valueToFreq, n.memory.getDefaultConfidence(Symbols.JUDGMENT),
                n.memory.DEFAULT_JUDGMENT_PRIORITY, n.memory.DEFAULT_JUDGMENT_DURABILITY);
    }

    public Sensor(NAR n, Term t, DoubleSupplier value, FloatToFloatFunction valueToFreq, float conf, float pri, float dur) {
        this.nar = n;
        this.term = t;
        n.onFrame(this);
        this.value = value;
        this.freq = valueToFreq;
        this.conf = conf;

        this.pri = pri;
        this.dur = dur;

        this.prevF = Float.NaN;
    }

    @Override
    public void accept(NAR nar) {
        double nextD = value.getAsDouble();
        if (!Double.isFinite(nextD))
            return; //allow the value function to prevent input by returning NaN
        float next = (float) nextD;

        float fRaw = freq.valueOf(next);
        if (!Float.isFinite(fRaw))
            return; //allow the frequency function to prevent input by returning NaN


        float f = Util.round(fRaw, freqResolution);
        if (inputIfSame || f != prevF) {
            input(f);
        }

        this.prevF = f;
        //this.prevValue = next;
    }

    private void input(float f) {
        Task t = new MutableTask(term).belief().truth(f, conf).present(nar.time()).budget(pri, dur);
        nar.input(t);
    }

    public void setConf(float conf) {
        this.conf = conf;
    }


    //        public void on() {
//
//        }
//        public void off() {
//
//        }
}
