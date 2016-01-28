package nars.util.meter.condition;

import nars.NAR;
import nars.nal.Tense;
import nars.nal.nal8.Operator;
import org.jetbrains.annotations.NotNull;

/**
 * measures the occurrence of an execution within certain
 * time and expectation ranges
 */
public class ExecutionCondition implements NARCondition {

    @NotNull
    private final Operator opTerm;
    private final long start, end;
    private final float minExpect, maxExpect;
    private boolean success = false;
    private long successTime = Tense.TIMELESS;

    public ExecutionCondition(@NotNull NAR n, long start, long end, @NotNull Operator opTerm, float minExpect, float maxExpect) {

        this.start = start;
        this.end = end;
        this.opTerm = opTerm;
        this.minExpect = minExpect;
        this.maxExpect = maxExpect;

        n.onExecution(opTerm, t -> {

            if (!success) {
                long now = n.time();
                if ((now >= start) && (now <= end)) {
                    float expect = t.task.expectation();
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
        return getClass().getSimpleName() + '[' + opTerm + ", time in " +
                start + ".." + end + ", expect in " + minExpect + ".." + maxExpect +
                //minExpect, maxExpect
                ']';
    }

    @Override
    public long getFinalCycle() {
        return end;
    }


}
