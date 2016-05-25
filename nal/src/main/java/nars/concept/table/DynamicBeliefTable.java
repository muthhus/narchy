package nars.concept.table;

import com.google.common.collect.Iterators;
import nars.NAR;
import nars.task.Task;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Iterator;

/**
 * Created by me on 4/4/16.
 */
abstract public class DynamicBeliefTable implements BeliefTable {

    private final NAR nar;
    @Nullable
    Task current;

    public DynamicBeliefTable(NAR nar) {
        //this.concept = concept;
        this.nar = nar;
    }

    @Override
    public void capacity(int eternals, int temporals) {
        //ignored
    }

    @Override
    public Task add(@NotNull Task input, QuestionTable questions, NAR nar) {
        if (input == current) {
            return input;
        } else {
            //TODO trigger update?
            //TODO measure discrepency?
            return null; //exclude other beliefs/goals
        }
    }

    @Nullable
    @Override
    public Task topEternal() {
        return null;
    }

    @Nullable
    @Override
    public Task topTemporal(long when, long now) {
        return current;
    }

    @Nullable
    public void updateTask(long now) {
        if (current == null || current.occurrence() != now) {
            Task prev = current;
            Task next = update(now);
            if (next!=null && (prev==null || (
                    !prev.truth().equals(next.truth() ) ||
                    !Arrays.equals(prev.evidence(), next.evidence())
            ))) {
                this.current = next;
                if (prev!=null)
                    prev.delete();
                nar.process(next);
            }
        }

    }

    @Nullable
    abstract protected Task update(long now);

    @Override public Task get(Task t) {
        return current.equals(t) ? current : null;
    }

    @Override
    public Truth truth(long now, long when) {
        return topTemporal(when, now).projectTruth(when, now, false);
    }

    @Override
    public int capacity() {
        return 0;
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
