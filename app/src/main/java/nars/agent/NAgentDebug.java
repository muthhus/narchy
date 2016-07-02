package nars.agent;

import com.gs.collections.api.block.function.primitive.FloatToObjectFunction;
import nars.Global;
import nars.NAR;
import nars.Narsese;
import nars.task.Task;
import nars.truth.Truth;
import nars.util.math.FloatSupplier;
import nars.util.signal.SensorConcept;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;

/**
 * Created by me on 5/5/16.
 */
public class NAgentDebug extends NAgent  {

    private final PrintStream log;

    boolean printConflict;

    public NAgentDebug(NAR n) {
        super(n);

        Global.DEBUG = true;

        log = System.out;
    }

    public class SensorConceptDebug extends SensorConcept {

        public SensorConceptDebug(@NotNull String term, @NotNull NAR n, FloatSupplier input, FloatToObjectFunction<Truth> truth) throws Narsese.NarseseException {
            super(term, n, input, truth);
        }

        @Override
        protected void onConflict(@NotNull Task belief) {
            NAgentDebug.this.onConflict(this, belief);
        }
    }

    protected void onConflict(SensorConceptDebug c, Task belief) {
        if (printConflict) {
            log.println("CONFLICT");
            log.println(belief.explanation());
            c.beliefs().print(log);
            @Nullable Truth projected = c.belief(belief.occurrence());
            log.println("@" + belief.occurrence() + ": incoming=" +
                    belief.truth() + " " + belief.expectation() + "\tvs\tprojected=" +
                    projected + " " + projected.expectation());

            log.println();
            log.println();
        }
    }
}
