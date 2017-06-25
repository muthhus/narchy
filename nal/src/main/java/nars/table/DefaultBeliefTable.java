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


/**
 * Stores beliefs ranked in a sorted ArrayList, with strongest beliefs at lowest indexes (first iterated)
 */
public class DefaultBeliefTable extends EternalTable implements BeliefTable {

//    @Nullable   public EternalTable eternal = this;

    @NotNull public final TemporalBeliefTable temporal;

    public DefaultBeliefTable(TemporalBeliefTable t) {
        super(0);
        temporal = t;
    }

    @Override
    public void setCapacity(int c) {
        throw new RuntimeException("only super.setCapacity should be called by this instance");
    }

    /**
     * TODO this value can be cached per cycle (when,now) etc
     */
    @Override
    public Truth truth(long when, long now, int dur, NAR nar) {

        Truth tt = temporal().truth(when, now, dur, this);

        return (tt != null) ? tt : super.truth();

    }

    @Override
    public boolean removeTask(Task x) {
        return (x.isEternal()) ? super.removeTask(x) : temporal().removeTask(x);
    }

    @Override
    public void clear() {
        temporal().clear();
        super.clear();
    }

    @NotNull
    @Override
    @Deprecated
    public final Iterator<Task> iterator() {
        return Iterators.concat(
                super.taskIterator(),
                temporal().taskIterator()
        );
    }

    @Override
    public void forEach(@NotNull Consumer<? super Task> action) {
        forEachTask(action);
    }

    @Override
    public final void forEachTask(Consumer<? super Task> action) {
        super.forEachTask(action);
        temporal().forEachTask(action);
    }

    @Override
    public float priSum() {
        final float[] total = {0};
        Consumer<Task> totaler = t -> total[0] += t.priSafe(0);
        super.forEachTask(totaler);
        temporal().forEachTask(totaler);
        return total[0];
    }

    @Override
    public int size() {
        return super.size() + temporal().size();
    }

    @Override
    @Deprecated
    public int capacity() {
        //throw new UnsupportedOperationException("doesnt make sense to call this");
        return super.capacity() + temporal().capacity();
    }

    @Override
    public final void capacity(int eternals, int temporals) {
        temporal.setCapacity(temporals);
        super.setCapacity(eternals);
    }

    public EternalTable eternal() {
        return this;
    }

    public TemporalBeliefTable temporal() {
        return temporal;
    }

    /**
     * get the most relevant belief/goal with respect to a specific time.
     */
    @Override
    public Task match(long when, long now, int dur, @Nullable Task against, Compound template, boolean noOverlap, NAR nar) {

        final Task ete = super.strongest();
//        if (ete != null && when == ETERNAL) {
//            return ete;
//        }

        Task tmp = temporal().match(when, now, dur, against, nar);

        if (tmp == null) {
            return ete;
        } else {
            if (ete == null) {
                return tmp;
            } else {
                return (ete.evi() > tmp.evi(when, dur)) ?
                        ete : tmp;
            }
        }


        //return null;

    }


    @Override
    public void add(@NotNull Task input, @NotNull TaskConcept concept, @NotNull NAR nar) {
        if (input.isEternal()) {

            super.add(input, concept, nar);

        } else {

            temporal.add(input, concept, nar);
        }
    }


}



