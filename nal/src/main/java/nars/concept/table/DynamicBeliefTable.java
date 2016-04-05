package nars.concept.table;

import com.google.common.collect.Iterators;
import nars.NAR;
import nars.concept.Concept;
import nars.task.Task;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;

/**
 * Created by me on 4/4/16.
 */
abstract public class DynamicBeliefTable implements BeliefTable {

    private final Concept concept;
    private final NAR nar;
    @Nullable
    Task current, next;

    public DynamicBeliefTable(Concept concept, NAR nar) {
        this.concept = concept;
        this.nar = nar;
    }

    @Nullable
    @Override
    public Task add(@NotNull Task input, NAR nar) {
        if (input == next || current == null) {
            current = input;
            return input; //first time processing the calculated new one
        } else if (input == current) {
            return null;  //duplicate
        } else {
            //TODO discrepency detector
            return null;
        }

        //return updateTask(nar.time());
    }

    @Nullable
    @Override
    public Task topEternal() {
        return null;
    }

    @Nullable
    @Override
    public Task topTemporal(long when, long now) {
        return updateTask(now);
    }

    @Nullable
    public Task updateTask(long now) {
        if (current == null || current.occurrence() != now) {
            Task prev = current;
            next = update(now);
            if (prev==null || ( !prev.truth().equals(next.truth()))) {
                if (prev!=null)
                    prev.delete();
                nar.process(next);
            }
        }
        return current;
    }

    abstract protected Task update(long now);

    @Nullable
    @Override
    public Truth truth(long when, long now, float dur) {
        return topTemporal(when, now).projectTruth(when, now, false);
    }

    @Override
    public int getCapacity() {
        return 0;
    }

    @Override
    public void setCapacity(int newCapacity) {

    }

    @Override
    public int size() {
        return isEmpty() ? 0 : 1;
    }

    @Override
    public void clear() {
        current = null;
    }

    @Override
    public boolean isEmpty() {
        return current==null;
    }

    @NotNull
    @Override
    public Iterator<Task> iterator() {
        return !isEmpty() ? Iterators.singletonIterator(current) : Iterators.emptyIterator();
    }
}
