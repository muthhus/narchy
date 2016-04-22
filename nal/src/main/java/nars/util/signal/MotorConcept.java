package nars.util.signal;

import com.gs.collections.api.block.function.primitive.FloatFunction;
import com.gs.collections.api.block.function.primitive.FloatToFloatFunction;
import nars.NAR;
import nars.Narsese;
import nars.Op;
import nars.concept.OperationConcept;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.util.data.Sensor;
import nars.util.data.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;


public class MotorConcept extends OperationConcept implements Consumer<NAR>, FloatFunction<Term> {

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


    @NotNull
    private final Sensor feedback;
    private final Logger logger;

    @FunctionalInterface  public interface MotorFunction {
        public float motor(float believed, float desired);
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

    public MotorConcept(@NotNull String compoundTermString, @NotNull NAR n, MotorFunction motor) throws Narsese.NarseseException {
        super(compoundTermString, n);

        assert (Op.isOperation(term()));

        this.logger = LoggerFactory.getLogger(getClass() + ":" + term);


        FloatToFloatFunction motivationToFeedback = (f) -> {
            return 0.5f + (f / 2f);
        };
        feedback = new Sensor(n, this, this, motivationToFeedback) {
            @Override
            protected int dt() {
                return 0; //0=now/immediate, +=future tense
            }

            @NotNull
            @Override
            protected Task newInputTask(float f, float c, long now) {
                Task t = super.newInputTask(f, c, now);
                t.log("Motor Feedback");
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
        //this value is chosen so that the expectation of feedback will
        //  equal the expectation of activation, by default. this creates a balanced self-cancelling feedback
        //  loop when a motor function returns a feedback equal to the activation it received
        //ex: activation
        feedback.conf(0.9f);
        //feedback.maxTimeBetweenUpdates(1);

        setMotor(motor);

        n.onFrame(this);

        nextFeedback = 0.5f; //initialize belief
        feedback.accept(n);
    }


//    @Override
//    protected int capacity(int cap, boolean beliefOrGoal, boolean eternalOrTemporal) {
//        return eternalOrTemporal ? 0 : cap; //no eternal
//    }


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
    public void setMotor(MotorFunction motor) {
        this.motor = motor;
    }

    /**
     * returns the last recorded feedback value
     */
    @Override
    public final float floatValueOf(Term anObject /* ignored */) {
        return nextFeedback;
    }


    @Override
    public void update(@NotNull NAR nar) {
        feedback.ready();
        super.update(nar);
    }

    @Override
    public final void accept(@NotNull NAR nar) {

        if (!hasGoals())
            return;

        //if (m != null) {
        float desired =
                //motivation(nar);
                expectation(nar);
        float believed = this.believed.expectation();

        float response = motor.motor(believed, desired);

        if (Float.isFinite(response)) {
            nextFeedback = response;
            feedback.accept(nar);
        }


        /*} else {
            logger.info("null motor function");
        }*/
    }


    @Nullable
    @Override
    public Task processBelief(@NotNull Task belief, @NotNull NAR nar) {
        return super.processBelief(belief, nar);
    }
}
