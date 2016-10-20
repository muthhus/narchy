package nars;

import nars.util.Loop;
import nars.util.Util;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * self managed set of processes which run a NAR
 * as a loop at a certain frequency.
 * TODO extract the hft core reservation to a subclass and put that in the app module, along with the hft dependency
 * <p>
 * mostly replaced by Executioner's
 */
@Deprecated
public class NARLoop extends Loop {

    private static final Logger logger = getLogger(NARLoop.class);


    @NotNull
    public final NAR nar;


    //private boolean running;


    /**
     * average desired cpu percentage
     */
    //public final MutableFloat priority = new MutableFloat(1f);
    private static final int framesPerLoop = 1;




    /**
     * @param n
     * @param initialPeriod
     */
    public NARLoop(@NotNull NAR n, int initialPeriod) {
        super(n.self + ":loop");

        nar = n;

        start(initialPeriod);

    }


    @Override
    public final void next() {

        //if (!nar.running.get()) {

        nar.run(framesPerLoop);
    }


}
