package nars.premise;

import jcog.version.Versioned;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.derive.meta.BoolPred;
import nars.task.DerivedTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.subst.Unify;
import nars.term.transform.substitute;
import nars.truth.Stamp;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

import static nars.Op.NEG;
import static nars.Op.VAR_PATTERN;
import static nars.term.transform.substituteIfUnifies.substituteIfUnifiesAny;
import static nars.term.transform.substituteIfUnifies.substituteIfUnifiesDep;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.ETERNAL;


/**
 * evaluates a premise (task, belief, termlink, taskLink, ...) to derive 0 or more new tasks
 */
abstract public class Derivation extends Unify {

    @NotNull
    public final NAR nar;
    public final float truthResolution;
    public final float confMin;
    public final DerivationBudgeting budgeting;
    @NotNull public final Versioned<TruthPuncEvidence> punct;

    /**
     * the current premise being evaluated in this context TODO make private again
     */
    @NotNull
    public  Premise premise;


    public  float premiseEvidence;

    /**
     * current MatchTerm to receive matches at the end of the Termute chain; set prior to a complete match by the matchee
     */
    @Nullable
    private BoolPred forEachMatch;

    /**
     * cached values
     */
    /** op ordinals: 0=task, 1=belief */
    public  byte termSub0op;
    public  byte termSub1op;

    /** op bits: 0=task, 1=belief */
    public  int termSub0opBit;
    public  int termSub1opBit;

    /** structs, 0=task, 1=belief */
    public  int termSub0Struct;
    public  int termSub1Struct;


    @Nullable
    public  Truth taskTruth;
    @Nullable
    public  Truth beliefTruth, beliefTruthRaw;

    @NotNull
    public  Compound taskTerm;
    @NotNull
    public  Term beliefTerm;

    @NotNull
    public  Task task;
    @Nullable
    public  Task belief;
    public  byte taskPunct;

    /**
     * whether the premise involves temporality that must be calculated upon derivation
     */
    public  boolean temporal;

    @Nullable
    private long[] evidenceDouble, evidenceSingle;


    public boolean cyclic, overlap;
    //public final float overlapAmount;



    public Derivation(@NotNull NAR nar,
                      DerivationBudgeting b,
                      int stack, int ttl) {
        super(nar.concepts, VAR_PATTERN, nar.random(), stack, ttl);
        this.nar = nar;
        this.budgeting = b;
        this.truthResolution = nar.truthResolution.floatValue();
        this.confMin = Math.max(truthResolution, nar.confMin.floatValue());
        this.punct = new Versioned<>(versioning, 1);

        set(new substitute(this));
        set(new substituteIfUnifiesAny(this));
        set(new substituteIfUnifiesDep(this));

    }

    public Derivation restart(@NotNull Premise p, int ttl) {

        this.versioning.setTTL(ttl);
        forEachMatch = null;
        termutes.clear(); //assert(termutes.isEmpty()); //should already have been cleared:


        this.premise = p;

        Task task;
        this.task = task = p.task;
        Compound tt = task.term();
        Term taskTerm = this.taskTerm = tt;
        this.taskTruth = task.truth();
        this.taskPunct = task.punc();

        Task belief;
        this.belief = belief = p.belief;
        this.beliefTerm = p.beliefTerm();
        if (beliefTerm.op()==NEG) {
            throw new RuntimeException("negated belief term");
        }


        int dur = nar.dur();
        if (belief == null) {
            this.beliefTruth = this.beliefTruthRaw = null;
        } else {
            Truth beliefTruth = this.beliefTruthRaw = belief.truth();
            long start = task.start();
            if ((start != ETERNAL) && (!belief.isEternal())) {
                beliefTruth = belief.truth(start, dur, confMin); //project belief truth to task's time
            }
            this.beliefTruth = beliefTruth;
        }


        this.cyclic = task.cyclic(); //belief cyclic should not be considered because in single derivation its evidence will not be used any way
        //NOT: this.cyclic = task.cyclic() || (belief != null && belief.cyclic());

        this.overlap = belief != null ? Stamp.overlapping(task, belief) : cyclic;
        //this.overlapAmount = belief!=null ? Stamp.overlapFraction(task.stamp(), belief.stamp()) : (cyclic? 1f : 0f);


        Op tOp = taskTerm.op();
        this.termSub0op = (byte) tOp.ordinal();
        this.termSub0opBit = tOp.bit;
        this.termSub0Struct = taskTerm.structure();
        this.termSub1Struct = beliefTerm.structure();

        Op bOp = beliefTerm.op();
        this.termSub1op = (byte) bOp.ordinal();
        this.termSub1opBit = bOp.bit;

        this.temporal = temporal(task, belief);

        float premiseEvidence = task.isBeliefOrGoal() ? task.evi() : 0;
        if (belief!=null)
            premiseEvidence = Math.max(premiseEvidence, belief.evi());
        this.premiseEvidence = premiseEvidence;

        return this;
    }

    /** called by conclusion */
    abstract public void derive(Task t);

    /**
     * only one thread should be in here at a time
     */
    public final boolean matchAll(@NotNull Term x, @NotNull Term y, @Nullable BoolPred eachMatch) {

        boolean finish = (this.forEachMatch = eachMatch)!=null;

        boolean result = unify(x, y, !finish, finish);

        this.forEachMatch = null;

        return result;
    }

    @Override public final boolean onMatch() {
        forEachMatch.test(this);
        //return  (--matchesRemain > 0) && ;
        return true;
    }


    private static boolean temporal(@NotNull Task task, @Nullable Task belief) {
        if (!task.isEternal() || (task.op().temporal && task.dt() != DTERNAL))
            return true;

        return (belief != null) &&
                        (!belief.isEternal()
                            ||
                        (belief.op().temporal && (belief.dt() != DTERNAL)));
    }







    /**
     * gets the op of the (top-level) pattern being compared
     *
     * @param subterm 0 or 1, indicating task or belief
     */
    /*public final boolean subTermIs(int subterm, int op) {
        return (subterm==0 ? termSub0op : termSub1op) == op;
    }*/
    public final int subOp(int i /* 0 or 1 */) {
        return (i == 0 ? termSub0op : termSub1op);
    }




    @Nullable
    public long[] evidenceSingle() {
        if (evidenceSingle == null) {
            evidenceSingle = Stamp.cyclic(task.stamp());
        }
        return evidenceSingle;
    }

    @Nullable
    public long[] evidenceDouble() {
        if (evidenceDouble == null) {
            evidenceDouble = Stamp.zip(task.stamp(), belief.stamp());
//            if ((task.cyclic() || belief.cyclic()) && Stamp.isCyclic(evidenceDouble))
//                throw new RuntimeException("cyclic should not be propagated");
            //System.out.println(Arrays.toString(task.evidence()) + " " + Arrays.toString(belief.evidence()) + " -> " + Arrays.toString(evidenceDouble));
        }

        return evidenceDouble;
    }

}


