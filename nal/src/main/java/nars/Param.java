package nars;

import jcog.Util;
import jcog.data.FloatParam;
import jcog.data.MutableInteger;
import nars.term.atom.Atom;
import nars.truth.PreciseTruth;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.Op.*;

/**
 * NAR Parameters which can be changed during runtime.
 */
public abstract class Param  {


    /** absolute limit for constructing terms in any context in which a NAR is not known, which could provide a limit.
     * typically a NAR instance's 'compoundVolumeMax' parameter will be lower than this */
    public static final int COMPOUND_VOLUME_MAX = 127;

    /**
     * limited because some subterm paths are stored as byte[]. to be safe, use 7-bits
     */
    public static final int COMPOUND_SUBTERMS_MAX = 127;

    /** how many answers to record per input question task (in its concept's answer bag) */
    public static final int MAX_INPUT_ANSWERS = 16;

    /** max retries for termpolation to produce a valid task content result during revision */
    public static final int MAX_TERMPOLATE_RETRIES = 2;


    /** determines if an input goal or command operation task executes */
    public static float EXECUTION_THRESHOLD = 0.666f;

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
    public static Atom randomSelf() {
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
    public static final int STAMP_CAPACITY = 12;

    public final static int UnificationStackMax = 24;
    public static final int MaxMatchConstraintsPerVariable = 8;
    public static final int MaxUnificationVariableStack = 4; //how many rewrites a variable is allowed

    public final static int BeliefMatchTTL = 24;

    /** 'time to live', unification steps until unification is stopped */
    public final static int UnificationTTL = 96;
    public final static int UnificationTTLMin = BeliefMatchTTL * 2;

    public final static int SubUnificationStackMax = UnificationStackMax/2;


    public static int DEFAULT_NAL_LEVEL = 8;

    /**
     * use this for advanced error checking, at the expense of lower performance.
     * it is enabled for unit tests automatically regardless of the value here.
     */
    public static boolean DEBUG;


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

    /** global scaling factor applied to termlink/tasklink activation */
    public final FloatParam linkActivation = new FloatParam(1f, 0, 1f);

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


    public float DEFAULT_QUESTION_QUALITY = 0.5f;
    public float DEFAULT_QUEST_QUALITY = DEFAULT_QUESTION_QUALITY;

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


    @Deprecated public float qualityDefault(byte punctuation) {
        switch (punctuation) {
            case COMMAND:
                return 0;

            case QUEST:
                return DEFAULT_QUEST_QUALITY;
            case QUESTION:
                return DEFAULT_QUESTION_QUALITY;
            /*case Symbols.GOAL:
                return DEFAULT_GOAL_QUALITY;*/

        }
        throw new RuntimeException("Use truthToQuality for: " + (char)punctuation);
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


//    @NotNull
//    Predicate levelMax(int level) {
//        return (r -> (level() <= level));
//    }

//    //TODO use IntStream.range?
//    Predicate[] maxLevel = {
//            levelMax(0), levelMax(1), levelMax(2), levelMax(3), levelMax(4), levelMax(5), levelMax(6), levelMax(7)
//    };
//

}
