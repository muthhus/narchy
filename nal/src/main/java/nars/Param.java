package nars;

import jcog.Util;
import jcog.data.FloatParam;
import jcog.data.MutableInteger;
import jcog.pri.op.PriMerge;
import jcog.util.FloatFloatToFloatFunction;
import nars.term.atom.Atom;
import nars.truth.PreciseTruth;
import nars.truth.Truth;
import nars.util.UtilityFunctions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.Op.*;

/**
 * NAR Parameters
 */
public abstract class Param  {

//
//    /** belief projection lookahead time in premise formation, in multiples of duration */
//    public static final int PREDICTION_HORIZON = 4;

    /** max time difference (measured in durations) between two non-adjacent/non-overlapping temporal tasks can be interpolated during a derivation */
    public static final int TEMPORAL_TOLERANCE_FOR_NON_ADJACENT_EVENT_DERIVATIONS = 2;

    public static final PriMerge termlinkMerge = PriMerge.plus;
    public static final PriMerge tasklinkMerge = PriMerge.plus; //not safe to plus without enough headroom
    public static final PriMerge taskMerge = PriMerge.max;
    public static final PriMerge conceptMerge = PriMerge.plus;

    /** used on premise formation  */
    public static final FloatFloatToFloatFunction tasktermLinkCombine =
            //Util::or;
            UtilityFunctions::aveAri;
            //Util::and;
            //Math::min;
            //Math::max;

    /** maximum time (in durations) that a signal task can latch its last value before it becomes unknown */
    public final static int SIGNAL_LATCH_TIME =
                    //0;
                    //Integer.MAX_VALUE;
                    4;

    /** cost of a termutate call */
    public static final int TTL_MUTATE = 1;

    /** cost of a term unification */
    public static final int TTL_UNIFY = 1;

    /** cost of a successful task derivation */
    public static final int TTL_DERIVE_TASK = 1;

    /** cost of a failed/aborted task derivation */
    public static final int TTL_DERIVE_TASK_FAIL = 1;

    /**
     * use this for advanced error checking, at the expense of lower performance.
     * it is enabled for unit tests automatically regardless of the value here.
     */
    public static boolean DEBUG;
    public static boolean TRACE;


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
    public static final int MAX_TERMPOLATE_RETRIES = 2;



//    /** determines if an input goal or command operation task executes */
//    public static float EXECUTION_THRESHOLD = 0.666f;

    public static boolean ANSWER_REPORTING = true;



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
    public static float HORIZON = 1f;

    public static final int MAX_VARIABLE_CACHED_PER_TYPE = 16;
    /**
     * Maximum length of the evidental base of the Stamp, a power of 2
     */
    public static final int STAMP_CAPACITY = 10;
    public static final int CAUSE_CAPACITY = 8;

    public final static int UnificationStackMax = 32; //how many assignments can be stored in the 'versioning' maps
    public static final int UnificationConstraintsMax = 8;
    public static final int UnificationVariableStackMax = 2; //how many rewrites a variable is allowed

    public final static int BeliefMatchTTL = 16;

    /** 'time to live', unification steps until unification is stopped */
    public final static int UnificationTTLMax = BeliefMatchTTL * 8;




    /** additional pedantic warnings */
    public static final boolean DEBUG_EXTRA = false;

    //public static final boolean DEBUG_BAG_MASS = false;
    //public static boolean DEBUG_TRACE_EVENTS = false; //shows all emitted events
    //public static boolean DEBUG_DERIVATION_STACKTRACES; //includes stack trace in task's derivation rule string
    //public static boolean DEBUG_INVALID_SENTENCES = true;
    //public static boolean DEBUG_NONETERNAL_QUESTIONS = false;
    public static boolean DEBUG_TASK_LOG = true; //false disables task history completely





    private Truth defaultGoalTruth, defaultBeliefTruth;


    /** internal granularity which truth components are rounded to */
    public static final float TRUTH_EPSILON = 0.01f;

    /**
     * how precise unit test results must match expected values to pass
     */
    public static float TESTS_TRUTH_ERROR_TOLERANCE = TRUTH_EPSILON;



//    /** EXPERIMENTAL  decreasing priority of sibling tasks on temporal task insertion */
//    public static final boolean SIBLING_TEMPORAL_TASK_FEEDBACK = false;

//    /** EXPERIMENTAL enable/disable dynamic tasklink truth revision */
//    public static final boolean ACTION_CONCEPT_LINK_TRUTH = false;



    /** derivation confidence (by evidence) multiplier.  normal=1.0, <1.0= under-confident, >1.0=over-confident */
    @NotNull public final FloatParam derivedEvidenceGain = new FloatParam(1f, 0f, 4f);




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



    public float priorityDefault(byte punctuation) {
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


    Param() {
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


    abstract public int level();



}
