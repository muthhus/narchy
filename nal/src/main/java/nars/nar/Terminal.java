package nars.nar;

import jcog.random.XORShiftRandom;
import nars.NAR;
import nars.time.FrameTime;
import nars.time.Time;
import nars.util.exe.SynchronousExecutor;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

/**
 * Terminal only executes commands and does not
 * reason.  however it will produce an event
 * stream which can be delegated to other
 * components like other NAR's
 */
public class Terminal extends NAR {

    //final Predicate<Task> taskFilter = Task::isCommand;

    public Terminal() {
        this(1024);
    }

    public Terminal(int capacity) {
        this(capacity, new XORShiftRandom(1), new FrameTime());
    }

    public Terminal(int capacity, @NotNull Random random, @NotNull Time time) {
        super(time, new Default.DefaultTermTermIndex(capacity), random, new SynchronousExecutor());
    }

}
