package nars;

import nars.budget.BudgetFunctions;
import nars.data.Range;
import nars.nal.Level;
import nars.task.MutableTask;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.truth.DefaultTruth;
import nars.truth.Truth;
import nars.util.data.MutableInteger;
import objenome.Container;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.Symbols.*;

/**
 * NAR Parameters which can be changed during runtime.
 */
public abstract class Param extends Container implements Level {


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
    public static final int MAX_VARIABLE_CACHED_PER_TYPE = 12;
    /**
     * Maximum length of the evidental base of the Stamp, a power of 2
     */
    public static final int STAMP_MAX_EVIDENCE = 10;
    /**
     * hard upper-bound limit on Compound term complexity;
     * if this is exceeded it may indicate a recursively
     * malformed term due to a serious inference bug
     */
    public static final int compoundVolumeMax = 192;
    /**
     * maximum changes logged in deriver's stack
     */
    public final static int UnificationStackMax = 72;
    /**
     * max # of chained termutes which can be active
     */
    public final static int UnificationTermutesMax = 8;
    /**
     * permute certain rules backward to questions (experimental, generates a lot of questions)
     */
    public static final boolean BACKWARD_QUESTION_RULES = true;
    /**
     * swap task and belief in eligible rules ("forward" permutation)
     */
    public static final boolean SWAP_RULES = true;
    /**
     * minimum difference necessary to indicate a significant modification in budget float number components
     */
    public static final float BUDGET_EPSILON = 0.0001f;
    /**
     * minimum durability and quality necessary for a derivation to form
     */
    public static final float DERIVATION_DURABILITY_THRESHOLD = BUDGET_EPSILON * 2f;
    /**
     * relates time and evidence (confidence); how past and future beliefs decay in rank
     * across time; width of the temporal focus
     */
    public static final float TEMPORAL_DURATION = 2f;
    public static int DEFAULT_NAL_LEVEL = 8;
    public static boolean EXIT_ON_EXCEPTION = true;
    /**
     * use this for advanced error checking, at the expense of lower performance.
     * it is enabled for unit tests automatically regardless of the value here.
     */
    public static boolean DEBUG;
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
    public static float matchTermutationsMax = 3;
    public static int QUERY_ANSWERS_PER_MATCH = 1;
    public static boolean REDUCE_TRUTH_BY_TEMPORAL_DISTANCE;
    /**
     * exponent by which confidence (modeled as luminance) decays through the time axis (>=1)
     */
    public static float TEMPORAL_MICROSPHERE_EXPONENT = 2f;
    /**
     * how much to multiply (shrink) the rank of a potential belief match if it overlaps with the task.
     * used to discourage premise's choice of belief tasks which overlap with the task.
     */
    public static float PREMISE_MATCH_OVERLAP_MULTIPLIER = 0.1f;
    /**
     * if false, then revection is not allowed to merge overlapping tasks when choosing a weakest pair to merge during compression
     */
    public static boolean REVECTION_ALLOW_MERGING_OVERLAPPING_EVIDENCE = true;

    @Nullable
    private Truth defaultGoalTruth, defaultJudgmentTruth;

    public final MutableInteger cyclesPerFrame = new MutableInteger(1);


    public static final float TRUTH_EPSILON = 0.01f;
    /**
     * how precise unit test results must match expected values to pass
     */
    public static final float TESTS_TRUTH_ERROR_TOLERANCE = TRUTH_EPSILON;
    public static final int TRUTH_DISCRETION = (int) (1f / TRUTH_EPSILON);


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


    /**
     * factor for concept activation [0 <= c <= 1]
     */
    public final MutableFloat conceptActivation = new MutableFloat(1f);


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

    /**
     * Minimum expectation for a desire value.
     * the range of "now" is [-DURATION, DURATION];
     */
    public final MutableFloat executionThreshold = new MutableFloat();

    public static boolean ensureValidVolume(@NotNull Term derived) {

        //HARD VOLUME LIMIT
        boolean valid = derived.volume() <= compoundVolumeMax;
        if (!valid && DEBUG) {
            //$.logger.error("Term volume overflow");
                /*c.forEach(x -> {
                    Terms.printRecursive(x, (String line) ->$.logger.error(line) );
                });*/

            $.logger.warn("Derivation explosion: {}", derived/*, rule*/);

            //System.err.println(m.premise.task().explanation());
            //System.err.println( (m.premise.belief()!=null) ? m.premise.belief().explanation() : "belief: null");
            //System.exit(1);
            //throw new RuntimeException(message);
            return false;
        }

        return valid;

    }


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
                return defaultJudgmentTruth.conf();

            case GOAL:
                return defaultGoalTruth.conf();

            default:
                throw new RuntimeException("Invalid punctuation " + punctuation + " for a TruthValue");
        }
    }

    public void budgetDefault(@NotNull MutableTask t) {

        char punc = t.punc();
        t.setPriority(priorityDefault(punc));
        t.setDurability(durabilityDefault(punc));

        if (t.isBeliefOrGoal()) {

            /** if q was not specified, and truth is, then we can calculate q from truthToQuality */
            float q = t.qua();
            if (q != q /* fast NaN test */) {
                t.setQuality(BudgetFunctions.truthToQuality(t.truth()));
            }
        } else if (t.isQuestion()) {
            t.setQuality(DEFAULT_QUESTION_QUALITY);
        } else if (t.isQuest()) {
            t.setQuality(DEFAULT_QUEST_QUALITY);
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
                return defaultJudgmentTruth;

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
        defaultJudgmentTruth = new DefaultTruth(1.0f, v);
    }
}
