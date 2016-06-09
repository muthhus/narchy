package nars.agent;

import nars.Global;
import nars.NAR;
import nars.task.Task;
import nars.truth.Truth;
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

    @Override
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
