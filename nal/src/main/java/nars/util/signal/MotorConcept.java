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

    @NotNull
    private final Sensor feedback;
    private final Logger logger;
    private FloatToFloatFunction motor;
    float nextFeedback;

    public MotorConcept(@NotNull String compoundTermString, @NotNull NAR n, FloatToFloatFunction motor) throws Narsese.NarseseException {
        this(compoundTermString, n);
        setMotor(motor);
    }

    public MotorConcept(@NotNull String compoundTermString, @NotNull NAR n) throws Narsese.NarseseException {
        super(compoundTermString, n);

        assert(Op.isOperation(term()));

        this.logger = LoggerFactory.getLogger(getClass() + ":" + term);


//        FloatToFloatFunction feedbackFuntion = (f) -> {
//            if (f == 0) return 0f;
//            return 0.5f + (f / 2f);
//        };
        feedback = new Sensor(n, this, this) {
            @Override
            protected int dt() {
                return nar.duration(); //future tense
            }

            @Override
            protected void init() {
                //Nothing, dont auto-start
            }

            @Override
            public Termed<Compound> term() {
                return MotorConcept.this; //allow access to this concept directly
            }
        };

        n.onFrame(this);
    }

    @Override
    protected int capacity(int cap, boolean beliefOrGoal, boolean eternalOrTemporal) {
        return eternalOrTemporal ? 0 : cap; //no eternal
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
    public FloatToFloatFunction getMotor() {
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
    public void setMotor(FloatToFloatFunction motor) {
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
    protected void update(float b, float d, long now) {
        feedback.ready();
        super.update(b, d, now);
    }

    @Override
    public final void accept(@NotNull NAR nar) {

        FloatToFloatFunction m = getMotor();
        if (m != null) {
            float activation = motivation(nar);

            float response = motor.valueOf(activation);
            nextFeedback = Util.clamp(response);

            feedback.accept(nar);

        } else {
            logger.info("null motor function");
        }
    }


    @Nullable
    @Override
    public Task processBelief(@NotNull Task belief, @NotNull NAR nar) {
        return super.processBelief(belief, nar);
    }
}
