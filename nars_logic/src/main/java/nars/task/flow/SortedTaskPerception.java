package nars.task.flow;

import nars.NAR;
import nars.budget.TaskAccumulator;
import nars.task.Task;
import nars.util.data.MutableInteger;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/** sorts and de-duplicates incoming tasks into a capacity-limited buffer */
public class SortedTaskPerception extends TaskPerception {

    @NotNull
    final TaskAccumulator buffer;

    public final MutableInteger inputPerCycle = new MutableInteger();

    /**
     *
     * @param nar
     * @param capacity
     * @param inputPerCycle -1 for everything
     */
    public SortedTaskPerception(@NotNull NAR nar,
                                int capacity, int inputPerCycle) {
        super(nar.memory);

        this.inputPerCycle.set( inputPerCycle );

        //TODO use MutableInteger for capacity for all Bags
        buffer = new TaskAccumulator(capacity);
    }

    @Override
    public final void accept(@NotNull Task t) {
        if (!t.isDeleted()) {
            Task overflow = buffer.getArrayBag().put(t).get();
            if (overflow!=null)
                onOverflow(overflow);
        }
    }

    protected void onOverflow(Task t) {

    }

//    @Override
//    public void forEach(@NotNull Consumer<? super Task> each) {
//        buffer.getArrayBag().forEach(e -> each.accept(e.get()));
//    }

    @Override
    public final void nextFrame(@NotNull Consumer<Task> receiver) {
        TaskAccumulator buffer = this.buffer;
        int available = size();
        if (available > 0) {

            int inputsPerCyc = inputPerCycle.intValue();
            if (inputsPerCyc == -1) {
                //send everything
                inputsPerCyc = available;
            }

            buffer.getArrayBag().pop(
                t -> receiver.accept(t.get()),
                Math.min(available, inputsPerCyc)
            );
        }
    }

    public final int size() {
        return buffer.getArrayBag().size();
    }

    @Override
    public void clear() {
        buffer.getArrayBag().clear();
    }
}
