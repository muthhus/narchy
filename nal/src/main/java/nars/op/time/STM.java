package nars.op.time;

import nars.NAR;
import nars.Symbols;
import nars.task.Task;
import nars.util.data.MutableInteger;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Short-term Memory Temporal Structure Formation
 */
public abstract class STM implements Consumer<Task> {
    protected final @NotNull NAR nar;

    public final MutableInteger capacity;

    public STM(@NotNull NAR nar, MutableInteger capacity) {
        this.nar = nar;
        this.capacity = capacity;
    }

    /** call this in constructor */
    protected final void start() {
        nar.eventTaskProcess.on(t -> { if (temporallyInductable(t)) accept(t); } );
        nar.eventReset.on(n -> clear());
    }

    protected void stop() {
        //TODO
    }

    abstract public void clear();

    static public boolean temporallyInductable(@NotNull Task newEvent) {
        return (!newEvent.isDeleted() && newEvent.isInput() && !newEvent.isEternal());
    }


    @Override
    public abstract void accept(@NotNull Task t);
}
