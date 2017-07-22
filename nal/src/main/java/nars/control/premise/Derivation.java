package nars.control.premise;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.io.ByteArrayDataOutput;
import jcog.Util;
import jcog.byt.DynBytes;
import jcog.math.ByteShuffler;
import nars.*;
import nars.control.Premise;
import nars.derive.AbstractPred;
import nars.derive.PrediTerm;
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

import java.util.Arrays;

import static nars.Op.Null;
import static nars.Op.VAR_PATTERN;
import static nars.term.transform.substituteIfUnifies.substituteIfUnifiesAny;
import static nars.term.transform.substituteIfUnifies.substituteIfUnifiesDep;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.ETERNAL;


/**
 * evaluates a premise (task, belief, termlink, taskLink, ...) to derive 0 or more new tasks
 */
public class Derivation extends Unify implements TermContext {

    public static final PrediTerm<Derivation> NullDeriver = new AbstractPred<Derivation>(Op.ZeroProduct) {
        @Override
        public boolean test(Derivation o) {
            return true;
        }
    };
    @NotNull
    public final NAR nar;

    public float truthResolution;
    public float confMin;

    @Nullable
    public Truth concTruth;
    public byte concPunc;
    @Nullable
    public long[] concEvidence;


    /**
     * the current premise being evaluated in this context TODO make private again
     */
    @NotNull
    public Premise premise;


    public float premiseEvi;

    /**
     * current MatchTerm to receive matches at the end of the Termute chain; set prior to a complete match by the matchee
     */
    @Nullable
    private PrediTerm forEachMatch;

    /**
     * cached values ==========================================
     */

    /**
     * op ordinals: 0=task, 1=belief
     */
    public byte termSub0op;
    public byte termSub1op;

    /**
     * op bits: 0=task, 1=belief
     */
    public int termSub0opBit;
    public int termSub1opBit;

    /**
     * structs, 0=task, 1=belief
     */
    public int termSub0Struct;
    public int termSub1Struct;

    /**
     * current NAR time, set at beginning of derivation
     */
    public long time = ETERNAL;

    @Nullable
    public Truth taskTruth;
    @Nullable
    public Truth beliefTruth, beliefTruthRaw;

    @NotNull
    public Compound taskTerm;
    @NotNull
    public Term beliefTerm;

    @NotNull
    public Task task;
    @Nullable
    public Task belief;
    public byte taskPunct;

    /**
     * whether the premise involves temporality that must be calculated upon derivation
     */
    public boolean temporal;

    @Nullable
    private long[] evidenceDouble, evidenceSingle;

    public boolean cyclic, overlap;
    //public final float overlapAmount;

    private final Functor substituteIfUnifiesAny, substituteIfUnifiesDep, polarize;

    public float premisePri;
    public short[] parentCause;

    public PrediTerm<Derivation> deriver;
    public final ByteShuffler shuffler = new ByteShuffler(64);

    private transient Term[][] currentMatch = null;

    public /*static*/ final Cache<Transformation, Term> transformsCache; //works in static mode too
    /*static*/ {
    }

//    final MRUCache<Transformation, Term> transformsCache = new MRUCache<>(Param.DERIVATION_THREAD_TRANSFORM_CACHE_SIZE);

    /**
     * if using this, must set: nar, index, random, DerivationBudgeting
     */
    public Derivation(NAR nar) {
        super(nar.terms, VAR_PATTERN, nar.random(), Param.UnificationStackMax, 0);

        this.nar = nar;

        Caffeine cb = Caffeine.newBuilder().executor(nar.exe);
            //.executor(MoreExecutors.directExecutor());
        int cs = Param.DERIVATION_THREAD_TRANSFORM_CACHE_SIZE;
        if (cs == -1)
            cb.softValues();
        else
            cb.maximumSize(cs);
                    //.recordStats()
        transformsCache = cb.build();


        substituteIfUnifiesAny = new substituteIfUnifiesAny(this) {
            @Override
            public boolean equals(Object u) {
                return this == u;
            }
        };
        substituteIfUnifiesDep = new substituteIfUnifiesDep(this) {
            @Override
            public boolean equals(Object u) {
                return this == u;
            }
        };
        polarize = Functor.f2("polarize", (subterm, whichTask) -> {
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
                case "subIfUnifiesAny":
                    return substituteIfUnifiesAny;
                case "subIfUnifiesDep":
                    return substituteIfUnifiesDep;
                case "polarize":
                    return polarize;
            }
            return terms.get(x, createIfAbsent);
        }
        return x;
    }

    /**
     * concept-scope
     */
    @NotNull
    public Derivation cycle(PrediTerm<Derivation> deriver) {
        this.time = this.nar.time();
        this.dur = this.nar.dur();
        this.truthResolution = this.nar.truthResolution.floatValue();
        this.confMin = Math.max(truthResolution, this.nar.confMin.floatValue());
        this.deriver = deriver;
        //transformsCached.cleanUp();
        return this;
    }


//    @Override
//    public void onDeath() {
//        nar.emotion.derivationDeath.increment();
//    }

    /**
     * tasklink/termlink scope
     */
    @NotNull
    public void run(@NotNull Premise p, Task task, Task belief, Term beliefTerm, float premisePri, int ttl) {


        revert(0); //revert directly

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
        this.termSub0op = tOp.id;
        this.termSub0opBit = tOp.bit;

        this.concTruth = null;
        this.concPunc = 0;
        this.concEvidence = null;


        evidenceDouble = evidenceSingle = null;
        temporal = cyclic = overlap = false;

        assert (ttl >= 0);
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
        if (belief != null) {
            cyclic |= belief.cyclic();
            if (!overlap)
                overlap |= Stamp.overlapping(task, belief);
        }

        this.termSub1Struct = beliefTerm.structure();

        Op bOp = beliefTerm.op();
        this.termSub1op = bOp.id;
        this.termSub1opBit = bOp.bit;

        this.temporal = temporal(task, belief);

        float premiseEvidence = task.isBeliefOrGoal() ? taskTruth.evi() : 0;
        if (beliefTruth != null)
            premiseEvidence = (premiseEvidence + beliefTruth.evi());
        this.premiseEvi = premiseEvidence;

        this.premisePri = premisePri;

        short[] taskCause = task.cause();
        short[] beliefCause = belief != null ? belief.cause() : ArrayUtils.EMPTY_SHORT_ARRAY;

        //HACK
        if (taskCause.length > 0 && beliefCause.length > 0) {
            //HACK zip
            this.parentCause = new short[]{taskCause[0], beliefCause[0]};
        } else if (taskCause.length > 0) {
            this.parentCause = new short[]{taskCause[0]};
        } else if (beliefCause.length > 0) {
            this.parentCause = new short[]{beliefCause[0]};
        }

        deriver.test(this);


    }


    /**
     * set in Solve once these (3) conclusion parameters have been determined
     */
    public void truth(Truth truth, byte punc, long[] evidence) {
        this.concTruth = truth;
        this.concPunc = punc;
        this.concEvidence = evidence;
    }

    /**
     * only one thread should be in here at a time
     */
    public final boolean matchAll(@NotNull Term x, @NotNull Term y, @Nullable PrediTerm<Derivation> eachMatch) {

        boolean finish = (this.forEachMatch = eachMatch) != null;
//        if (!finish) {
//            //before the start
//        }

        try {
            unify(x, y, finish);
        } finally {
            this.forEachMatch = null;
        }
//        if (finish) {
//            //after the end
//        }

        return live();
    }

    @Override
    public final void onMatch(Term[][] match) {

        this.currentMatch = match;

        try {
            forEachMatch.test(this);
        } finally {
            this.currentMatch = null;
        }

    }


    private static boolean temporal(@NotNull Task task, @Nullable Task belief) {
        if (!task.isEternal() || (task.op().temporal && task.dt() != DTERNAL))
            return true;

        return (belief != null) &&
                (!belief.isEternal()
                        ||
                (belief.op().temporal && (belief.dt() != DTERNAL)));
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
            float te = tt != null ?
                    tt.conf() :  //for belief/goal use the task conf
                    task.priElseZero(); //for question/quest, use the task priority
            float be = belief.conf();
            evidenceDouble = Stamp.zip(task.stamp(), belief.stamp(), te / (te + be));
//            if ((task.cyclic() || belief.cyclic()) && Stamp.isCyclic(evidenceDouble))
//                throw new RuntimeException("cyclic should not be propagated");
            //System.out.println(Arrays.toString(task.evidence()) + " " + Arrays.toString(belief.evidence()) + " -> " + Arrays.toString(evidenceDouble));
        }

        return evidenceDouble;
    }

    @Override
    public String toString() {
        return task + " " + (belief != null ? belief : beliefTerm)
                + " " + super.toString();
    }

    public int ttl() {
        return ttl;
    }

    public void accept(DerivedTask t) {
        nar.input(t);
        nar.emotion.taskDerivations.increment();
    }

    /**
     * experimental memoization of transform results
     */
    @Nullable
    public Term transform(@NotNull Term pattern) {
        if (!(pattern instanceof Compound) || pattern.vars(type) == 0 || pattern.size() == 0) {
            //return super.transform(pattern);
            return super.transform(pattern); //xy.get(pattern); //fast variable resolution
        }
        if (pattern.OR(x -> x == Null))
            return Null;

        Transformation key = Transformation.the((Compound) pattern, currentMatch);

        //avoid recursive update problem on the single thread by splitting the get/put
        Term value = transformsCache.getIfPresent(key);
        if (value != null)
            return value;

        value = super.transform(key.pattern);
        if (value == null)
            value = Null;

        transformsCache.put(key, value);

        if (value == Null)
            value = null;

        return value;
    }

    final static class Transformation {
        public final Compound pattern;
        final byte[] assignments;
        final int hash;

        Transformation(Compound pattern, DynBytes assignments) {
            this.pattern = pattern;
            this.assignments = assignments.array();
            this.hash = Util.hashCombine(assignments.hashCode(), pattern.hashCode());
        }

        @Override
        public boolean equals(Object o) {
            //if (this == o) return true;
            Transformation tthat = (Transformation) o;
            if (hash != tthat.hash) return false;
            return pattern.equals(tthat.pattern) && Arrays.equals(assignments, tthat.assignments);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        static Transformation the(@NotNull Compound pattern, Term[][] match) {

            //TODO only include in the key the free variables in the pattern because there can be extra and this will cause multiple results that could have done the same thing
            //FasterList<Term> key = new FasterList<>(currentMatch.length * 2 + 1);

            DynBytes key = new DynBytes((2 * match.length + 1) * 8 /* estimate */);
            pattern.append((ByteArrayDataOutput) key); //in 0th
            key.writeByte(0);
            for (Term[] m : match) {
                Term var = m[0];
                if (pattern.containsRecursively(var)) {
                    var.append((ByteArrayDataOutput) key);
                    key.writeByte(0);
                    m[1].append((ByteArrayDataOutput) key);
                    key.writeByte(0);
                }
            }
            return new Transformation(pattern, key);
        }

    }


}


