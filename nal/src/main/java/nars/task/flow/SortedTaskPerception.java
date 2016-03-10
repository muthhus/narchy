package nars.task.flow;

import nars.NAR;
import nars.budget.BudgetMerge;
import nars.budget.ItemAccumulator;
import nars.task.Task;
import nars.util.data.MutableInteger;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/** sorts and de-duplicates incoming tasks into a capacity-limited buffer */
public class SortedTaskPerception extends TaskPerception {

    @NotNull
    final ItemAccumulator<Task> buffer;

    public final MutableInteger inputPerCycle = new MutableInteger();

    /**
     *
     * @param nar
     * @param capacity
     * @param inputPerCycle -1 for everything
     */
    public SortedTaskPerception(@NotNull NAR nar,
                                int capacity, int inputPerCycle, BudgetMerge merge) {
        super(nar, nar::input);

        this.inputPerCycle.set( inputPerCycle );

        //TODO use MutableInteger for capacity for all Bags
        buffer = new ItemAccumulator<>(capacity, merge);
    }

    @Override
    public final void accept(@NotNull Task t) {
        if (!t.isDeleted()) {
            Task overflow = buffer.bag().put(t).get();
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
    public final void nextFrame(@NotNull Consumer<Task[]> receiver) {
        ItemAccumulator<Task> buffer = this.buffer;
        int available = size();
        if (available > 0) {

            int inputsPerCyc = inputPerCycle.intValue();
            if (inputsPerCyc == -1) {
                //send everything
                inputsPerCyc = available;
            }

            buffer.bag().pop(
                t -> receiver.accept(new Task[] { t.get() }),
                Math.min(available, inputsPerCyc)
            );
        }
    }

    public final int size() {
        return buffer.bag().size();
    }

    @Override
    public void clear() {
        buffer.bag().clear();
    }
}
