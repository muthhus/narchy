package nars.table;

import com.google.common.collect.Iterators;
import nars.NAR;
import nars.Task;
import nars.concept.CompoundConcept;
import nars.truth.Truth;
import nars.truth.TruthDelta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.function.Consumer;

import static nars.time.Tense.ETERNAL;


/**
 * Stores beliefs ranked in a sorted ArrayList, with strongest beliefs at lowest indexes (first iterated)
 */
public class DefaultBeliefTable implements BeliefTable {

    @NotNull
    public final EternalTable eternal;
    @NotNull
    public final TemporalBeliefTable temporal;

    public DefaultBeliefTable(EternalTable eternal, TemporalBeliefTable temporal) {

        /* Ranking by originality is a metric used to conserve original information in balance with confidence */
        this.eternal = eternal;
        this.temporal = temporal;
    }


    /**
     * TODO this value can be cached per cycle (when,now) etc
     */
    @Nullable
    @Override
    public Truth truth(long when, long now) {

        Truth tt = temporal.truth(when, now, eternal);

        return (tt != null) ? tt : eternal.truth();

    }

    @Override
    public void clear(@NotNull NAR nar) {
        if (!eternal.isEmpty())
            throw new UnsupportedOperationException("eternal clear impl soon");

        temporal.clear(nar);
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
        Consumer<Task> totaler = t -> total[0] += t.priSafe(0);
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
    @Deprecated public int capacity() {
        //throw new UnsupportedOperationException("doesnt make sense to call this");
        return eternal.capacity() + temporal.capacity();
    }

    @Override
    public final void capacity(int eternals, int temporals, NAR nar) {
        temporal.capacity(temporals, nar);
        eternal.capacity(eternals, nar);
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




    /**
     * get the most relevant belief/goal with respect to a specific time.
     */
    @Nullable
    public Task match(long when, long now, @Nullable Task against, boolean noOverlap) {

        final Task ete = eternal.match();
        if (when == ETERNAL) {
            if (ete != null) {
                return ete;
            }
        }

        if (now!=ETERNAL) {

            Task tmp = temporal.match(when, now, against);

            if (tmp == null) {
                return ete;
            } else {
                if (ete == null) {
                    return tmp;
                } else {
                    return (ete.confWeight(when) > tmp.confWeight(when)) ?
                            ete : tmp;
                }
            }
        }

        return null;

    }


    @Override
    public TruthDelta add(@NotNull Task input, @NotNull QuestionTable questions, @NotNull CompoundConcept<?> concept, @NotNull NAR nar) {
        return input.isEternal() ?  eternal.add(input, concept, nar) : temporal.add(input, eternal, concept, nar);
    }

//    @NotNull
//    private EternalTable nonEmptyEternal(@NotNull CompoundConcept<?> concept, @NotNull Task input) {
//        if (eternal == EternalTable.EMPTY) {
//            eternal = new EternalTable(concept.policy().beliefCap(concept, input.isBelief(), true));
//        }
//        return eternal;
//    }


//    private void eternalizeForgottenTemporals(@NotNull List<Task> displaced, @NotNull NAR nar, float factor) {
//        float confMin = nar.confMin.floatValue();
//
//        @NotNull EternalTable eternal = this.eternal;
//
//        float minRank = eternal.isFull() ? eternal.rank(eternal.weakest()) : 0;
//
//        int displacedSize = displaced.size();
//
//        //should use indexed list access because adding eternal might add new eternal tasks at the end (which should not be processed here
//        for (int i = 0; i < displacedSize; i++) {
//            Task d = displaced.get(i);
//
//            assert (d.occurrence() != ETERNAL);
//
//            if (!d.isDeleted()) {
//                float eConf = TruthFunctions.eternalize(d.conf()) * factor;
//                if (eConf > confMin) {
//                    if (eternal.rank(eConf, d.evidence().length) > minRank) {
//
//                        Task ee = new EternalizedTask(
//                                d.term(), d.punc(),
//                                $.t(d.freq(), eConf)
//                        )
//                                .time(nar.time(), ETERNAL)
//                                .evidence(d)
//                                .budget(Budget.Zero)
//                                .log("Eternalized");
//
//                        nar.inputLater(ee);
//                    }
//
//                }
//            }
//        }
//    }


}



