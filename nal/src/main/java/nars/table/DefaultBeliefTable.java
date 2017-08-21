package nars.table;

import com.google.common.collect.Iterators;
import nars.NAR;
import nars.Task;
import nars.concept.BaseConcept;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static nars.time.Tense.ETERNAL;


/**
 * Stores beliefs ranked in a sorted ArrayList, with strongest beliefs at lowest indexes (first iterated)
 */
public class DefaultBeliefTable implements BeliefTable {

    @Nullable public final EternalTable eternal;

    @NotNull public final TemporalBeliefTable temporal;

    public DefaultBeliefTable(TemporalBeliefTable t) {
        super();
        eternal = new EternalTable(0);
        temporal = t;
    }

    @Override
    public Stream<Task> stream() {
        return Stream.concat(eternal.stream(), temporal.stream()).filter(x -> !x.isDeleted());
    }

    /**
     * TODO this value can be cached per cycle (when,now) etc
     */
    @Override
    public Truth truth(long start, long end, NAR nar) {
        return temporal.truth(start, end, eternal, nar);
    }

    @Override
    public boolean removeTask(Task x) {
        return (x.isEternal()) ? eternal.removeTask(x) : temporal.removeTask(x);
    }

    @Override
    public void clear() {
        temporal.clear();
        eternal.clear();
    }

    @NotNull
    @Override
    @Deprecated
    public final Iterator<Task> iterator() {
        return Iterators.concat(
                eternal.iterator(),
                temporal.taskIterator()
        );
    }

    @Override
    public void forEachTask(boolean includeEternal, long minT, long maxT, Consumer<? super Task> x) {
        if (includeEternal) {
            eternal.forEachTask(x);
        }
        temporal.forEach(minT, maxT, x);
    }

    @Override
    public void forEach(@NotNull Consumer<? super Task> action) {
        forEachTask(action);
    }

    @Override
    public final void forEachTask(Consumer<? super Task> action) {
        eternal.forEachTask(action);
        temporal.forEachTask(action);
    }

    @Override
    public float priSum() {
        final float[] total = {0};
        Consumer<Task> totaler = t -> total[0] += t.priSafe(0);
        eternal.forEachTask(totaler);
        temporal.forEachTask(totaler);
        return total[0];
    }

    @Override
    public int size() {
        return eternal.size() /* eternal */ + temporal.size();
    }

    @Override
    @Deprecated
    public int capacity() {
        //throw new UnsupportedOperationException("doesnt make sense to call this");
        return eternal.capacity() /* eternal */ + temporal.capacity();
    }

    @Override
    public final void setCapacity(int eternals, int temporals) {
        temporal.setCapacity(temporals);
        eternal.setCapacity(eternals);
    }

    /**
     * get the most relevant belief/goal with respect to a specific time.
     */
    @Override
    public Task match(long start, long end, Term template, boolean noOverlap, NAR nar) {

        final Task ete = eternal.strongest();
        if (ete != null && start == ETERNAL) {
            return ete;
        }

        if (start == ETERNAL) {
            int dur = nar.dur();
            start = nar.time() - dur/2;
            end = nar.time() + dur/2;
        }

        Task tmp = temporal.match(start, end, template, nar);

        if (tmp == null) {
            return ete;
        } else {
            if (ete == null) {
                return tmp;
            } else {
                return (ete.evi() > tmp.evi(tmp.nearestTimeBetween(start,end), nar.dur())) ?
                        ete : tmp;
            }
        }


        //return null;

    }


    @Override
    public void add(@NotNull Task input, @NotNull BaseConcept concept, @NotNull NAR nar) {
        if (input.isEternal()) {

            eternal.add(input, concept, nar);

        } else {

            temporal.add(input, concept, nar);
        }
    }


}



