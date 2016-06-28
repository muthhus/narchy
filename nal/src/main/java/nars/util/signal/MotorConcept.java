package nars.util.signal;

import com.gs.collections.api.block.function.primitive.FloatFunction;
import com.gs.collections.api.block.function.primitive.FloatToFloatFunction;
import nars.NAR;
import nars.Narsese;
import nars.Op;
import nars.budget.policy.ConceptPolicy;
import nars.concept.OperationConcept;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.Truth;
import nars.util.data.Sensor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.$.$;
import static nars.budget.policy.DefaultConceptPolicy.beliefCapacityNonEternal;


public class MotorConcept extends OperationConcept implements FloatFunction<Term> {

    /**
     * all effected motor actuation relative to the differential of current desire and current belief
     */
    public static final MotorFunction relative = (d,b) -> d;

    /**
     * motor feedback attenuated by half of motor input
     */
    public static final FloatToFloatFunction leaky = m -> m / 2;

    /**
     * motor actuation directly controlled by desire value d
     */
    public static final FloatToFloatFunction absolute = m -> Float.NaN;


    /** relative temporal lookahead for desire/belief prediction */
    final int motorDT = 1;

    @NotNull
    public final Sensor feedback;
    //private final Logger logger;

    @FunctionalInterface  public interface MotorFunction {
        float motor(float believed, float desired);
    }

    /**
     * input: 0..+1 (expectation)   output feedback: 0..+1 or NaN
     */
    @NotNull
    private MotorFunction motor;

    /**
     * belief feedback expectation
     */
    float nextFeedback;

    public MotorConcept(@NotNull String compoundTermString, @NotNull NAR n, @NotNull MotorFunction motor) throws Narsese.NarseseException {
        this($(compoundTermString), n, motor);
    }
    public MotorConcept(@NotNull Compound term, @NotNull NAR n, @NotNull MotorFunction motor) throws Narsese.NarseseException {
        super(term, n);

        assert (Op.isOperation(this));

        //this.logger = LoggerFactory.getLogger(getClass() + ":" + term);


        feedback = new Sensor(n, this, this) {

            @Override
            protected int dt() {
                return 0; //0=now/immediate, +=future tense
            }

            @NotNull
            @Override
            protected Task newInputTask(float v, long now) {
                Task t = super.newInputTask(v, now);
                if (t!=null) {
                    t.log("Motor Feedback");
                }
                return t;
            }

            @Override
            protected void init() {
                //Nothing, dont auto-start
            }

            @NotNull
            @Override
            public Termed<Compound> term() {
                return MotorConcept.this; //allow access to this concept directly
            }
        };


        this.motor = motor;

        //n.onFrame(this);

    }

    /** allow no eternal beliefs, and ONE eternal goal */
    @Override protected void beliefCapacity(ConceptPolicy p) {
        beliefCapacityNonEternal(this, p);
        goals().capacity(1, p.beliefCap(this, false, true) + p.beliefCap(this, false, false)  - 1);
    }



    /**
     * called each frame with the current motivation measurement (0 <= m <= 1).
     * return a value indicating the actual motivation applied.  for example,
     * <p>
     * if all the motivation was applied then return the input value as-is.
     * <p>
     * if a motor experienced resistance to being driven, then the return value
     * would be less than the input motivation.
     */
    @NotNull
    public MotorFunction getMotor() {
        return motor;
    }

    @NotNull
    public final Sensor getFeedback() {
        return feedback;
    }

    /**
     * adjust min/max temporal resolution of feedback input
     */
    @NotNull
    public MotorConcept setFeedbackTiming(int minCycles, int maxCycles) {
        feedback.minTimeBetweenUpdates(minCycles);
        feedback.maxTimeBetweenUpdates(maxCycles);
        return this;
    }

    /**
     * change the motor function
     */
    public void setMotor(@NotNull MotorFunction motor) {
        this.motor = motor;
    }

    /**
     * returns the last recorded feedback value
     */
    @Override
    public float floatValueOf(Term anObject /* ignored */) {
        return nextFeedback;
    }

    @Override
    public void accept(NAR nar) {
        super.accept(nar);

        long now = nar.time();
        @Nullable Truth d = this.desire(now+motorDT);
        float desired = d!=null ? d.motivation() : 0f;
        @Nullable Truth b = this.belief(now+motorDT);
        float believed = b!=null ? b.motivation() : 0f;

        float response = motor.motor(believed, desired);

        if (Float.isFinite(response)) {
            nextFeedback = response;
            feedback.accept(nar);
        }

    }

    //    @Override
//    public @Nullable
//    Task processBelief(@NotNull Task belief, @NotNull NAR nar) {
//        //if (belief.evidence().length > 1) {
//
////        //Filter feedback that contradicts the sensor's provided beliefs
////        if (belief!=feedback.next()) {
////            //logger.error("Sensor concept rejected derivation:\n {}\npredicted={} derived={}", belief.explanation(), belief(belief.occurrence()), belief.truth());
////
////            //TODO delete its non-input parent tasks?
////            onConflict(belief);
////
////            return null;
////        }
//
//        return super.processBelief(belief, nar);
//    }

//    @Override
//    public @Nullable Task processGoal(@NotNull Task goal, @NotNull NAR nar) {
//        //if (!goal.isInput())
//            //System.err.println(goal.explanation());
//
//        return super.processGoal(goal, nar);
//    }

    /** called when a conflicting belief has attempted to be processed */
    protected void onConflict(@NotNull Task belief) {
        //if (!belief.isInput())
            //System.err.println(belief.explanation());
    }

}
