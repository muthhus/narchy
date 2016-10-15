package nars;

import nars.budget.BudgetFunctions;
import nars.learn.microsphere.InterpolatingMicrosphere;
import nars.nal.Level;
import nars.task.MutableTask;
import nars.task.TruthPolation;
import nars.term.atom.Atom;
import nars.truth.DefaultTruth;
import nars.truth.Truth;
import nars.truth.TruthFunctions;
import nars.util.Util;
import nars.util.data.MutableInteger;
import nars.util.data.Range;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.Symbols.*;
import static nars.truth.TruthFunctions.c2w;
import static nars.truth.TruthFunctions.w2c;

/**
 * NAR Parameters which can be changed during runtime.
 */
public abstract class Param /*extends Container*/ implements Level {


    //TODO use 'I' for SELf, it is 3 characters shorter
    public static final Atom DEFAULT_SELF = (Atom) $.the("I");
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


    /** average priority target for bag forgetting, between 0 and 1 usually 0.25..0.5 for balance */
    public static final float BAG_THRESHOLD = 0.25f;

    /** conjunctions over this length will be ineligible for 2nd-layer termlink templates. it can be decomposed however, and decompositions of this size or less will be eligible. */
    public static final int MAX_CONJ_SIZE_FOR_LAYER2_TEMPLATES = 3;

    /** 1 should work */
    public static final int ACTIVATION_TERMLINK_DEPTH = 1;
    public static final int ACTIVATION_TASKLINK_DEPTH = 1;

    final static float LIGHT_EPSILON = 0.5f;

    public static final InterpolatingMicrosphere.LightCurve evidentialDecayThroughTime = (dt, evidence) -> {

        if (dt <= LIGHT_EPSILON) {
            return evidence;
        } else {
//            float eternalized =
//                    //c2w(TruthFunctions.eternalize(w2c(evidence)));
//                    evidence/8f;


            float decayPeriod = 2f;
            float decayFactor = 1f / (1f + dt / decayPeriod);
            float newEvidence = evidence * decayFactor;
            //System.out.println(dt + "," + evidence + "\t" + decayPeriod + ","+decayFactor + "\t --> " + newEvidence);
            //return Math.max(eternalized, newEvidence);
            return newEvidence;
        }

    };


    public static boolean DEBUG_ANSWERS;

    /** how many times the desired selection size that bags should sample in case some of the selections are unused */
    public static float BAG_OVERSAMPLING = 2.0f;

    public static boolean SENSOR_TASKS_SHARE_COMMON_EVIDENCE = false;

    /** used in linear interpolating link adjustments during feedback. set to zero to disable */
    public final MutableFloat linkFeedbackRate = new MutableFloat(0.0f);

    /**
     * hard upper-bound limit on Compound term complexity;
     * if this is exceeded it may indicate a recursively
     * malformed term due to a serious inference bug
     */
    public final MutableInteger compoundVolumeMax = new MutableInteger(32);

    /**
     * maximum changes logged in deriver's stack.
     * bigger number means deeper unification depth for bigger compounds and more permutations
     */
    public final static int UnificationStackMax = 48;

    /**
     * upper and lower limits for # of termutations derived, determined by premise's priority
     */
    public static float UnificationMatchesMax = 4, UnificationMatchesMin = 2;


    /**
     * max # of chained termutes which can be active
     * bigger number means deeper unification depth for bigger compounds and more permutations
     */
    public final static int UnificationTermutesMax = 3;


    public final static int SubUnificationStackMax = UnificationStackMax/2;
    public final static int SubUnificationTermutesMax = UnificationTermutesMax/2;
    public static final int SubUnificationMatchRetries = Math.round(UnificationMatchesMin);

    /**
     * swap task and belief in eligible rules ("forward" permutation)
     */
    public static final boolean SWAP_RULES = true;
    /**
     * minimum difference necessary to indicate a significant modification in budget float number components
     */
    public static final float BUDGET_EPSILON = 0.0001f;


    public static final int DEFAULT_WIRED_CONCEPT_BELIEFS = 24;
    public static final int DEFAULT_WIRED_CONCEPT_GOALS = 16;

    /** size of each thread's normalization cache, in entries */
    public static final int NORMALIZATION_CACHE_SIZE = 16*1024;
    public static final int TERM_CACHE_SIZE = 32*1024;


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



    public static int QUERY_ANSWERS_PER_MATCH = 1;
    //public static boolean REDUCE_TRUTH_BY_TEMPORAL_DISTANCE;


    /**
     * how much to multiply (shrink) the rank of a potential belief match if it overlaps with the task.
     * used to discourage premise's choice of belief tasks which overlap with the task.
     */
    //public static float PREMISE_MATCH_OVERLAP_MULTIPLIER = 1f; //0.1f;


//    /**
//     * relates time and evidence (confidence); how past and future beliefs decay in rank
//     * across time; width of the temporal focus relative to the min/max occurrence times
//     * of tasks contained in the belief table
//     */
//    public static final float TEMPORAL_DURATION = 0.9f;

//    /**
//     * exponent by which confidence (modeled as luminance) decays through the time axis (>=1)
//     * see: the microsphere interpolation paper for analysis on this parameter
//     */
//    public static FloatToFloatFunction timeToLuminosity = (dt) -> {
//        //luminosity curve function
//        // see: https://en.wikipedia.org/wiki/Inverse-square_law
//        //      https://en.wikipedia.org/wiki/Distance_decay
//        //      https://en.wikipedia.org/wiki/Proportionality_(mathematics)#Inverse_proportionality
//        //float timeRate = 1f;
//        //return InterpolatingMicrosphere.pow(Math.max(0.5f, diffNorm)*timeRate, -exponent);
//
//        float duration = 1f;
//        return 1f / (1 + (dt*dt)/(duration*duration));
//
//        //return 1f / (1f + dt);
//        //return 1f / (1f + dt*dt);
//        //return 1f / ( 1f + (float)Math.sqrt(dt));
//        //return 1f / ( (float)Math.pow(1+dt, 1.5f));
//
//    };



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
    public static boolean REVECTION_PRIORITY_ZERO;

    /** additional output for when deriver fails before deriving something */
    public static boolean DEBUG_DERIVER;


    private Truth defaultGoalTruth, defaultBeliefTruth;



    public static final float TRUTH_EPSILON = 0.01f;
    /**
     * how precise unit test results must match expected values to pass
     */
    public static final float TESTS_TRUTH_ERROR_TOLERANCE = TRUTH_EPSILON;



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
            t.setBudget(defPri, defDur, DEFAULT_QUESTION_QUALITY);
        } else if (t.isQuest()) {
            t.setBudget(defPri, defDur, DEFAULT_QUEST_QUALITY);
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


    /**
     * a multiplicative factor which represents the relative separation in time. if  returns a value <= 1.0
     */
    public static float simultaneity(long delta /* positive only */, float duration /* <1, divides usually */) {
        //return (1f + (float)Math.log(1+delta/duration));
        return 1f / (1f + delta / duration);
    }

    /**
     * @param t
     * @return
     */
    public static float rankTemporalByConfidence(@Nullable Task t, long now) {
        if (t == null || t.isDeleted())
            return Float.NEGATIVE_INFINITY;

        long tOcc = t.occurrence();
        //long dWhenNow = Math.abs(when - now);
        //long dtCre = Math.abs(tOcc - t.creation());
        long dtOcc = Math.abs(tOcc - now);

        float pastAndPresentDuration = 1f;
        float futureDuration = 1f;

        float rank = t.conf() *
                simultaneity(
                        dtOcc,
                        tOcc <= now ? pastAndPresentDuration : futureDuration);
                //+ temporalIrrelevance(dWhenNow, 1f)
         // + temporalIrrelevance(dtCre, 1f));
        //System.out.println(now + ": " + t + " for " + when + " dt="+ dt + " rele=" + relevance + " rank=" + rank);
        return rank;


    }

}
