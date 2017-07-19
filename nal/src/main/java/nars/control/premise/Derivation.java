package nars.control.premise;

import jcog.math.ByteShuffler;
import jcog.version.Versioned;
import nars.*;
import nars.control.Premise;
import nars.derive.meta.PrediTerm;
import nars.derive.rule.PremiseRule;
import nars.index.term.TermContext;
import nars.task.DerivedTask;
import nars.term.Compound;
import nars.term.Functor;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atom;
import nars.term.subst.Unify;
import nars.truth.Stamp;
import nars.truth.Truth;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.function.Predicate;

import static nars.Op.VAR_PATTERN;
import static nars.term.transform.substituteIfUnifies.substituteIfUnifiesAny;
import static nars.term.transform.substituteIfUnifies.substituteIfUnifiesDep;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.ETERNAL;


/**
 * evaluates a premise (task, belief, termlink, taskLink, ...) to derive 0 or more new tasks
 */
public class Derivation extends Unify implements TermContext {

    @NotNull public NAR nar;

    public float truthResolution;
    public float confMin;

    @Nullable public Truth concTruth;
    public byte concPunc;
    @Nullable public long[] concEvidence;



    /**
     * the current premise being evaluated in this context TODO make private again
     */
    @NotNull
    public Premise premise;


    public  float premiseEvi;

    /**
     * current MatchTerm to receive matches at the end of the Termute chain; set prior to a complete match by the matchee
     */
    @Nullable
    private PrediTerm forEachMatch;

    /**
     * cached values ==========================================
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

    /** current NAR time, set at beginning of derivation */
    public long time = ETERNAL;

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

    private final Functor substituteIfUnifiesAny, substituteIfUnifiesDep, polarize;

    public float parentPri;
    public short[] parentCause;

    public Predicate<Derivation> deriver;
    public ByteShuffler shuffler = new ByteShuffler(64);


    /** if using this, must set: nar, index, random, DerivationBudgeting */
    public Derivation() {
        super(null, VAR_PATTERN, null, Param.UnificationStackMax, 0);


        substituteIfUnifiesAny = new substituteIfUnifiesAny(this) {
            @Override public boolean equals(Object u) { return this == u; }
        };
        substituteIfUnifiesDep = new substituteIfUnifiesDep(this) {
            @Override public boolean equals(Object u) { return this == u; }
        };
        polarize = Functor.f2("polarize", (subterm, whichTask)->{
            Truth compared;
            if (whichTask.equals(PremiseRule.Task)) {
                compared = taskTruth;
            } else {
                compared = beliefTruth;
            }
            return compared.isNegative() ? $.neg(subterm) : subterm;
        });
    }




    @Override
    public Termed get(Term x, boolean createIfAbsent) {
        if (x instanceof Atom) {
            switch (x.toString()) {
                case "subIfUnifiesAny": return substituteIfUnifiesAny;
                case "subIfUnifiesDep": return substituteIfUnifiesDep;
                case "polarize": return polarize;
            }
            return terms.get(x, createIfAbsent);
        }
        return x;
    }

    /** concept-scope
     * @param n*/
    @NotNull public void restart(NAR n) {
        this.nar = n;
        this.terms = n.terms;
        this.random = n.random();
        this.time = n.time();
        this.dur = n.dur();
        this.truthResolution = nar.truthResolution.floatValue();
        this.confMin = Math.max(truthResolution, nar.confMin.floatValue());
        this.deriver = n.deriver();
    }

    /** tasklink/termlink scope */
    @NotNull public void run(@NotNull Premise p, Task task, Task belief, Term beliefTerm, float parentTaskPri, int ttl) {


        versioning.revert(0); //revert directly

        //remove common variable entries because they will just consume memory if retained as empty
        //xy.map.entrySet().removeIf(e -> e.getKey() instanceof CommonVariable);
        xy.map.clear();

        termutes.clear();

        forEachMatch = null;



        this.task = task;

        this.taskTruth = task.truth();
        this.taskPunct = task.punc();

        Compound tt = task.term();
        this.taskTerm = tt;
        this.termSub0Struct = tt.structure();
        Op tOp = tt.op();
        this.termSub0op = (byte) tOp.id;
        this.termSub0opBit = tOp.bit;

        this.concTruth = null;
        this.concPunc = 0;
        this.concEvidence = null;


        evidenceDouble = evidenceSingle = null;
        temporal = cyclic = overlap = false;

        assert(ttl >= 0);
        this.setTTL(ttl);

        this.premise = p;

        this.belief = belief;

        //assert(beliefTerm.op()!=NEG): beliefTerm + " is negated";
        this.beliefTerm = beliefTerm.unneg();


        if (belief == null) {
            this.beliefTruth = this.beliefTruthRaw = null;
        } else {
            Truth beliefTruth = this.beliefTruthRaw = belief.truth();

            /** to compute the time-discounted truth, find the minimum distance
             *  of the tasks considering their dtRange
             */
            long tstart = task.start();
            if ((tstart != ETERNAL)) {
                long bstart = belief.start();
                if (bstart != ETERNAL) {

                    long beliefTruthTime = belief.nearestStartOrEnd(tstart, task.end());
                    beliefTruth = belief.truth(beliefTruthTime, dur, nar.confMin.floatValue() /* confMin */); //project belief truth to task's time
                }
            }
            this.beliefTruth = beliefTruth;
        }


        this.overlap = this.cyclic = task.cyclic(); //belief cyclic should not be considered because in single derivation its evidence will not be used any way
        if (belief!=null) {
            cyclic |= belief.cyclic();
            if (!overlap)
                overlap |= Stamp.overlapping(task, belief);
        }

        this.termSub1Struct = beliefTerm.structure();

        Op bOp = beliefTerm.op();
        this.termSub1op = (byte) bOp.id;
        this.termSub1opBit = bOp.bit;

        this.temporal = temporal(task, belief);

        float premiseEvidence = task.isBeliefOrGoal() ? taskTruth.evi() : 0;
        if (beliefTruth!=null)
            premiseEvidence = (premiseEvidence + beliefTruth.evi());
        this.premiseEvi = premiseEvidence;

        this.parentPri = parentTaskPri;

        short[] taskCause = task.cause();
        short[] beliefCause = belief!=null ?  belief.cause() : ArrayUtils.EMPTY_SHORT_ARRAY;

        //HACK
        if (taskCause.length > 0 && beliefCause.length > 0) {
            //HACK zip
            this.parentCause = new short[] { taskCause[0], beliefCause[0] };
        } else if (taskCause.length > 0) {
            this.parentCause = new short[] { taskCause[0] };
        } else if (beliefCause.length > 0) {
            this.parentCause = new short[] { beliefCause[0] };
        }

        deriver.test(this);

    }


  /** set in Solve once these (3) conclusion parameters have been determined */
    public void truth(Truth truth, byte punc, long[] evidence) {
        this.concTruth = truth;
        this.concPunc = punc;
        this.concEvidence = evidence;
    }

    /**
     * only one thread should be in here at a time
     */
    public final boolean matchAll(@NotNull Term x, @NotNull Compound y, @Nullable PrediTerm eachMatch) {

        boolean finish = (this.forEachMatch = eachMatch)!=null;

        unify(x, y, finish);

        this.forEachMatch = null;

        return live();
    }

    @Override public final void onMatch() {
        //try {


        forEachMatch.test(this);
//        } catch (InvalidTermException | InvalidTaskException t) {
//            if (Param.DEBUG_EXTRA) {
//                logger.error("Derivation onMatch {}", t);
//            }
//        }
        //return  (--matchesRemain > 0) && ;
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
            Truth tt = task.truth();
            float te = tt!=null ?
                    tt.conf() :  //for belief/goal use the task conf
                    task.priElseZero(); //for question/quest, use the task priority
            float be = belief.conf();
            evidenceDouble = Stamp.zip(task.stamp(), belief.stamp(), te/(te+be));
//            if ((task.cyclic() || belief.cyclic()) && Stamp.isCyclic(evidenceDouble))
//                throw new RuntimeException("cyclic should not be propagated");
            //System.out.println(Arrays.toString(task.evidence()) + " " + Arrays.toString(belief.evidence()) + " -> " + Arrays.toString(evidenceDouble));
        }

        return evidenceDouble;
    }

    @Override
    public String toString() {
        return task + " " + (belief!=null ? belief : beliefTerm)
                + " " + super.toString();
    }

    public int ttl() {
        return ttl;
    }

    /** forms a new cause by appending a cause ID to the derivation's cause */
    public short[] cause(short c) {
        return ArrayUtils.add(this.parentCause, c);
    }

    public void accept(DerivedTask t) {
        nar.input(t);
        nar.emotion.taskDerivations.increment();
    }
}


