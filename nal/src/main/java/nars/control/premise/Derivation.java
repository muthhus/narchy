package nars.control.premise;

import jcog.math.Interval;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.control.Premise;
import nars.derive.meta.BoolPred;
import nars.index.term.TermContext;
import nars.task.DerivedTask;
import nars.task.TruthPolation;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atom;
import nars.term.subst.Unify;
import nars.term.transform.substitute;
import nars.term.util.InvalidTermException;
import nars.term.var.CommonVariable;
import nars.time.TimeFunctions;
import nars.truth.Stamp;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.Op.NEG;
import static nars.Op.VAR_PATTERN;
import static nars.term.transform.substituteIfUnifies.substituteIfUnifiesAny;
import static nars.term.transform.substituteIfUnifies.substituteIfUnifiesDep;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.ETERNAL;


/**
 * evaluates a premise (task, belief, termlink, taskLink, ...) to derive 0 or more new tasks
 */
abstract public class Derivation extends Unify implements TermContext {

    @NotNull public NAR nar;
    @NotNull public DerivationBudgeting budgeting;
    public float truthResolution;
    public float confMin;

    @Nullable public Truth concTruth = null;
    public byte concPunc = 0;
    @Nullable public long[] concEvidence = null;



    /**
     * the current premise being evaluated in this context TODO make private again
     */
    @NotNull
    public Premise premise;


    public  float premiseEvidence;

    /**
     * current MatchTerm to receive matches at the end of the Termute chain; set prior to a complete match by the matchee
     */
    @Nullable
    private BoolPred forEachMatch;

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
    public int dur = -1;

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

    private substitute _substitute;
    private substituteIfUnifiesAny _substituteIfUnifiesAny;
    private substituteIfUnifiesDep _substituteIfUnifiesDep;



    /** if using this, must set: nar, index, random, DerivationBudgeting */
    public Derivation() {
        super(null, VAR_PATTERN, null, Param.UnificationStackMax, 0);

        _substitute = new substitute(this) {
            @Override public boolean equals(Object u) { return this == u; }
        };
        _substituteIfUnifiesAny = new substituteIfUnifiesAny(this) {
            @Override public boolean equals(Object u) { return this == u; }
        };
        _substituteIfUnifiesDep = new substituteIfUnifiesDep(this) {
            @Override public boolean equals(Object u) { return this == u; }
        };
    }


    @Override
    public Termed get(Term x, boolean createIfAbsent) {
        if (x instanceof Atom) {
            switch (x.toString()) {
                case "substitute": return _substitute;
                case "subIfUnifiesAny": return _substituteIfUnifiesAny;
                case "subIfUnifiesDep": return _substituteIfUnifiesDep;
            }
        }
        return terms.get(x, createIfAbsent);
    }

    @Override
    public Term the(@NotNull Op op, int dt, Term[] subs) {
        return terms.the(op, dt, subs);
    }


    /** concept-scope
     * @param n*/
    @NotNull public void restartA(NAR n) {
        this.nar = n;
        this.terms = n.terms;
        this.budgeting = n.budgeting;
        this.random = n.random();
        this.time = nar.time();
        this.dur = nar.dur();
        this.truthResolution = nar.truthResolution.floatValue();
        this.confMin = Math.max(truthResolution, nar.confMin.floatValue());
    }

    /** tasklink scope */
    @NotNull public void restartB(@NotNull Task task) {

        this.task = task;

        this.taskTruth = task.truth();
        this.taskPunct = task.punc();

        Compound tt = task.term();
        this.taskTerm = tt;
        this.termSub0Struct = tt.structure();
        Op tOp = tt.op();
        this.termSub0op = (byte) tOp.ordinal();
        this.termSub0opBit = tOp.bit;

    }


    /** termlink scope */
    @NotNull public Derivation restartC(@NotNull Premise p, Task belief, Term beliefTerm, int ttl) {

        assert(ttl >= 0);

        revert(0);

        //remove common variable entries because they will just consume memory if retained as empty
        xy.map.entrySet().removeIf(e -> {
            if (e.getKey() instanceof CommonVariable)
                return true;
            else
                return false;
        });

        this.concTruth = null;
        this.concPunc = 0;
        this.concEvidence = null;

        forEachMatch = null;
        termutes.clear(); //assert(termutes.isEmpty()); //should already have been cleared:

        evidenceDouble = evidenceSingle = null;
        temporal = cyclic = overlap = false;

        this.versioning.setTTL(ttl);

        this.premise = p;

        this.belief = belief;
        this.beliefTerm = beliefTerm;
        assert(beliefTerm.op()!=NEG);


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
                    long tend = task.end();
                    long bend = belief.end();
                    long btime;
                    if (tstart <= bstart) btime = tstart;
                    else if (tend >= bend) btime = tend;
                    else {
                      //if ((bend >= tend) && (bstart <= tstart))
                          btime = belief.mid();
                      //else if (Math.abs(bend - tstart))
                        //TODO
                    }
                    if (!belief.contains(btime)) {
                        beliefTruth = belief.truth(btime, dur, confMin); //project belief truth to task's time
                    }
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
    abstract public void derive(DerivedTask t);

  /** set in Solve once these (3) conclusion parameters have been determined */
    public void truth(Truth truth, byte punc, long[] evidence) {
        this.concTruth = truth;
        this.concPunc = punc;
        this.concEvidence = evidence;
    }

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
        try {
            forEachMatch.test(this);
        } catch (InvalidTermException t) {
            if (Param.DEBUG) {
                logger.error("{}", t.getMessage());
            }
        }
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


