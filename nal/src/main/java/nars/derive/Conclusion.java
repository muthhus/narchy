package nars.derive;

import nars.NAR;
import nars.Param;
import nars.control.Derivation;
import nars.term.Compound;
import nars.term.InvalidTermException;
import nars.term.Term;
import nars.term.transform.Retemporalize;
import nars.term.var.Variable;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

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
public final class Conclusion extends AbstractPred<Derivation> {


    private final static Logger logger = LoggerFactory.getLogger(Conclusion.class);
    public final Term pattern;
    private final boolean goalUrgent;


    public final Set<Variable> uniqueVars;

    public Conclusion(Term id, Term pattern, boolean goalUrgent) {
        super(id);
        this.pattern = pattern;
        this.uniqueVars = pattern instanceof Compound ? ((PatternCompound)pattern).uniqueVars : Set.of();
        this.goalUrgent = goalUrgent;
    }





    @Override
    public final boolean test(@NotNull Derivation p) {

        NAR nar = p.nar;

        p.use(Param.TTL_DERIVE_TRY);
        nar.emotion.derivationEval.increment();

        Term c1 = pattern.transform(p); //SUBSTITUTE and EVAL

        int volMax = nar.termVolumeMax.intValue();
        if (c1 == null || !c1.op().conceptualizable || c1.varPattern() > 0 || c1.volume() > volMax)
            return false;

        final long[] occ = p.derivedOcc;
        occ[0] = occ[1] = ETERNAL;

        final float[] confGain = {1f}; //flat by default

        Term c2;
        if (p.temporal) {

            try {

                DerivationTemporalize dt = p.temporalize;
                if (dt == null) {
                    p.temporalize = dt = new DerivationTemporalize(p); //cache in derivation
                }

                c2 = dt.solve(p, c1, occ, confGain);

            } catch (InvalidTermException t) {
                if (Param.DEBUG) {
                    logger.error("temporalize error: {} {} {}", p, c1, t.getMessage());
                }
                return false;
            }

            //invalid or impossible temporalization; could not determine temporal attributes. seems this can happen normally
            if (c2 == null || c2.volume() > volMax || !c2.op().conceptualizable/*|| (Math.abs(occReturn[0]) > 2047483628)*/ /* long cast here due to integer wraparound */) {
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

                if (taskStart == ETERNAL)
                    taskStart = p.time;

                //if (taskStart != ETERNAL) {
                if (occ[0] != ETERNAL && taskStart != ETERNAL && occ[0] < taskStart) {

                    long taskDur = occ[1] - occ[0];
                    occ[0] = taskStart;
                    occ[1] = occ[0] + taskDur;

                }
            }

        } else {
            c2 = c1.temporalize(Retemporalize.retemporalizeAllToDTERNAL);
            if (c2 == null)
                return false;
        }

        c2 = c2.normalize();
        if (c2 == null)
            return false;

        if (p.live()) {
            p.derivedTerm.set(c2);
            return true;
        } else {
            return false;
        }
    }


}
