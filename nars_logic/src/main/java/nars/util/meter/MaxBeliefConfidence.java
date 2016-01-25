package nars.util.meter;

import com.google.common.collect.Lists;
import nars.Global;
import nars.NAR;
import nars.concept.Concept;
import nars.term.Term;
import nars.util.event.CycleReaction;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Calculates the maximum confidence of a concept's beliefs for a specifc frequency
 * TODO support freq ranges
 */
public class MaxBeliefConfidence extends CycleReaction implements Signals {

    @NotNull
    public final Term term;
    @NotNull
    private final NAR nar;
    private final float freq;
    public long bestAt = -1;
    float conf = -1;
    private float best;

    public MaxBeliefConfidence(@NotNull NAR nar, @NotNull String term, float freq) {
        super(nar);
        this.nar = nar;
        this.term = nar.term(term);
        this.freq = freq;
    }

    @Override
    public void onCycle() {
        Concept c = nar.concept(term);
        if (c == null) conf = -1;
        else {
            float lastConf = conf;
            conf = c.beliefs().getConfidenceMax(
                    freq - Global.TRUTH_EPSILON / 2.0f,
                    freq + Global.TRUTH_EPSILON / 2.0f
            );
            if (lastConf < conf) {
                bestAt = nar.time();
                best = conf;
            }

            if (!Float.isFinite(conf))
                conf = -1;
        }
    }

    @NotNull
    @Override
    public String toString() {
        String s = term + " best=" + best + " @ " + bestAt;
        if (best != conf)
            s += " current=" + conf;

        return s;
    }

    @NotNull
    @Override
    public List<Signal> getSignals() {
        return Lists.newArrayList(new Signal(term + "_confMax"));
    }

    @NotNull
    @Override
    public Object[] sample(Object key) {
        return new Object[]{conf};
    }
}
