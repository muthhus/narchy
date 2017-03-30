package nars;

import jcog.Util;
import jcog.bag.PLink;
import jcog.data.FloatParam;
import jcog.data.MutableInteger;
import nars.term.atom.Atom;
import nars.truth.DefaultTruth;
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




    /** determines if an input goal or command operation task executes */
    public static float EXECUTION_THRESHOLD = 0.666f;

    public static boolean ANSWER_REPORTING = true;

    /** default initial value for ConceptBagControl */
    public static int TASKS_INPUT_PER_CYCLE = 32;


    /**
     * hard upper-bound limit on Compound term complexity;
     * if this is exceeded it may indicate a recursively
     * malformed term due to a serious inference bug
     */
    public final MutableInteger termVolumeMax = new MutableInteger(COMPOUND_VOLUME_MAX );

    //public static final boolean ARITHMETIC_INDUCTION = false;


    /** whether derivation's concepts are cross-termlink'ed with the premise concept */
    public static boolean DERIVATION_TERMLINKED;
    public static boolean DERIVATION_TASKLINKED;


    //    //TODO use 'I' for SELf, it is 3 characters shorter
//    public static final Atom DEFAULT_SELF = (Atom) $.the("I");
    public static Atom randomSelf() {
        return $.quote("I_" + Util.uuid64());
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
    public static final int STAMP_CAPACITY = 16;


    /**
     * permute certain rules backward to questions (experimental, generates a lot of questions)
     */
    public static final boolean DERIVER_PERMUTE_BACKWARD = true;


    /**
     * swap task and belief in eligible rules ("forward" permutation)
     */
    public static final boolean DERIVER_PERMUTE_SWAPPED = true;


    ///** conjunctions over this length will be ineligible for 2nd-layer termlink templates. it can be decomposed however, and decompositions of this size or less will be eligible. */
    //public static final int MAX_CONJ_SIZE_FOR_LAYER2_TEMPLATES = 3;

    ///* adjuts this between 0 and 1. 0=fully backward, 0.5=balanced, 1=fully forward */
    //public static final float ACTIVATION_TERMLINK_BALANCE = 0.5f;


//    /** used in linear interpolating link adjustments during feedback. set to zero to disable */
//    public final FloatParam linkFeedbackRate = new FloatParam(0.0f);



    /**
     * maximum changes logged in deriver's stack.
     * bigger number means deeper unification depth for bigger compounds and more permutations
     */
    public final static int UnificationStackMax = 48;

    /**
     * upper and lower limits for # of termutations derived, determined by premise's priority
     */
    public static final int UnificationMatchesMax = 3;


    public final static int SubUnificationStackMax = UnificationStackMax/2;
    public static final int SubUnificationMatchRetries = UnificationMatchesMax;

    /**
     * minimum difference necessary to indicate a significant modification in budget float number components
     */
    public static final float BUDGET_EPSILON = PLink.EPSILON_DEFAULT;



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
    public static final float TRUTH_EPSILON = 0.001f;

    public static final float DEFAULT_SENSOR_RESOLUTION = 0.01f;

    /**
     * how precise unit test results must match expected values to pass
     */
    public static float TESTS_TRUTH_ERROR_TOLERANCE = 0.01f;



//    /** EXPERIMENTAL  decreasing priority of sibling tasks on temporal task insertion */
//    public static final boolean SIBLING_TEMPORAL_TASK_FEEDBACK = false;

//    /** EXPERIMENTAL enable/disable dynamic tasklink truth revision */
//    public static final boolean ACTION_CONCEPT_LINK_TRUTH = false;



    /** derivation confidence (by evidence) multiplier.  normal=1.0, <1.0= under-confident, >1.0=over-confident */
    @NotNull public FloatParam derivedEvidenceGain = new FloatParam(1f, 0f, 4f);




    @NotNull
    public final FloatParam truthResolution = new FloatParam(TRUTH_EPSILON, TRUTH_EPSILON, 1f);

    /**
     * truth confidence threshold necessary to form tasks
     */
    @NotNull
    public final FloatParam confMin = new FloatParam(0.01f, TRUTH_EPSILON, 1f);

    /**
     * budget quality threshold necessary to form tasks
     */
    public final FloatParam quaMin = new FloatParam(0, 0, 1f);


    /**
     * controls the speed (0..+1.0) of budget propagating from compound
     * terms to their subterms by adjusting the proportion of priority
     * retained by a compound vs. its subterms during an activation

     * values of 0 means all budget is transferred to subterms,
     * values of 1 means no budget is transferred
     */
    public final FloatParam momentum = new FloatParam(0.5f, 0, 1f);

    public float confidenceDefault(byte punctuation) {

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
        defaultGoalTruth = new DefaultTruth(1.0f, theDefaultValue);
    }

    /**
     * sets the default input belief confidence
     */
    public void beliefConfidence(float theDefaultValue) {
        defaultBeliefTruth = new DefaultTruth(1.0f, theDefaultValue);
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
