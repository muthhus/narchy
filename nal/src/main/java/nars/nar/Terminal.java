package nars.nar;

import jcog.random.XORShiftRandom;
import nars.NAR;
import nars.conceptualize.DefaultConceptBuilder;
import nars.index.term.BasicTermIndex;
import nars.nar.exe.BufferedExecutioner;
import nars.time.CycleTime;
import nars.time.Time;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

/**
 * Terminal only executes commands and does not
 * reason.  however it will produce an event
 * stream which can be delegated to other
 * components like other NAR's
 */
@Deprecated public class Terminal extends NAR {

    public Terminal() {
        this(1024);
    }

    public Terminal(int capacity) {
        this(capacity, new XORShiftRandom(1), new CycleTime());
    }

    public Terminal(int capacity, @NotNull Random random, @NotNull Time time) {
        super(new BasicTermIndex(capacity), new BufferedExecutioner(capacity, capacity), time, random, new DefaultConceptBuilder());
    }

}
