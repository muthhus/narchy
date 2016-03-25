package nars.util.signal;

import com.gs.collections.api.block.function.primitive.FloatFunction;
import com.gs.collections.api.block.function.primitive.FloatToFloatFunction;
import nars.NAR;
import nars.Narsese;
import nars.bag.Bag;
import nars.concept.CompoundConcept;
import nars.concept.OperationConcept;
import nars.concept.util.ArrayBeliefTable;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.util.FloatSupplier;
import nars.util.data.Sensor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;


public class MotorConcept extends OperationConcept implements Consumer<NAR>, FloatFunction<Term> {

    private final Sensor feedback;
    private final Logger logger;
    private FloatToFloatFunction motor;
    float nextFeedback;

    public MotorConcept(@NotNull String compoundTermString, NAR n, FloatToFloatFunction motor) throws Narsese.NarseseException {
        this(compoundTermString, n);
        this.motor = motor;
    }

    public MotorConcept(@NotNull String compoundTermString, NAR n) throws Narsese.NarseseException {
        super(compoundTermString, n);
        this.logger = LoggerFactory.getLogger(getClass() + ":" + term);


        FloatToFloatFunction feedbackFuntion = (f) -> {
            if (f == 0) return 0f;
            return 0.5f + (f / 2f);
        };
        feedback = new Sensor(n, term, this, feedbackFuntion) {
            @Override
            protected int dt() {
                return nar.duration(); //future tense
            }

            @Override
            protected void init() {
                //Nothing, dont auto-start
            }

        };

        n.onFrame(this);
    }

    @Override
    protected int capacity(int maxBeliefs, boolean beliefOrGoal, boolean eternalOrTemporal) {
        return eternalOrTemporal ? 0 : maxBeliefs; //no eternal
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

    public final Sensor getFeedback() {
        return feedback;
    }

    /**
     * adjust min/max temporal resolution of feedback input
     */
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
    public final void accept(NAR nar) {
        FloatToFloatFunction m = getMotor();
        if (m != null) {
            nextFeedback = motor.valueOf(motivation(nar));
            feedback.accept(nar);
        } else {
            logger.info("null motor function");
        }
    }


}
