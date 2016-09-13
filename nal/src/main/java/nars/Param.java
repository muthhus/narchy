package nars;

import nars.budget.BudgetFunctions;
import nars.nal.Level;
import nars.task.MutableTask;
import nars.term.atom.Atom;
import nars.truth.DefaultTruth;
import nars.truth.Truth;
import nars.util.data.MutableInteger;
import nars.util.data.Range;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.Symbols.*;

/**
 * NAR Parameters which can be changed during runtime.
 */
public abstract class Param /*extends Container*/ implements Level {


    //TODO use 'I' for SELf, it is 3 characters shorter
    public static final Atom DEFAULT_SELF = $.the("I");
    /**
     * limited because some subterm paths are stored as byte[]. to be safe, use 7-bits
     */
    public static final int MAX_SUBTERMS = 127;
    /**
     * Evidential Horizon, the amount of future evidence to be considered (during revision).
     * Must be >=1.0, usually 1 .. 2
     */
    public static final float HORIZON = 1f;
    public static final int MAX_VARIABLE_CACHED_PER_TYPE = 16;
    /**
     * Maximum length of the evidental base of the Stamp, a power of 2
     */
    public static final int STAMP_CAPACITY = 12;


    /**
     * permute certain rules backward to questions (experimental, generates a lot of questions)
     */
    public static final boolean BACKWARD_QUESTION_RULES = true;

    /** factor applied to budgeting backward question derivations */
    public static final float BACKWARD_DERIVATION_FACTOR = 1f;

    /** average priority target for bag forgetting */
    public static final float BAG_THRESHOLD = 0.5f;

    /** used in linear interpolating link adjustments during feedback */
    public final MutableFloat linkFeedbackRate = new MutableFloat(0.05f);

    /**
     * hard upper-bound limit on Compound term complexity;
     * if this is exceeded it may indicate a recursively
     * malformed term due to a serious inference bug
     */
    public final MutableInteger compoundVolumeMax = new MutableInteger(64);

    /**
     * maximum changes logged in deriver's stack.
     * bigger number means deeper unification depth for bigger compounds and more permutations
     */
    public final static int UnificationStackMax = 48;

    /**
     * max # of chained termutes which can be active
     * bigger number means deeper unification depth for bigger compounds and more permutations
     */
    public final static int UnificationTermutesMax = 4;
    /**
     * swap task and belief in eligible rules ("forward" permutation)
     */
    public static final boolean SWAP_RULES = true;
    /**
     * minimum difference necessary to indicate a significant modification in budget float number components
     */
    public static final float BUDGET_EPSILON = 0.0002f;


    public static final int DEFAULT_WIRED_CONCEPT_BELIEFS = 16;
    public static final int DEFAULT_WIRED_CONCEPT_GOALS = 16;

    /** size of each thread's normalization cache, in entries */
    public static final int NORMALIZATION_CACHE_SIZE = 32*1024;
    public static final int TERM_CACHE_SIZE = 64*1024;


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
    /**
     * lower limit for # of termutations derived, determined by premise's priority
     */
    public static float matchTermutationsMin = 1;
    /**
     * upper limit for # of termutations derived, determined by premise's priority
     */
    public static float matchTermutationsMax = 2;
    public static int QUERY_ANSWERS_PER_MATCH = 1;
    //public static boolean REDUCE_TRUTH_BY_TEMPORAL_DISTANCE;


    /**
     * how much to multiply (shrink) the rank of a potential belief match if it overlaps with the task.
     * used to discourage premise's choice of belief tasks which overlap with the task.
     */
    //public static float PREMISE_MATCH_OVERLAP_MULTIPLIER = 1f; //0.1f;


    /** the result of an overlap will reduce the confidence by a proportional amount of the evidential overlap */
    public static boolean REVECTION_ALLOW_MERGING_OVERLAPPING_EVIDENCE = true;

//    /**
//     * relates time and evidence (confidence); how past and future beliefs decay in rank
//     * across time; width of the temporal focus relative to the min/max occurrence times
//     * of tasks contained in the belief table
//     */
//    public static final float TEMPORAL_DURATION = 0.9f;

    /**
     * exponent by which confidence (modeled as luminance) decays through the time axis (>=1)
     * see: the microsphere interpolation paper for analysis on this parameter
     */
    public static FloatToFloatFunction timeToLuminosity = (dt) -> {
        //luminosity curve function
        // see: https://en.wikipedia.org/wiki/Inverse-square_law
        //      https://en.wikipedia.org/wiki/Distance_decay
        //      https://en.wikipedia.org/wiki/Proportionality_(mathematics)#Inverse_proportionality
        //float timeRate = 1f;
        //return InterpolatingMicrosphere.pow(Math.max(0.5f, diffNorm)*timeRate, -exponent);

        float duration = 1f;
        return 1f / (1 + (dt*dt)/(duration*duration));

        //return 1f / (1f + dt);
        //return 1f / (1f + dt*dt);
        //return 1f / ( 1f + (float)Math.sqrt(dt));
        //return 1f / ( (float)Math.pow(1+dt, 1.5f));

    };



//    /** confidence factor to multiply eternalizable temporal beliefs.
//     *  displaced temporal beliefs and goals can be eternalized before being deleted, possibly preserving some of their truth value
//     *  should be equal to or less than 1.0, so that resulting eternal beliefs dont override temporal beliefs of the smae confidence.
//     *  (although revision can accumulate higher confidence).
//     *
//     *  this is applied after the usual TruthFunctions.eternalize function
//     *  that determines a (lower) confidence value for the given temporal.
//     *
//     *  set to 0.0 to disable this functionality.
//     */
//    public static float ETERNALIZE_FORGOTTEN_TEMPORAL_TASKS_CONFIDENCE_FACTOR = 1f;


    /** if false, then revection will be budgeted with parent's budget mix, otherwise it will have dur/qua mixed but priority set to zero to not trigger linking */
    public static boolean REVECTION_PRIORITY_ZERO = true;

    /** additional output for when deriver fails before deriving something */
    public static boolean DEBUG_DERIVER;


    private Truth defaultGoalTruth, defaultBeliefTruth;



    public static final float TRUTH_EPSILON = 0.01f;
    /**
     * how precise unit test results must match expected values to pass
     */
    public static final float TESTS_TRUTH_ERROR_TOLERANCE = TRUTH_EPSILON;


    public static final boolean ALLOW_RECURSIVE_IMPLICATIONS = false;
    //public static final boolean ALLOW_RECURSIVE_STATEMENTS = false;


    ///** extra debugging checks */
    //public static final boolean DEBUG_PARANOID = false;

    //public static boolean PRINT_DUPLICATE_DERIVATIONS = false;
    //public static final boolean DEBUG_DERIVATION_GRAPH = false;
    //public static final boolean DEBUG_REMOVED_CYCLIC_DERIVATIONS = false;
    //public static final boolean DEBUG_REMOVED_INSUFFICIENT_BUDGET_DERIVATIONS = false;
    //public static boolean DEBUG_DETECT_DUPLICATE_RULES;
    //public static final boolean DEBUG_NON_INPUT_ANSWERED_QUESTIONS = false;


    //TODO eventually sort out in case that a parameter is not needed anymore
//
//    public static float CURIOSITY_BUSINESS_THRESHOLD=0.15f; //dont be curious if business is above
//    public static float CURIOSITY_PRIORITY_THRESHOLD=0.3f; //0.3f in 1.6.3
//    public static float CURIOSITY_CONFIDENCE_THRESHOLD=0.8f;
//    public static float CURIOSITY_DESIRE_CONFIDENCE_MUL=0.1f; //how much risk is the system allowed to take just to fullfill its hunger for knowledge?
//    public static float CURIOSITY_DESIRE_PRIORITY_MUL=0.1f; //how much priority should curiosity have?
//    public static float CURIOSITY_DESIRE_DURABILITY_MUL=0.3f; //how much durability should curiosity have?
//    public static boolean CURIOSITY_FOR_OPERATOR_ONLY=false; //for Peis concern that it may be overkill to allow it for all <a =/> b> statement, so that a has to be an operator
//    public static boolean CURIOSITY_ALSO_ON_LOW_CONFIDENT_HIGH_PRIORITY_BELIEF=true;
//
//    //public static float HAPPY_EVENT_HIGHER_THRESHOLD=0.75f;
//    public static float HAPPY_EVENT_CHANGE_THRESHOLD =0.01f;
//    //public static float BUSY_EVENT_HIGHER_THRESHOLD=0.9f; //1.6.4, step by step^, there is already enough new things ^^
//    public static float BUSY_EVENT_CHANGE_THRESHOLD =0.5f;
//    public static boolean REFLECT_META_HAPPY_GOAL = false;
//    public static boolean REFLECT_META_BUSY_BELIEF = false;
//    public static boolean CONSIDER_REMIND=true;

//
//    public static boolean QUESTION_GENERATION_ON_DECISION_MAKING=true;
//    public static boolean HOW_QUESTION_GENERATION_ON_DECISION_MAKING=true;
//
//    public static float ANTICIPATION_CONFIDENCE=0.95f;


    @NotNull
    @Range(min = 0, max = 1f)
    public final MutableFloat confMin = new MutableFloat(TRUTH_EPSILON);

    @NotNull
    @Range(min = 0, max = 1f)
    public final MutableFloat truthResolution = new MutableFloat(TRUTH_EPSILON);


    /*
     BUDGET THRESHOLDS
     * Increasing this value decreases the resolution with which
     *   budgets are propagated or otherwise measured, which can result
     *   in a performance gain.      */


//    /** budget summary necessary to Conceptualize. this will compare the summary of the task during the TaskProcess */
//    public final AtomicDouble newConceptThreshold = new AtomicDouble(0);

    /**
     * budget durability threshold necessary to form a derived task.
     */
    public final MutableFloat durMin = new MutableFloat(0);


//    /** budget summary necessary to execute a desired Goal */
//    public final AtomicDouble questionFromGoalThreshold = new AtomicDouble(0);

    /**
     * budget summary necessary to run a TaskProcess for a given Task
     * this should be equal to zero to allow subconcept seeding.
     */
    public final MutableFloat taskProcessThreshold = new MutableFloat(0);

    /**
     * budget summary necessary to propagte tasklink activation
     */
    public final MutableFloat taskLinkThreshold = new MutableFloat(0);

    /**
     * budget summary necessary to propagte termlink activation
     */
    public final MutableFloat termLinkThreshold = new MutableFloat(0);


//    /** Maximum number of beliefs kept in a Concept */
//    public final AtomicInteger conceptBeliefsMax = new AtomicInteger();
//
//    /** Maximum number of questions, and max # of quests kept in a Concept */
//    public final AtomicInteger conceptQuestionsMax = new AtomicInteger();
//
//    /** Maximum number of goals kept in a Concept */
//    public final AtomicInteger conceptGoalsMax = new AtomicInteger();

    public float confidenceDefault(char punctuation) {

        switch (punctuation) {
            case BELIEF:
                return defaultBeliefTruth.conf();

            case GOAL:
                return defaultGoalTruth.conf();

            default:
                throw new RuntimeException("Invalid punctuation " + punctuation + " for a TruthValue");
        }
    }

    public void budgetDefault(@NotNull MutableTask t) {

        char punc = t.punc();

        float defPri = priorityDefault(punc);
        float defDur = durabilityDefault(punc);

        if (t.isBeliefOrGoal()) {
            t.budget(defPri, defDur);

            /** if q was not specified, and truth is, then we can calculate q from truthToQuality */
            float q = t.qua();
            if (q != q /* fast NaN test */) {
                t.setQuality(BudgetFunctions.truthToQuality(t.truth()));
            }
        } else if (t.isQuestion()) {
            t.budget(defPri, defDur, DEFAULT_QUESTION_QUALITY);
        } else if (t.isQuest()) {
            t.budget(defPri, defDur, DEFAULT_QUEST_QUALITY);
        }

    }


    /**
     * Default priority of input judgment
     */
    public float DEFAULT_BELIEF_PRIORITY = 0.5f;
    /**
     * Default durability of input judgment
     */
    public float DEFAULT_BELIEF_DURABILITY = 0.5f; //was 0.8 in 1.5.5; 0.5 after
    /**
     * Default priority of input question
     */
    public float DEFAULT_QUESTION_PRIORITY = 0.5f;
    /**
     * Default durability of input question
     */
    public float DEFAULT_QUESTION_DURABILITY = 0.5f;


    /**
     * Default priority of input judgment
     */
    public float DEFAULT_GOAL_PRIORITY = 0.5f;
    /**
     * Default durability of input judgment
     */
    public float DEFAULT_GOAL_DURABILITY = 0.5f;
    /**
     * Default priority of input question
     */
    public float DEFAULT_QUEST_PRIORITY = 0.5f;
    /**
     * Default durability of input question
     */
    float DEFAULT_QUEST_DURABILITY = 0.5f;

    float DEFAULT_QUESTION_QUALITY = 0.5f;
    float DEFAULT_QUEST_QUALITY = 0.5f;

    public float priorityDefault(char punctuation) {
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

    public float durabilityDefault(char punctuation) {
        switch (punctuation) {
            case BELIEF:
                return DEFAULT_BELIEF_DURABILITY;
            case QUEST:
                return DEFAULT_QUEST_DURABILITY;
            case QUESTION:
                return DEFAULT_QUESTION_DURABILITY;
            case GOAL:
                return DEFAULT_GOAL_DURABILITY;

            case COMMAND:
                return 0;
        }
        throw new RuntimeException("Unknown sentence type: " + punctuation);
    }

    public float qualityDefault(char punctuation) {
        switch (punctuation) {
            case QUEST:
                return DEFAULT_QUEST_QUALITY;
            case QUESTION:
                return DEFAULT_QUESTION_QUALITY;
            /*case Symbols.GOAL:
                return DEFAULT_GOAL_QUALITY;*/
        }
        throw new RuntimeException("Use truthToQuality for: " + punctuation);
    }

    //decision threshold is enough for now
    //float EXECUTION_SATISFACTION_TRESHOLD;

    /*public float getExecutionSatisfactionThreshold() {
        return EXECUTION_SATISFACTION_TRESHOLD;
    }*/


    @Nullable
    public final Truth truthDefault(char p) {
        switch (p) {
            case GOAL:
                return defaultGoalTruth;
            case BELIEF:
                return defaultBeliefTruth;

            case COMMAND:
                return null;
            case QUEST:
                return null;
            case QUESTION:
                return null;
            default:
                throw new RuntimeException("invalid punctuation");
        }
    }


//    /** Reliance factor, the empirical confidence of analytical truth.
//        (generally, the same as default judgment confidence)  */
//    public final AtomicDouble reliance = new AtomicDouble();




/*    public static Param fromJSON(String json) {
        return Param.json.fromJson(json, Param.class);
    }*/
//    @Override
//    public String toString() {
//        return Json.toJson(this);
//    }

//    public double[] toGenome(String... excludeFields) {
//        JsonObject j = json.toJsonTree(this).getAsJsonObject();
//        TreeSet<Map.Entry<String, JsonElement>> fields = new TreeSet<>(j.entrySet());
//        
//        Set<String> excluded = new HashSet();
//        for (String e : excludeFields)
//            excluded.add(e);
//        
//        List<Double> l = new ArrayList();
//        for (Map.Entry<String, JsonElement> e : fields) {
//            String f = e.getKey();
//            if (excluded.contains(f))
//                continue;
//            JsonElement v = e.getValue();
//            if (v.isJsonPrimitive()) {
//                try {
//                    double d = v.getAsDouble();
//                    l.add(d);                
//                }
//                catch (NumberFormatException nfe) { }
//            }
//        }
//        return Doubles.toArray(l);
//    }

//    static public final Gson json;
//    static {
//        GsonBuilder b = new GsonBuilder();
//        b.setPrettyPrinting();
//        b.disableHtmlEscaping();
//        b.serializeNulls();
//
//        final JsonSerializer<AtomicDouble> atomicDoubleSerializer = new JsonSerializer<AtomicDouble>() {
//            @Override public JsonElement serialize(AtomicDouble t, Type type, JsonSerializationContext jsc) {
//                return new JsonPrimitive(t.get());
//            }
//        };
//
//        JsonDeserializer<AtomicDouble> atomicDoubleDeserializer = new JsonDeserializer<AtomicDouble>() {
//            @Override public AtomicDouble deserialize(JsonElement je, Type type, JsonDeserializationContext jdc) throws JsonParseException {
//                return new AtomicDouble(je.getAsDouble());
//            }
//        };
//
//        b.registerTypeAdapter(AtomicDouble.class, atomicDoubleSerializer);
//        b.registerTypeAdapter(AtomicDouble.class, atomicDoubleDeserializer);
//
//        b.registerTypeAdapter(AtomicInteger.class, new JsonSerializer<AtomicInteger>() {
//            @Override public JsonElement serialize(AtomicInteger t, Type type, JsonSerializationContext jsc) {
//                return new JsonPrimitive(t.get());
//            }
//        });
//        b.registerTypeAdapter(AtomicInteger.class, new JsonDeserializer<AtomicInteger>() {
//            @Override public AtomicInteger deserialize(JsonElement je, Type type, JsonDeserializationContext jdc) throws JsonParseException {
//                return new AtomicInteger(je.getAsInt());
//            }
//        });
//
//
//        json = b.create();
//    }

    Param() {
        beliefConfidence(0.9f);
        goalConfidence(0.9f);
    }

    /**
     * sets the default input goal confidence
     */
    public void goalConfidence(float v) {
        defaultGoalTruth = new DefaultTruth(1.0f, v);
    }

    /**
     * sets the default input belief confidence
     */
    public void beliefConfidence(float v) {
        defaultBeliefTruth = new DefaultTruth(1.0f, v);
    }


    /** eternalize if... */
    public static boolean eternalizeForgottenTemporal(Op op) {
        return false;
        //return op.statement;
    }

    public static final float ETERNALIZATION_CONFIDENCE_FACTOR = 0.5f;

}
