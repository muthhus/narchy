package nars.concept.table;

import com.google.common.collect.Iterators;
import nars.NAR;
import nars.concept.JunctionConcept;
import nars.task.Task;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;

/**
 * Created by me on 4/4/16.
 */
abstract public class DynamicBeliefTable implements BeliefTable {

    private final JunctionConcept junctionConcept;
    @Nullable
    Task current;

    public DynamicBeliefTable(JunctionConcept junctionConcept) {
        this.junctionConcept = junctionConcept;

    }

    @Nullable
    @Override
    public Task add(@NotNull Task input, NAR nar) {
        return current;
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
            current = update(now);
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
        return updateTask(junctionConcept.nar.time()) == null;
    }

    @NotNull
    @Override
    public Iterator<Task> iterator() {
        return !isEmpty() ? Iterators.singletonIterator(current) : Iterators.emptyIterator();
    }
}
