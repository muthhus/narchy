package nars.derive;

import nars.NAR;
import nars.Param;
import nars.Task;
import nars.control.Derivation;
import nars.task.NALTask;
import nars.term.InvalidTermException;
import nars.term.Term;
import nars.time.Tense;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiFunction;

import static nars.Op.GOAL;
import static nars.time.Tense.ETERNAL;

/**
 * Final conclusion step of the derivation process that produces a derived task
 * <p>
 * Each rule corresponds to a unique instance of this
 * <p>
 * runtime instance. each leaf of each NAR's derivation tree should have
 * a unique instance, assigned the appropriate cause id by the NAR
 * at initialization.
 */
public class Conclusion extends AbstractPred<Derivation> {


    private final static Logger logger = LoggerFactory.getLogger(Conclusion.class);
    private final Term pattern;
    private final boolean goalUrgent;

    public Conclusion(Term id, Term pattern, boolean goalUrgent) {
        super(id);
        this.pattern = pattern;
        this.goalUrgent = goalUrgent;
    }

    /**
     * @return true to allow the matcher to continue matching,
     * false to stop it
     */
    @Override
    public final boolean test(@NotNull Derivation p) {

        NAR nar = p.nar;


        p.use(Param.TTL_DERIVE_TRY);
        nar.emotion.derivationEval.increment();

        Term c1 = pattern.transform(p); //SUBSTITUTE and EVAL

        int volMax = nar.termVolumeMax.intValue();
        if (c1 == null || !c1.op().conceptualizable || c1.varPattern() > 0 || c1.volume() > volMax)
            return false;

        @NotNull final long[] occ;
        final float[] confGain = {1f}; //flat by default

        Term c2;
        if (p.temporal) {

            Term t1;
            try {
                occ = new long[]{ETERNAL, ETERNAL};
                t1 = solveTime(p, c1, occ, confGain);


//                if (t1!=null && occ[0] == 7) {
//                    //FOR A SPECIFIC TEST TEMPORAR
//                    System.err.println("wtf");
//                    solveTime(d, c1, occ, eviGain);
//                }
            } catch (InvalidTermException t) {
                if (Param.DEBUG) {
                    logger.error("temporalize error: {} {} {}", p, c1, t.getMessage());
                }
                return false;
            }

            //invalid or impossible temporalization; could not determine temporal attributes. seems this can happen normally
            if (t1 == null || t1.volume() > volMax || !t1.op().conceptualizable/*|| (Math.abs(occReturn[0]) > 2047483628)*/ /* long cast here due to integer wraparound */) {
//                            throw new InvalidTermException(c1.op(), c1.dt(), "temporalization failure"
//                                    //+ (Param.DEBUG ? rule : ""), c1.toArray()
//                            );

                //FOR DEBUGGING
//                if (t1==null)
//                    new Temporalize(d.random).solve(d, c1, new long[]{ETERNAL, ETERNAL});

                return false;
            }

            if (occ[1] == ETERNAL) occ[1] = occ[0]; //HACK probbly isnt needed

            if (goalUrgent && p.concPunc == GOAL) {
                long taskStart = p.task.start();

                if (p.temporal && taskStart == ETERNAL)
                    taskStart = p.time;

                //if (taskStart != ETERNAL) {
                if (occ[0] != ETERNAL && taskStart != ETERNAL && occ[0] < taskStart) {

                    long taskDur = occ[1] - occ[0];
                    occ[0] = taskStart;
                    occ[1] = occ[0] + taskDur;

                }
            }

            c2 = t1;

        } else {
            occ = Tense.ETERNAL_RANGE;
            c2 = c1;
        }

        c2 = c2.normalize();
        if (c2 == null)
            return false;


        p.derivedTerm.set(c2);
        p.derivedOcc = occ;
        return p.live();
    }

    final static BiFunction<Task, Task, Task> DUPLICATE_DERIVATION_MERGE = (pp, tt) -> {
        pp.priMax(tt.pri());
        ((NALTask) pp).causeMerge(tt);
        return pp;
    };

    @Nullable
    private static Term solveTime(@NotNull Derivation d, Term c1, @NotNull long[] occ, float[] confGain) {
        DerivationTemporalize dt = d.temporalize;
        if (dt == null) {
            d.temporalize = dt = new DerivationTemporalize(d); //cache in derivation
        }
//        dt = new DerivationTemporalize(d);
        return dt.solve(d, c1, occ, confGain);
    }


}
