package nars.control;

import jcog.math.ByteShuffler;
import nars.*;
import nars.derive.DerivationTemporalize;
import nars.derive.PrediTerm;
import nars.derive.rule.PremiseRule;
import nars.index.term.TermContext;
import nars.op.substitute;
import nars.task.ITask;
import nars.term.Functor;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atom;
import nars.term.atom.Bool;
import nars.term.subst.Unify;
import nars.truth.Stamp;
import nars.truth.Truth;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.impl.factory.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static nars.Op.Null;
import static nars.op.substituteIfUnifies.substituteIfUnifiesAny;
import static nars.time.Tense.ETERNAL;


/**
 * evaluates a premise (task, belief, termlink, taskLink, ...) to derive 0 or more new tasks
 */
public class Derivation extends Unify implements TermContext {

    @NotNull
    public final NAR nar;

    private final ImmutableMap<Term, Termed> derivationFunctors;

    public float truthResolution;
    public float confMin;

    @Nullable
    public Truth concTruth;
    public byte concPunc;


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
    public Term taskTerm;
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

    public float premisePri;
    public short[] parentCause;

    public PrediTerm<Derivation> deriver;
    public final ByteShuffler shuffler = new ByteShuffler(64);
    public boolean single;
    public final Map<Task,Task> derivations = new LinkedHashMap();
    public DerivationTemporalize temporalize;

//    private transient Term[][] currentMatch;

//    public /*static*/ final Cache<Transformation, Term> transformsCache; //works in static mode too
//    /*static*/ {
//    }

//    final MRUCache<Transformation, Term> transformsCache = new MRUCache<>(Param.DERIVATION_THREAD_TRANSFORM_CACHE_SIZE);

    /**
     * if using this, must set: nar, index, random, DerivationBudgeting
     */
    public Derivation(NAR nar) {
        super(null, nar.random(), Param.UnificationStackMax, 0);

        this.nar = nar;

//        Caffeine cb = Caffeine.newBuilder().executor(nar.exe);
//            //.executor(MoreExecutors.directExecutor());
//        int cs = Param.DERIVATION_TRANSFORM_CACHE_SIZE_PER_THREAD;
//        if (cs == -1)
//            cb.softValues();
//        else
//            cb.maximumSize(cs);
//
//        //cb.recordStats();
//
//        transformsCache = cb.builder();

        final Functor substituteIfUnifiesAny = new substituteIfUnifiesAny(this);
        //final Functor substituteIfUnifiesDep = new substituteIfUnifiesDep(this);
        final Functor polarize = Functor.f2("polarize", (subterm, whichTask) -> {
            Truth compared;
            if (whichTask.equals(PremiseRule.Task)) {
                compared = taskTruth;
            } else {
                compared = beliefTruth;
            }
            if (compared == null)
                return Null;
            else
                return compared.isNegative() ? subterm.neg() : subterm;
        });
        final Functor substitute = new substitute() {
            @Override
            protected boolean onChange(Term from, Term x, Term y, Term to) {
                assert(Derivation.this.xy.tryPut(x, y));
                return true;
            }
        };

        derivationFunctors = functors(
                substituteIfUnifiesAny,
                polarize,
                substitute,
                nar.get($.the("dropAnyEvent")),
                nar.get($.the("dropAnySet")),
                nar.get($.the("union")),
                nar.get($.the("differ")),
                nar.get($.the("intersect")),
                nar.get($.the("conjEvent"))
        );
    }

    static ImmutableMap<Term, Termed> functors(Termed... t) {
        java.util.Map m = new HashMap();
        for (Termed x : t) {
            m.put(x.term(), x);
        }
        return Maps.immutable.ofMap(m);
    }


    /**
     * only returns derivation-specific functors.  other functors must be evaluated at task execution time
     */
    @Override
    public Termed get(Term x, boolean createIfAbsent) {
        if (x instanceof Atom) {
            Termed f = derivationFunctors.get(x);
            if (f != null)
                return f;
        }

        return x;
    }

    /**
     * concept-scope
     *
     * @param deriver
     */
    public Derivation cycle(PrediTerm<Derivation> deriver) {
        long now = this.nar.time();
        if (now != this.time) {
            this.time = now;
            this.dur = nar.dur();
            this.truthResolution = nar.truthResolution.floatValue();
            this.confMin = nar.confMin.floatValue();
            //transformsCache.cleanUp();
        }
        this.deriver = deriver;
        return this;
    }


//    @Override
//    public void onDeath() {
//        nar.emotion.derivationDeath.increment();
//    }

    /**
     * tasklink/termlink scope
     */
    public Collection<Task> run(@NotNull Premise p, Task task, Task belief, Term beliefTerm, int ttl) {

        Term bt = beliefTerm.unneg();
        assert (!(bt instanceof Bool));

        revert(0); //revert directly

        //remove common variable entries because they will just consume memory if retained as empty
        //xy.map.entrySet().removeIf(e -> e.getKey() instanceof CommonVariable);
        xy.map.clear();

        termutes.clear();

        derivations.clear();

        forEachMatch = null;

        temporalize = null;

        this.task = task;

        this.taskTruth = task.truth();
        this.taskPunct = task.punc();

        Term tt = task.term();
        this.taskTerm = tt;
        this.termSub0Struct = tt.structure();
        Op tOp = tt.op();
        this.termSub0op = tOp.id;
        this.termSub0opBit = tOp.bit;

        this.concTruth = null;
        this.concPunc = 0;

        this.single = false;
        evidenceDouble = evidenceSingle = null;
        temporal = cyclic = overlap = false;

        assert (ttl > 0);
        this.setTTL(ttl);

        this.premise = p;

        this.belief = belief;



//        int ttv = taskTerm.vars();
//        if (ttv > 0 && bt.vars() > 0) {
//            bt = bt.normalize(ttv); //shift variables up to be unique compared to taskTerm's
//        }
        this.beliefTerm = bt;


        this.cyclic = task.cyclic(); //belief cyclic should not be considered because in single derivation its evidence will not be used any way

        if (belief != null) {
            Truth beliefTruth = this.beliefTruthRaw = belief.truth();

            /** to compute the time-discounted truth, find the minimum distance
             *  of the tasks considering their dtRange
             */
            if (!belief.isEternal()) {

                long beliefTruthTime;
                if (task.isEternal()) {
                    beliefTruthTime = ETERNAL;
                } else {
                    beliefTruthTime = belief.nearestTimeBetween(task.start(), task.end());
                }

                beliefTruth = belief.truth(beliefTruthTime, dur, 0); //project belief truth to task's time
            }

            this.beliefTruth = beliefTruth;

            overlap = (cyclic |= belief.cyclic()) || Stamp.overlapping(task, belief);

        } else {
            this.beliefTruth = this.beliefTruthRaw = null;
            this.overlap = false;
        }


        this.termSub1Struct = beliefTerm.structure();

        Op bOp = beliefTerm.op();
        this.termSub1op = bOp.id;
        this.termSub1opBit = bOp.bit;

        this.temporal = temporal(task) || temporal(belief);

        float premiseEvidence = task.isBeliefOrGoal() ? taskTruth.evi() : 0;
        if (beliefTruth != null)
            premiseEvidence = (premiseEvidence + beliefTruth.evi());
        this.premiseEvi = premiseEvidence;

        this.premisePri = p.priElseZero();

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

        return derivations.values();
    }


    /**
     * set in Solve once these (3) conclusion parameters have been determined
     */
    public void truth(Truth truth, byte punc, boolean single) {
        this.concTruth = truth;
        this.concPunc = punc;
        this.single = single;
        //this.concEvidence = evidence;
    }

    /**
     * only one thread should be in here at a time
     */
    public final void unifyAll(@NotNull Term x, @NotNull Term y, @Nullable PrediTerm<Derivation> eachMatch) {

        boolean finish = (this.forEachMatch = eachMatch) != null;
//        if (!finish) {
//            //before the start
//        }

        try {

//            x.recurseTerms(t -> {
//                assert (!(t instanceof UnnormalizedVariable));
//            });
//            y.recurseTerms(t -> {
//                assert (!(t instanceof UnnormalizedVariable));
//            });

            unify(x, y, finish);
        } finally {
            this.forEachMatch = null;
        }
//        if (finish) {
//            //after the end
//        }
    }

    @Override
    public final void onMatch(Term[][] match) {

        //this.currentMatch = match;

        int now = now();
        try {
            forEachMatch.test(this);
        } finally {
            revert(now); //undo any changes applied in conclusion
        }

    }


    private static boolean temporal(@Nullable Task task) {
        return task != null &&
                (!task.isEternal() || task.term().isTemporal());
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
            float te, be;
            if (task.isBeliefOrGoal()) {
                //for belief/goal use the relative conf
                te = taskTruth.conf();
                be = beliefTruth != null ? beliefTruth.conf() : 0; //beliefTruth can be zero in temporal cases
            } else {
                //for question/quest, use the relative priority
                te = task.priElseZero();
                be = belief.priElseZero();
            }
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
                + ' ' + super.toString();
    }

    public int ttl() {
        return ttl;
    }



    public void reset() {
        clear();
        time = ETERNAL;
    }

    @Override
    public void clear() {
        derivations.clear();
        super.clear();
    }

    //    /**
//     * experimental memoization of transform results
//     */
//    @Override
//    @Nullable
//    public Term transform(@NotNull Term pattern) {
//
//        if (!Param.DERIVATION_TRANSFORM_CACHE) {
//            return super.transform(pattern); //xy.get(pattern); //fast variable resolution
//        }
//
//        Term y = xy(pattern);
//        if (y!=null) {
//            if (pattern instanceof Variable || y.vars(null) == 0) {
////                if (xy.get(y)!=null)
////                    System.out.println(y + " -> " + xy.get(y));
//
//                return y;
//            }
//            pattern = y;
//        }
//        if (pattern instanceof Atomic) {
////            if (xy.get(x)!=null)
////                System.out.println(x + " -> " + xy.get(x));
//            return pattern;
//        }
//
////        if (x.OR(xx -> xx == Null))
////            return Null;
//
//        Transformation key = Transformation.the((Compound) pattern, currentMatch);
//
//        //avoid recursive update problem on the single thread by splitting the get/put
//        Term value = transformsCache.getIfPresent(key);
//        if (value != null)
//            return value;
//
//        value = super.transform(key.pattern);
//        if (value == null)
//            value = Null;
//
//        transformsCache.put(key, value);
//
//        if (value == Null)
//            value = null;
//
//        return value;
//    }
//
//    final static class Transformation {
//        public final Compound pattern;
//        final byte[] assignments;
//        final int hash;
//
//        Transformation(Compound pattern, DynBytes assignments) {
//            this.pattern = pattern;
//            this.assignments = assignments.array();
//            this.hash = Util.hashCombine(assignments.hashCode(), pattern.hashCode());
//        }
//
//        @Override
//        public boolean equals(Object o) {
//            //if (this == o) return true;
//            Transformation tthat = (Transformation) o;
//            if (hash != tthat.hash) return false;
//            return pattern.equals(tthat.pattern) && Arrays.equals(assignments, tthat.assignments);
//        }
//
//        @Override
//        public int hashCode() {
//            return hash;
//        }
//
//        static Transformation the(@NotNull Compound pattern, @NotNull Term[][] match) {
//
//            //TODO only include in the key the free variables in the pattern because there can be extra and this will cause multiple results that could have done the same thing
//            //FasterList<Term> key = new FasterList<>(currentMatch.length * 2 + 1);
//
//            DynBytes key = new DynBytes((2 * match.length + 1) * 8 /* estimate */);
//            pattern.append((ByteArrayDataOutput) key); //in 0th
//            key.writeByte(0);
//            for (Term[] m : match) {
//                Term var = m[0];
//                if (pattern.containsRecursively(var)) {
//                    var.append((ByteArrayDataOutput) key);
//                    key.writeByte(0);
//                    m[1].append((ByteArrayDataOutput) key);
//                    key.writeByte(0);
//                }
//            }
//            return new Transformation(pattern, key);
//        }
//
//    }


}


