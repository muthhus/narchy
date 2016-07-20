package nars.util.condition;

import nars.$;
import nars.NAR;
import nars.term.atom.Operator;
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
    //private long successTime = Tense.TIMELESS;

    public ExecutionCondition(@NotNull NAR n, long start, long end, @NotNull String opTerm, float minExpect, float maxExpect) {

        this.start = start;
        this.end = end;
        //this.opTerm = opTerm;
        this.minExpect = minExpect;
        this.maxExpect = maxExpect;

        operator = $.oper(opTerm);
        n.onExecution(operator, tt -> {
            if (!success) {
                this.success = (tt.goals().motivation(n.time()) > 0);
            }
        });

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
