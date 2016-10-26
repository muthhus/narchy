package nars.op.time;

import nars.NAR;
import nars.Task;
import nars.util.data.MutableInteger;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Created by me on 9/16/16.
 */
abstract public class STM /*extends BagBuffer<Task>*/ implements Consumer<Task> {

    @NotNull
    public final NAR nar;
    boolean allowNonInput;
    public final MutableInteger capacity;


    public STM(@NotNull NAR nar, MutableInteger capacity) {
        super();
        this.nar = nar;
        this.capacity = capacity;
        nar.eventTaskProcess.on(t -> {
            if (temporallyInductable(t, allowNonInput))
                accept(t);
        });
        nar.eventReset.on(n -> clear());

    }

    static public boolean temporallyInductable(@NotNull Task newEvent, boolean allowNonInput) {
        return ( (allowNonInput || newEvent.isInput()) && newEvent.isBeliefOrGoal() && !newEvent.isEternal());
    }

    abstract public void clear();

    @Override
    public abstract void accept(@NotNull Task t);

}
