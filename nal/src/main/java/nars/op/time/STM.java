package nars.op.time;

import nars.NAR;
import nars.Task;
import nars.util.data.MutableInteger;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Short-term Memory Temporal Structure Formation
 */
public abstract class STM implements Consumer<Task> {
    public final @NotNull NAR nar;

    public final MutableInteger capacity;
    protected boolean allowNonInput;

    public STM(@NotNull NAR nar, MutableInteger capacity) {
        this.nar = nar;
        this.capacity = capacity;
    }

    /** call this in constructor */
    protected void start() {
        nar.eventTaskProcess.on(t -> {
            if (temporallyInductable(t, allowNonInput))
                accept(t);
        } );
        nar.eventReset.on(n -> clear());
    }

    protected void stop() {
        //TODO
    }

    abstract public void clear();

    static public boolean temporallyInductable(@NotNull Task newEvent, boolean allowNonInput) {
        return (!newEvent.isDeleted() && (allowNonInput || newEvent.isInput()) && !newEvent.isEternal());
    }


    @Override
    public abstract void accept(@NotNull Task t);
}
