package nars.table;

import com.google.common.collect.Iterators;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.budget.Budget;
import nars.concept.CompoundConcept;
import nars.task.EternalizedTask;
import nars.truth.Truth;
import nars.truth.TruthDelta;
import nars.truth.TruthFunctions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import static nars.time.Tense.ETERNAL;


/**
 * Stores beliefs ranked in a sorted ArrayList, with strongest beliefs at lowest indexes (first iterated)
 */
public class DefaultBeliefTable implements BeliefTable {

    @NotNull
    public EternalTable eternal;
    @NotNull
    public final TemporalBeliefTable temporal;

    public DefaultBeliefTable(int initialTemporalCapacity) {

        /* Ranking by originality is a metric used to conserve original information in balance with confidence */
        eternal = EternalTable.EMPTY;
        //new EternalTable(initialEternalCapacity);
        temporal = newTemporalBeliefTable(initialTemporalCapacity);
    }

    @NotNull
    protected static TemporalBeliefTable newTemporalBeliefTable(int initialTemporalCapacity) {
        return new MicrosphereTemporalBeliefTable(initialTemporalCapacity);
    }

    /**
     * TODO this value can be cached per cycle (when,now) etc
     */
    @Override
    public Truth truth(long when, long now) {

        final Truth tt;

        tt = temporal.truth(when, now, eternal);

        if (tt != null) {
            return tt;
        } else {
            return eternal.truth();
        }

    }

    @Override
    public void clear(NAR nar) {
        if (!eternal.isEmpty())
            throw new UnsupportedOperationException("eternal clear impl soon");

        if (!temporal.isEmpty()) {
            List<Task> l = $.newArrayList();
            temporal.removeIf((Task x)->true,l);
            nar.tasks.remove(l);
        }
    }

    @NotNull
    @Override
    @Deprecated
    public final Iterator<Task> iterator() {
        return Iterators.concat(
                eternal.iterator(),
                temporal.iterator()
        );
    }

    @Override
    public void forEach(@NotNull Consumer<? super Task> action) {
        eternal.forEach(action);
        temporal.forEach(action);
    }

    @Override
    public float priSum() {
        final float[] total = {0};
        Consumer<Task> totaler = t -> total[0] += t.priIfFiniteElseZero();
        eternal.forEach(totaler);
        temporal.forEach(totaler);
        return total[0];
    }

    @Override
    public int size() {
        return eternal.size() + temporal.size();
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public int capacity() {
        return eternal.capacity() + temporal.capacity();
    }

    @Override
    public final void capacity(int eternals, int temporals, @NotNull List<Task> removed, long now) {
        eternal.capacity(eternals, removed);
        temporal.capacity(temporals, now, removed);
    }

//    @Override
//    public void range(long[] t) {
//        temporal.range(t);
//    }

    //    @Override
//    public void remove(@NotNull Task belief, List<Task> displ) {
//        ((belief.isEternal()) ? eternal : temporal).remove(belief, displ);
//    }

//    /**
//     * calculates the max confidence of a belief within the given frequency range
//     */
//    public float confMax(float minFreq, float maxFreq) {
//        float max = 0;
//
//        //HACK eternal top task may not hold the highest confidence (since rank involves originality) however we'll use that value here
//        Task eternalMax = eternalTop();
//        if (eternalMax != null) {
//            float f = eternalMax.freq();
//            if ((f >= minFreq) && (f <= maxFreq)) {
//                max = eternalMax.conf();
//            }
//        }
//
//        List<Task> temporals = ((MicrosphereTemporalBeliefTable) temporal).list;
//        for (int i = 0, temporalsSize = temporals.size(); i < temporalsSize; i++) {
//            Task t = temporals.get(i);
//            if (t != null) {
//                float f = t.freq();
//                if ((f >= minFreq) && (f <= maxFreq)) {
//                    float c = t.conf();
//                    if (c > max)
//                        max = c;
//                }
//            }
//        }
//
//
//        return max;
//    }


    @Nullable
    @Override
    public final Task eternalTop() {
        return eternal.strongest();
    }

    @Override
    public final Task topTemporal(long when, long now, Task against) {
        return temporal.match(when, now, against);
    }


    @Override
    public TruthDelta add(@NotNull Task input, @NotNull QuestionTable questions, @NotNull List<Task> displaced, CompoundConcept<?> concept, @NotNull NAR nar) {

        TruthDelta result;
        if (input.isEternal()) {
            result = nonEmptyEternal(concept, input).add(input, displaced, concept, nar);
        } else {
            result = temporal.add(input, eternal, displaced, concept, nar);
            if (result != null && !displaced.isEmpty() && Param.eternalizeForgottenTemporal(input.op())) {
                eternalizeForgottenTemporals(displaced, nar, Param.ETERNALIZATION_CONFIDENCE_FACTOR);
            }
        }

        if (result != null) {
            questions.answer(input, concept, nar, displaced);
        }

        return result;
    }

    private EternalTable nonEmptyEternal(CompoundConcept<?> concept, @NotNull Task input) {
        if (eternal == EternalTable.EMPTY) {
            eternal = new EternalTable(concept.policy().beliefCap(concept, input.isBelief(), true));
        }
        return eternal;
    }


    private void eternalizeForgottenTemporals(@NotNull List<Task> displaced, @NotNull NAR nar, float factor) {
        float confMin = nar.confMin.floatValue();

        @NotNull EternalTable eternal = this.eternal;

        float minRank = eternal.isFull() ? eternal.rank(eternal.weakest()) : 0;

        int displacedSize = displaced.size();

        //should use indexed list access because adding eternal might add new eternal tasks at the end (which should not be processed here
        for (int i = 0; i < displacedSize; i++) {
            Task d = displaced.get(i);

            assert (d.occurrence() != ETERNAL);

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
                                .budget(Budget.Zero)
                                .log("Eternalized");

                        nar.inputLater(ee);
                    }

                }
            }
        }
    }


}



