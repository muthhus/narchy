package nars;

import jcog.Services;
import jcog.Util;
import jcog.data.FloatParam;
import jcog.data.MutableInteger;
import jcog.pri.op.PriMerge;
import jcog.util.FloatFloatToFloatFunction;
import nars.task.Tasked;
import nars.task.TruthPolation;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.truth.PreciseTruth;
import nars.truth.Truth;
import nars.util.UtilityFunctions;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;

import static nars.Op.*;

/**
 * NAR Parameters
 */
public abstract class Param extends Services<Term,NAR> {


    /** must be big enough to support as many layers of compound terms as exist in an eval */
    public static final int MAX_EVAL_RECURSION = 16;

    /** rate that integers in integer-containing termlink compounds will be dynamically mutated on activation */
    public static final float MUTATE_INT_CONTAINING_TERMS_RATE = 0.5f;

    /** freq coherence below which a contradictory sensor belief is dis-valued, and above which it is valued */
    public static final float SENSOR_FEEDBACK_FREQ_THRESHOLD = 0.9f;

    /**
     * controls interpolation policy:
     * true: dt values will be interpolated
     * false: dt values will be chosen by weighted random decision
     * */
    public final MutableBoolean dtMergeOrChoose = new MutableBoolean(false);

    /** how many INT terms are canonically interned/cached. [0..n) */
    public final static int MAX_CACHED_INTS = 64;


    public static final boolean FILTER_SIMILAR_DERIVATIONS = true;
    public static final boolean DEBUG_SIMILAR_DERIVATIONS = false;


    /**
     * use this for advanced error checking, at the expense of lower performance.
     * it is enabled for unit tests automatically regardless of the value here.
     */
    public static boolean DEBUG;
    public static boolean TRACE;


//
//    /** belief projection lookahead time in premise formation, in multiples of duration */
//    public static final int PREDICTION_HORIZON = 4;

    /** max time difference (measured in durations) between two non-adjacent/non-overlapping temporal tasks can be interpolated during a derivation */
    public static final int TEMPORAL_TOLERANCE_FOR_NON_ADJACENT_EVENT_DERIVATIONS = 1;

    public static final PriMerge termlinkMerge = PriMerge.max;
    public static final PriMerge tasklinkMerge = PriMerge.max; //not safe to plus without enough headroom
    public static final PriMerge premiseMerge = PriMerge.max;
    public static final PriMerge taskMerge = PriMerge.max;
    public static final PriMerge conceptActivate = PriMerge.plus;


    /** used on premise formation  */
    public static final FloatFloatToFloatFunction tasktermLinkCombine =
            //UtilityFunctions::aveGeo;
            UtilityFunctions::aveAri;
            //Util::or; //potentially explosive
            //Util::and;
            //Math::min;
            //Math::max;

    /** maximum time (in durations) that a signal task can latch its last value before it becomes unknown */
    public final static int SIGNAL_LATCH_TIME_MAX =
                    //0;
                    //Integer.MAX_VALUE;
                    //4;
                    32;

    /** 'time to live', unification steps until unification is stopped */
    public final MutableInteger matchTTL = new MutableInteger(196);

    /** how much percent of a premise's allocated TTL can be used in the belief matching phase. */
    public static final float BELIEF_MATCH_TTL_FRACTION = 0.15f;

    /** cost of attempting a unification */
    public static final int TTL_UNIFY = 1;

    /** cost of a termutate permutation */
    public static final int TTL_MUTATE = 1;

    /** cost of attempting a derivation */
    public static final int TTL_DERIVE_EVAL = 1;

    /** cost of a successful task derivation */
    public static final int TTL_DERIVE_TASK_SUCCESS = 2;

    /** cost of a repeat (of another within the premise's batch) task derivation */
    public static final int TTL_DERIVE_TASK_REPEAT = 1;

    /** cost of a task derived, but too similar to one of its parents */
    public static final int TTL_DERIVE_TASK_SAME = 2;

    /** cost of a failed/aborted task derivation */
    public static final int TTL_DERIVE_TASK_FAIL = 1;

    /** number between 0 and 1 controlling the proportion of activation going
     * forward (compound to subterms) vs. reverse (subterms to parent compound).
     * when calculated, the total activation will sum to 1.0.
     * so 0.5 is equal amounts for both. */
    public static final float TERMLINK_BALANCE = 0.5f;

    /** how many durations above which to dither dt relations to dt=0 (parallel)
     *  set to zero to disable dithering.  typically the value will be 0..~1.0.
     */
    public final MutableFloat dtDither = new MutableFloat(0f);


    /** abs(term.dt()) safety limit for non-dternal/non-xternal temporal compounds */
    public static int DT_ABS_LIMIT = Integer.MAX_VALUE/256;

//    public final FloatParam valuePositiveDecay = new FloatParam(0.9f, 0, 1f);
//    public final FloatParam valueNegativeDecay = new FloatParam(0.9f, 0, 1f);

    /** pessimistic positive value measuring the computational cost of
     * inputting a task
     * (from now until the time of concept processing)
     * this may be balanced by a future positive value (ie. on concept processing) */
    public static float inputCost(Task t, NAR nar) {

//        //prefer simple
        float c = ((float)t.complexity())/nar.termVolumeMax.floatValue();

        //c *= t.priSafe(0);

        return c;
//
//        if (t.isBeliefOrGoal()) {
//
//            //prefer confidence
//            c *= (1f + (1f - Math.min(1f, t.conf()/nar.confDefault(t.punc()))));
//
//            //prefer polarized
//            //c *= (1f + p * (0.5f - Math.abs(t.freq()-0.5f)));
//        } else {
//            c *= 2;
//        }
//
//        return c;



        //return 0;
    }

    public float derivePriority(Term t, Truth tr, byte punc) {
        float p = 1f / (1f + ((float)t.complexity())/termVolumeMax.floatValue());


        if (/* belief or goal */ tr!=null) {

            //prefer confidence
            float cMin = confMin.floatValue();
            float cDef = confDefault(punc);
            p *= Util.unitize( (tr.conf()-cMin) / (cDef-cMin) );

            //prefer polarized
            //c *= (1f + p * (0.5f - Math.abs(t.freq()-0.5f)));
        } else {
            p *= 0.5f;
        }

        return p;
    }





    /** absolute limit for constructing terms in any context in which a NAR is not known, which could provide a limit.
     * typically a NAR instance's 'compoundVolumeMax' parameter will be lower than this */
    public static final int COMPOUND_VOLUME_MAX = 127;

    /**
     * limited because some subterm paths are stored as byte[]. to be safe, use 7-bits
     */
    public static final int COMPOUND_SUBTERMS_MAX = 127;

    /** how many answers to record per input question task (in its concept's answer bag) */
    public static final int MAX_INPUT_ANSWERS = 8;

    /** max retries for termpolation to produce a valid task content result during revision */
    public static final int MAX_TERMPOLATE_RETRIES = 1;



//    /** determines if an input goal or command operation task executes */
//    public static float EXECUTION_THRESHOLD = 0.666f;

    public static boolean ANSWER_REPORTING = true;


//    public static final boolean DERIVATION_TRANSFORM_CACHE = false;
//
//    /** -1 for softref */
//    public static final int DERIVATION_TRANSFORM_CACHE_SIZE_PER_THREAD =
//            //-1; //softref
//            32 * 1024;

    /**
     * hard upper-bound limit on Compound term complexity;
     * if this is exceeded it may indicate a recursively
     * malformed term due to a serious inference bug
     */
    public final MutableInteger termVolumeMax = new MutableInteger(COMPOUND_VOLUME_MAX );

    //public static final boolean ARITHMETIC_INDUCTION = false;


//    /** whether derivation's concepts are cross-termlink'ed with the premise concept */
//    public static boolean DERIVATION_TERMLINKED;
//    public static boolean DERIVATION_TASKLINKED;


    //    //TODO use 'I' for SELf, it is 3 characters shorter
//    public static final Atom DEFAULT_SELF = (Atom) $.the("I");
    static Atom randomSelf() {
        return (Atom) $.quote("I_" + Util.uuid64());
    }



    /**
     * Evidential Horizon, the amount of future evidence to be considered (during revision).
     * Must be >=1.0, usually 1 .. 2
     */
    public static final float HORIZON = 1f;

    public static final int MAX_VARIABLE_CACHED_PER_TYPE = 16;
    /**
     * Maximum length of the evidental base of the Stamp, a power of 2
     */
    public static final int STAMP_CAPACITY = 10;
    public static final int CAUSE_CAPACITY = 8;

    public final static int UnificationStackMax = 72; //how many assignments can be stored in the 'versioning' maps

    public static final int UnificationVariableCapInitial = 8;




    //public static final boolean DEBUG_BAG_MASS = false;
    //public static boolean DEBUG_TRACE_EVENTS = false; //shows all emitted events
    //public static boolean DEBUG_DERIVATION_STACKTRACES; //includes stack trace in task's derivation rule string
    //public static boolean DEBUG_INVALID_SENTENCES = true;
    //public static boolean DEBUG_NONETERNAL_QUESTIONS = false;
    public static final boolean DEBUG_TASK_LOG = true; //false disables task history completely





    private Truth defaultGoalTruth, defaultBeliefTruth;


    /** internal granularity which truth components are rounded to */
    public static final float TRUTH_EPSILON = 0.01f;

    /**
     * how precise unit test results must match expected values to pass
     */
    public static final float TESTS_TRUTH_ERROR_TOLERANCE = TRUTH_EPSILON;



//    /** EXPERIMENTAL  decreasing priority of sibling tasks on temporal task insertion */
//    public static final boolean SIBLING_TEMPORAL_TASK_FEEDBACK = false;

//    /** EXPERIMENTAL enable/disable dynamic tasklink truth revision */
//    public static final boolean ACTION_CONCEPT_LINK_TRUTH = false;



//    /** derivation confidence (by evidence) multiplier.  normal=1.0, <1.0= under-confident, >1.0=over-confident */
//    @NotNull public final FloatParam derivedEvidenceGain = new FloatParam(1f, 0f, 4f);




    @NotNull
    public final FloatParam truthResolution = new FloatParam(TRUTH_EPSILON, TRUTH_EPSILON, 1f);

    /**
     * truth confidence threshold necessary to form tasks
     */
    @NotNull
    public final FloatParam confMin = new FloatParam(TRUTH_EPSILON, TRUTH_EPSILON, 1f);


    /**
     * controls the speed (0..+1.0) of budget propagating from compound
     * terms to their subterms by adjusting the proportion of priority
     * retained by a compound vs. its subterms during an activation

     * values of 0 means all budget is transferred to subterms,
     * values of 1 means no budget is transferred
     */
    public final FloatParam momentum = new FloatParam(0.5f, 0, 1f);

    /**
     * dt > 0
     */
    public static float evidenceDecay(float evi, float dur, long dt) {

        //hard linear with half duration on either side of the task -> sum to 1.0 duration
//        float scale = dt / dur;
//        if (scale > 0.5f) return 0;
//        else return evi * (1f - scale*2f);


        //return evi / (1 + ((float) Math.log(1+dt/dur))); //inverse log

        //return evi / (1 + (((float) Math.log(1+dt)) / dur)); //inverse log

        return evi / (1 + ( dt / dur) ); //inverse linear

        //return evi / ( 1f + (dt*dt)/dur ); //inverse square

        //return evi /( 1 + 2 * (dt/dur) ); //inverse linear * 2 (nyquist recovery period)


        //return evi / (1f + dt / dur ); //first order decay
        //return evi / (1f + (dt*dt) / (dur*dur) ); //2nd order decay

    }

    @Nullable
    public static PreciseTruth truth(@Nullable Task topEternal, long start, long end, int dur, @NotNull Iterable<? extends Tasked> tasks) {

        assert (dur > 0);

        TruthPolation t =
                new TruthPolation.TruthPolationBasic(start, end, dur);
                //new TruthPolation.TruthPolationGreedy(start, end, dur);
                //..SoftMax..
                //new TruthPolation.TruthPolationRoulette(start, end, dur, ThreadLocalRandom.current());
                //new TruthPolationWithVariance(when, dur);

        // Contribution of each task's truth
        // use forEach instance of the iterator(), since HijackBag forEach should be cheaper
        tasks.forEach(t);
        if (topEternal != null) {
            t.accept(topEternal);
        }

        return t.truth();
    }


    public float confDefault(byte punctuation) {

        switch (punctuation) {
            case BELIEF:
                return defaultBeliefTruth.conf();

            case GOAL:
                return defaultGoalTruth.conf();

            default:
                throw new RuntimeException("Invalid punctuation " + punctuation + " for a TruthValue");
        }
    }


    /** no term sharing means faster comparison but potentially more memory usage. TODO determine effects */
    public static boolean CompoundDT_TermSharing;


    /**
     * Default priority of input judgment
     */
    public float DEFAULT_BELIEF_PRIORITY = 0.5f;

    /**
     * Default priority of input question
     */
    public float DEFAULT_QUESTION_PRIORITY = 0.5f;



    /**
     * Default priority of input judgment
     */
    public float DEFAULT_GOAL_PRIORITY = 0.5f;

    /**
     * Default priority of input question
     */
    public float DEFAULT_QUEST_PRIORITY = 0.5f;



    public float priDefault(byte punctuation) {
        switch (punctuation) {
            case BELIEF:
                return DEFAULT_BELIEF_PRIORITY;

            case QUEST:
                return DEFAULT_QUEST_PRIORITY;

            case QUESTION:
                return DEFAULT_QUESTION_PRIORITY;

            case GOAL:
                return DEFAULT_GOAL_PRIORITY;

            case COMMAND:
                return 0;
        }
        throw new RuntimeException("Unknown sentence type: " + punctuation);
    }




    @Nullable
    public final Truth truthDefault(byte p) {
        switch (p) {
            case GOAL:
                return defaultGoalTruth;
            case BELIEF:
                return defaultBeliefTruth;

            case COMMAND:
            case QUEST:
            case QUESTION:
                return null;

            default:
                throw new RuntimeException("invalid punctuation");
        }
    }


    Param(Executor exe) {
        super(null, exe);
        beliefConfidence(0.9f);
        goalConfidence(0.9f);
    }

    /**
     * sets the default input goal confidence
     */
    public void goalConfidence(float theDefaultValue) {
        defaultGoalTruth = new PreciseTruth(1.0f, theDefaultValue);
    }

    /**
     * sets the default input belief confidence
     */
    public void beliefConfidence(float theDefaultValue) {
        defaultBeliefTruth = new PreciseTruth(1.0f, theDefaultValue);
    }


    abstract public int nal();



}
