package nars.nal.nal7;

import nars.$;
import nars.NAR;
import nars.Narsese;
import nars.Task;
import nars.util.data.TimeMap;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

import static org.junit.Assert.assertTrue;


abstract public class TemporalStabilityTest {

    Set<Task> irregular = $.newHashSet(1);

    private boolean stopOnFirstError = true;

    public void test(int cycles, @NotNull NAR n) throws Narsese.NarseseException {

        //n.log();
        n.onCycle(f -> {

            TimeMap m = new TimeMap(n);

            //Set<Between<Long>> times = m.keySetSorted();
            /*if (times.size() < 3)
                continue; //wait until the initial temporal model is fully constructed*/

            //m.print();
            m.forEach(tt -> {

                long start = tt.start();
                long end = tt.end();
                if (!validOccurrence(start) || (end != start && !validOccurrence(tt.end())))  {
                    if (irregular.add(tt)) { //already detected?
                        if (stopOnFirstError)
                            n.stop();
                        //System.err.println("  instability: " + tt + "\n" + tt.proof() + "\n");
                    }
                }
            });


        });

        input(n);

        run(cycles, n);

    }

    private void run(int cycles, NAR n) {

        if (cycles > 0) {
            irregular.clear();

            n.run(cycles);

            //evaluate(n);
        }
    }

    public void evaluate(@NotNull NAR n) {

        if (!irregular.isEmpty()) {

//            TimeMap m = new TimeMap(n);

            irregular.forEach(i -> {

                System.err.println(i.proof());
            });

            //m.print();

            assertTrue(false);
        }

    }


    abstract public boolean validOccurrence(long o);

    /**
     * inputs the tasks for a test
     */
    abstract public void input(NAR n) throws Narsese.NarseseException;
}
