package nars.util.signal.condition;

import nars.$;
import nars.NAR;
import nars.nal.Tense;
import nars.term.Operator;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.NotNull;

/**
 * measures the occurrence of an execution within certain
 * time and expectation ranges
 */
public class ExecutionCondition implements NARCondition {

    private final long start, end;
    private final float minExpect, maxExpect;
    private final @NotNull Operator operator;
    private boolean success;
    private long successTime = Tense.TIMELESS;

    public ExecutionCondition(@NotNull NAR n, long start, long end, @NotNull String opTerm, float minExpect, float maxExpect) {

        this.start = start;
        this.end = end;
        //this.opTerm = opTerm;
        this.minExpect = minExpect;
        this.maxExpect = maxExpect;

        operator = $.operator(opTerm);
        n.onExecution(operator, t -> {

            if (!success) {
                long now = n.time();
                if ((now >= start) && (now <= end)) {
                    float expect = t.expectation();
                    if ((expect >= minExpect) && (expect <= maxExpect)) {
                        success = true;
                        successTime = now;
                    }
                }
            }
        });

    }

    @Override
    public long getSuccessTime() {
        return successTime;
    }

    @Override
    public boolean isTrue() {
        return success;
    }

    @NotNull
    @Override
    public String toString() {
        return getClass().getSimpleName() + '[' + operator + ", time in " +
                start + ".." + end + ", expect in " + minExpect + ".." + maxExpect +
                //minExpect, maxExpect
                ']';
    }

    @Override
    public long getFinalCycle() {
        return end;
    }


}
