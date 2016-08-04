package nars.concept.table;

import com.google.common.collect.Iterators;
import nars.$;
import nars.NAR;
import nars.task.EternalizedTask;
import nars.task.Task;
import nars.truth.Truth;
import nars.truth.TruthFunctions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import static nars.nal.Tense.ETERNAL;


/**
 * Stores beliefs ranked in a sorted ArrayList, with strongest beliefs at lowest indexes (first iterated)
 */
public class DefaultBeliefTable implements BeliefTable {

    @NotNull public final EternalTable eternal;
    @NotNull public final TemporalBeliefTable temporal;



    public DefaultBeliefTable(int initialEternalCapacity, int initialTemporalCapacity) {

        /* Ranking by originality is a metric used to conserve original information in balance with confidence */
        eternal = new EternalTable(initialEternalCapacity);
        temporal = newTemporalBeliefTable(initialTemporalCapacity);
    }

    @NotNull
    protected static TemporalBeliefTable newTemporalBeliefTable(int initialTemporalCapacity) {
        return new MicrosphereTemporalBeliefTable(initialTemporalCapacity);
    }

    /** TODO this value can be cached per cycle (when,now) etc */
    @Override
    public final Truth truth(long when, long now) {

        final Truth tt;

            tt = temporal.truth(when, now, eternal);


        if (tt!=null) {
            return tt;
        } else {
            return eternal.truth();
        }

    }


    @NotNull
    @Override
    @Deprecated public final Iterator<Task> iterator() {
        return Iterators.concat(
            eternal.iterator(),
            temporal.iterator()
        );
    }

    @Override
    public void forEach(@NotNull Consumer<? super Task> action) {
        eternal.forEach(action);
        synchronized(temporal) {
            temporal.forEach(action);
        }
    }

    @Override
    public int size() {
        return eternal.size() + temporal.size();
    }

    @Override public boolean isEmpty() {
        return size()==0;
    }

    @Override
    public int capacity() {
        return eternal.capacity() + temporal.capacity();
    }

    @Override
    public final void capacity(int eternals, int temporals, List<Task> displ, long now) {
        eternal.capacity(eternals, displ);
        synchronized (temporal) {
            temporal.capacity(temporals, now, displ);
        }
    }

    @Override
    public void range(long[] t) {
        temporal.range(t);
    }

    //    @Override
//    public void remove(@NotNull Task belief, List<Task> displ) {
//        ((belief.isEternal()) ? eternal : temporal).remove(belief, displ);
//    }

    /** calculates the max confidence of a belief within the given frequency range */
    public float confMax(float minFreq, float maxFreq) {
        float max = 0;

        //HACK eternal top task may not hold the highest confidence (since rank involves originality) however we'll use that value here
        Task eternalMax = eternalTop();
        if (eternalMax!=null) {
            float f = eternalMax.freq();
            if ((f >= minFreq) && (f <= maxFreq)) {
                max = eternalMax.conf();
            }
        }

        synchronized(temporal) {
            List<Task> temporals = ((MicrosphereTemporalBeliefTable) temporal);
            for (int i = 0, temporalsSize = temporals.size(); i < temporalsSize; i++) {
                Task t = temporals.get(i);
                if (t != null) {
                    float f = t.freq();
                    if ((f >= minFreq) && (f <= maxFreq)) {
                        float c = t.conf();
                        if (c > max)
                            max = c;
                    }
                }
            }
        }

        return max;
    }


    @Nullable
    @Override
    public final Task eternalTop() {
        return eternal.strongest();
    }

    @Override
    public final Task topTemporal(long when, long now, Task against) {
        TemporalBeliefTable tt = temporal;
        if (!tt.isEmpty()) {
            synchronized (temporal) {
                return tt.strongest(when, now, against);
            }
        }
        return null;
    }


    @Override public Task add(@NotNull Task input, @NotNull QuestionTable questions, List<Task> displaced, @NotNull NAR nar) {


        //Filter duplicates; return null if duplicate
        // (no link activation will propagate and TaskProcess event will not be triggered)
        Task result;
        if (input.isEternal()) {

            result = eternal.add(input, displaced, nar);

        } else {
            synchronized (temporal) {

                result = temporal.add(input, eternal, displaced, nar);

            }
        }

        if (result!=null) {
            questions.answer(result, nar, displaced);
        }

        return result;
    }

    private void eternalizeForgottenTemporals(List<Task> displaced, @NotNull NAR nar, float factor) {
        float confMin = nar.confMin.floatValue();

        @NotNull EternalTable eternal = this.eternal;

        float minRank = eternal.isFull() ? eternal.minRank() : 0;

        int displacedSize = displaced.size();

        //should use indexed list access because adding eternal might add new eternal tasks at the end (which should not be processed here
        for (int i = 0; i < displacedSize; i++) {
            Task d = displaced.get(i);

            assert(d.occurrence()!=ETERNAL);

            if (!d.isDeleted()) {
                float eConf = TruthFunctions.eternalize(d.conf()) * factor;
                if (eConf > confMin) {
                    if (eternal.rank(eConf, d.evidence().length) > minRank) {

                        Task ee = new EternalizedTask(
                                d.term(), d.punc(),
                                $.t(d.freq(), eConf)
                            )
                                .time(nar.time(), ETERNAL)
                                .evidence(d)
                                .budget(d.budget())
                                .log("Eternalized");

                        nar.inputLater(ee);
                    }

                }
            }
        }
    }


}



