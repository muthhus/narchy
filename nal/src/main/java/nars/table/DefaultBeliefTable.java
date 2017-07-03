package nars.table;

import com.google.common.collect.Iterators;
import nars.NAR;
import nars.Task;
import nars.concept.TaskConcept;
import nars.term.Compound;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.function.Consumer;

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


    /**
     * TODO this value can be cached per cycle (when,now) etc
     */
    @Override
    public Truth truth(long when, NAR nar) {
        return temporal.truth(when, eternal, nar);
    }

    @Override
    public boolean removeTask(Task x) {
        return (x.isEternal()) ? eternal.removeTask(x) : temporal().removeTask(x);
    }

    @Override
    public void clear() {
        temporal().clear();
        eternal.clear();
    }

    @NotNull
    @Override
    @Deprecated
    public final Iterator<Task> iterator() {
        return Iterators.concat(
                eternal.iterator(),
                temporal().taskIterator()
        );
    }

    @Override
    public void forEach(@NotNull Consumer<? super Task> action) {
        forEachTask(action);
    }

    @Override
    public final void forEachTask(Consumer<? super Task> action) {
        eternal.forEachTask(action);
        temporal().forEachTask(action);
    }

    @Override
    public float priSum() {
        final float[] total = {0};
        Consumer<Task> totaler = t -> total[0] += t.priSafe(0);
        eternal.forEachTask(totaler);
        temporal().forEachTask(totaler);
        return total[0];
    }

    @Override
    public int size() {
        return eternal.size() /* eternal */ + temporal().size();
    }

    @Override
    @Deprecated
    public int capacity() {
        //throw new UnsupportedOperationException("doesnt make sense to call this");
        return eternal.capacity() /* eternal */ + temporal().capacity();
    }

    @Override
    public final void setCapacity(int eternals, int temporals) {
        temporal.setCapacity(temporals);
        eternal.setCapacity(eternals);
    }

    public EternalTable eternal() {
        return eternal;
    }

    public TemporalBeliefTable temporal() {
        return temporal;
    }

    /**
     * get the most relevant belief/goal with respect to a specific time.
     */
    @Override
    public Task match(long when, @Nullable Task against, Compound template, boolean noOverlap, NAR nar) {

        final Task ete = eternal.strongest();
        if (ete != null && when == ETERNAL) {
            return ete;
        }

        Task tmp = temporal().match(when, against, nar);

        if (tmp == null) {
            return ete;
        } else {
            if (ete == null) {
                return tmp;
            } else {
                return (ete.evi() > tmp.evi(when, nar.dur())) ?
                        ete : tmp;
            }
        }


        //return null;

    }


    @Override
    public void add(@NotNull Task input, @NotNull TaskConcept concept, @NotNull NAR nar) {
        if (input.isEternal()) {

            eternal.add(input, concept, nar);

        } else {

            temporal.add(input, concept, nar);
        }
    }


}



