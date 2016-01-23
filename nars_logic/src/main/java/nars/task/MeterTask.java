package nars.task;

import nars.Memory;
import nars.term.Termed;
import nars.term.compound.Compound;
import nars.util.event.Active;
import org.jetbrains.annotations.NotNull;

/**
 * Task which is specifically for collecting statistics about
 * its budget dynamics across time and reacting to
 * lifecycle events which are empty stubs in its
 * super-classes
 *
 */
public abstract class MeterTask extends MutableTask {

    private final Active active = new Active();

    public MeterTask(Termed<Compound> c) {
        super(c);
    }

    @Override
    protected void onNormalized(@NotNull Memory memory) {
        active.add(
                memory.eventFrameStart.on((n) -> onFrame(memory))
        );
    }

    abstract void onFrame(Memory memory);

}
