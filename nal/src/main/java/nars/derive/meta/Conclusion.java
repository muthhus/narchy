package nars.derive.meta;

import nars.NAR;
import nars.Param;
import nars.budget.Budget;
import nars.budget.RawBudget;
import nars.derive.rule.PremiseRule;
import nars.premise.Premise;
import nars.task.DerivedTask;
import nars.term.Compound;
import nars.term.util.InvalidTermException;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

import static nars.term.Terms.compoundOrNull;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.ETERNAL;
import static nars.time.Tense.XTERNAL;

/** the final part of a derivation, which can lazily produce a Derived Task */
public class Conclusion extends RawBudget {

    @NotNull public final Compound term;

    public final PremiseRule rule;

    //TODO store punctuation, truth, and evidence as a separae component so that
    //  they can be used to construct the Task independently of any time function or other term processing which they can all share
    public final char punc;
    @Nullable public final Truth truth;
    private final long[] evidence;

    public Conclusion(Compound term, char punc, Truth truth, Budget budget, long[] evidence, @NotNull PremiseRule rule) {
        super(budget);


        this.term = term;
        this.punc = punc;
        this.truth = truth;
        this.evidence = evidence;

        this.rule = rule;
    }

    /**
     * 2nd-stage
     */
    public final DerivedTask derive(@NotNull Premise p, NAR nar) {

        Compound content = term;


//this is performed on input also
//        if (!Task.taskContentValid(content, ct.punc, nar, false/* !Param.DEBUG*/))
//            return; //INVALID TERM FOR TASK

        long[] occ;


        if (p.temporal) {
//            if (nar.level() < 7)
//                throw new NAR.InvalidTaskException(content, "invalid NAL level");

            long[] occReturn = {ETERNAL, ETERNAL};
            float[] confScale = {1f};

            Compound temporalized = rule.time.compute(content,
                    p, this, occReturn, confScale, nar
            );

            //temporalization failure, could not determine temporal attributes. seems this can happen normally
            if ((temporalized == null) /*|| (Math.abs(occReturn[0]) > 2047483628)*/ /* long cast here due to integer wraparound */) {
//                 {
//                    //FOR DEBUGGING, re-run it
//                    Compound temporalized2 = this.time.compute(content,
//                            m, this, occReturn, confScale
//                    );
//                }

                throw new InvalidTermException(content.op(), content.dt(), "temporalization failure" + (Param.DEBUG ? rule : ""), content.terms()
                );
            }

            int tdt = temporalized.dt();
            if (tdt == XTERNAL || tdt == -XTERNAL) {
                throw new InvalidTermException(content.op(), content.dt(), "XTERNAL/DTERNAL leak", content.terms());
            }

//            if (Param.DEBUG && occReturn[0] != ETERNAL && Math.abs(occReturn[0] - DTERNAL) < 1000) {
//                //temporalizer.compute(content.term(), m, this, occReturn, confScale); //leave this commented for debugging
//                throw new NAR.InvalidTaskException(content, "temporalization resulted in suspicious occurrence time");
//            }

            if (temporalized != content) {
                ((content = temporalized)).setNormalized();
            }


            //apply any non 1.0 the confidence scale
            if (truth != null) {

                float cf = confScale[0];
                if (cf != 1) {
                    throw new UnsupportedOperationException("yet");
//                    truth = truth.confMultViaWeightMaxEternal(cf);
//                    if (truth == null) {
//                        throw new InvalidTaskException(content, "temporal leak");
//                    }
                }
            }

            occ = occReturn;

        } else {

            occ = null;
        }


        //the derived compound indicated a potential dt, but the premise was actually atemporal;
        // this indicates a temporal placeholder (XTERNAL) in the rules which needs to be set to DTERNAL
        if (content.dt() == XTERNAL /*&& !o.isImage()*/) {

            content = compoundOrNull(nar.concepts.the(content, DTERNAL)); //necessary to trigger flattening as a result of the atemporalization
            if (content == null)
                return null;

            content = compoundOrNull(nar.concepts.normalize(content));
            if (content == null)
                return null;
        }

        Truth truth = this.truth;

        if (truth != null) {
            float eFactor = nar.evidenceFactor.asFloat();
            if (eFactor != 1) {
                truth = truth.confWeightMult(eFactor);
                if (truth == null)
                    return null; //excessive doubt
            }
        }

        content = nar.pre(content);
        if (content.volume() > nar.termVolumeMax.intValue())
            return null;

        return derive(content, this, nar.time(), occ, truth, evidence, punc, rule, p);
    }


    /**
     * part 2
     */
    @Nullable
    protected final static DerivedTask derive(@NotNull Compound c, @NotNull Budget budget, long now, long[] occ, Truth truth, long[] evidence, char punc, PremiseRule rule, Premise p) {


        DerivedTask d =
                new DerivedTask.DefaultDerivedTask(c, truth, punc, evidence, p, now, occ);


        //new RuleFeedbackDerivedTask(c, truth, punc, evidence, p, rule);
        d.budget(budget) // copied in, not shared
                //.anticipate(derivedTemporal && d.anticipate)
                .log(Param.DEBUG ? rule : null);


//            //TEMPORARY MEASUREMENT
//            if (dt.isGoal()) {
//               synchronized (posGoal) {
//                   ((dt.freq() >= 0.5f) ? posGoal : negGoal).addOccurrences(rule, (int)(Math.abs(dt.freq()-0.5f)*100));
//               }
//            }
//            //</TEMPORARY MEASUREMENT

        return d;


        //ETERNALIZE: (CURRENTLY DISABLED)

//        if ((occ != ETERNAL) && (truth != null) && d.eternalize  ) {


//            if (!derived.isDeleted()) {
//
//
//                nar.process(newDerivedTask(c, punct, new DefaultTruth(truth.freq(), eternalize(truth.conf())), parents)
//                        .time(now, ETERNAL)
//                        .budgetCompoundForward(budget, this)
//                        /*
//                TaskBudgeting.compoundForward(
//                        budget, truth(),
//                        term(), premise);*/
//                        .log("Immediaternalized") //Immediate Eternalization
//
//                );
//            }

//        }

    }


    @Override
    public String toString() {
        return "Conclusion{" +
                "term=" + term +
                ", punc=" + punc +
                ", evi=" + evidence +
                ", truth=" + truth +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Conclusion)) return false;

        Conclusion that = (Conclusion) o;

        if (!Arrays.equals(evidence, that.evidence)) return false;
        if (punc != that.punc) return false;
        if (!term.equals(that.term)) return false;
        return truth != null ? truth.equals(that.truth) : that.truth == null;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + term.hashCode();
        result = 31 * result + Arrays.hashCode(evidence);
        result = 31 * result + (int) punc;
        result = 31 * result + (truth != null ? truth.hashCode() : 0);
        return result;
    }
}
