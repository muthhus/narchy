package nars.concept.table;

import com.google.common.collect.Iterators;
import nars.NAR;
import nars.Task;
import nars.concept.CompoundConcept;
import nars.concept.TruthDelta;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static nars.nal.Tense.ETERNAL;

/**
 * Created by me on 4/4/16.
 */
abstract public class DynamicBeliefTable implements BeliefTable {

    protected @Nullable Task current;
    //boolean changed = true;

    @NotNull
    abstract public NAR nar();

    @Override
    public void capacity(int eternals, int temporals, List<Task> removed, long now) {
        //ignored
    }

    @Override
    public TruthDelta add(@NotNull Task input, @NotNull QuestionTable questions, List<Task> displaced, CompoundConcept<?> concept, @NotNull NAR nar) {
        return null;
    }

    @Nullable
    @Override
    public Task eternalTop() {
        return null;
    }

    @Nullable
    @Override
    public Task topTemporal(long when, long now, Task against) {
        updateTask(now);
        return current;
    }

    public void updateTask(long now) {
        ///if (changed) { //
        if (current == null || current.occurrence() != now) {
            Task prev = current;
            Task next = update(now);
            if (next!=null && (prev==null || (
                    !prev.truth().equals(next.truth() ) ||
                    !Arrays.equals(prev.evidence(), next.evidence())
            ))) {
                this.current = next;
                @NotNull NAR nar = nar();
                if (prev!=null)
                    prev.delete(nar);
                nar.input(next);
            }
            //changed = false;
        }

    }

    @Nullable
    abstract protected Task update(long now);

    @Nullable
    public Task get(Task t) {
        return current.equals(t) ? current : null;
    }

    @Nullable
    @Override
    public Truth truth(long when, long now) {
        if (now == ETERNAL)
            throw new UnsupportedOperationException("soon");

        @Nullable Task x = topTemporal(when, now, null);
        return x != null ? x.projectTruth(when, now, false) : null;
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
    public boolean isEmpty() {
        return current==null;
    }

    @NotNull
    @Override
    public Iterator<Task> iterator() {
        return !isEmpty() ? Iterators.singletonIterator(current) : Collections.emptyIterator();
    }

//    public void changed() {
//        changed = true;
//    }
}
