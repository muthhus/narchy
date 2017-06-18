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
import java.util.Random;
import java.util.function.Consumer;

import static nars.time.Tense.ETERNAL;


/**
 * Stores beliefs ranked in a sorted ArrayList, with strongest beliefs at lowest indexes (first iterated)
 */
public class DefaultBeliefTable implements BeliefTable {

    @Nullable
    public EternalTable eternal = null;
    @Nullable
    public TemporalBeliefTable temporal = null;


    /**
     * TODO this value can be cached per cycle (when,now) etc
     */
    @Nullable
    @Override
    public Truth truth(long when, long now, int dur) {

        Truth tt = temporal().truth(when, now, dur, eternal);

        return (tt != null) ? tt : eternal().truth();

    }

    @Override
    public boolean removeTask(Task x) {
        return (x.isEternal()) ? eternal().removeTask(x) : temporal().removeTask(x);
    }

    @Override
    public void clear() {
        temporal().clear();
        eternal().clear();
    }

    @NotNull
    @Override
    @Deprecated
    public final Iterator<Task> iterator() {
        return Iterators.concat(
                eternal().taskIterator(),
                temporal().taskIterator()
        );
    }

    @Override
    public void forEach(@NotNull Consumer<? super Task> action) {
        eternal().forEachTask(action);
        temporal().forEachTask(action);
    }

    @Override
    public final void forEachTask(Consumer<? super Task> x) {
        forEach(x);
    }

    @Override
    public float priSum() {
        final float[] total = {0};
        Consumer<Task> totaler = t -> total[0] += t.priSafe(0);
        eternal().forEachTask(totaler);
        temporal().forEachTask(totaler);
        return total[0];
    }

    @Override
    public int size() {
        return eternal().size() + temporal().size();
    }

    @Override
    @Deprecated
    public int capacity() {
        //throw new UnsupportedOperationException("doesnt make sense to call this");
        return eternal().capacity() + temporal().capacity();
    }

    @Override
    public final void capacity(int eternals, int temporals) {
        temporal().setCapacity(temporals);
        eternal().setCapacity(eternals);
    }

    public EternalTable eternal() {
        @Nullable EternalTable e = eternal;
        if (e == null) return EternalTable.EMPTY;
        return e;
    }

    public TemporalBeliefTable temporal() {
        @Nullable TemporalBeliefTable t = temporal;
        if (t == null) return TemporalBeliefTable.EMPTY;
        return t;
    }

    /**
     * get the most relevant belief/goal with respect to a specific time.
     */
    @Override
    public Task match(long when, long now, int dur, @Nullable Task against, Compound template, boolean noOverlap, Random rng) {

        final Task ete = eternal().strongest();
//        if (ete != null && when == ETERNAL) {
//            return ete;
//        }

        Task tmp = temporal().match(when, now, dur, against, rng);

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

            if (eternal == null) {
                //synchronized (concept) {
                    /*if (eternal == EternalTable.EMPTY)*/
                {
                    boolean isBeliefOrGoal = input.isBelief();
                    int cap = concept.state().beliefCap(concept, isBeliefOrGoal, true);
                    if (cap > 0)
                        eternal = concept.newEternalTable(cap, isBeliefOrGoal); //allocate
                    else
                        return;
                }
                //}
            }

            eternal.add(input, concept, nar);

        } else {
            if (temporal == null) {
                //synchronized (concept) {
                    /*if (temporal == TemporalBeliefTable.EMPTY)*/
                { //HACK double boiler
                    int cap = concept.state().beliefCap(concept, input.isBelief(), false);
                    if (cap > 0)
                        temporal = concept.newTemporalTable(cap, nar); //allocate
                    else
                        return;
                }
                //}
            }

            temporal.add(input, concept, nar);
        }
    }


}



