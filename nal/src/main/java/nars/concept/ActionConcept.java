package nars.concept;

import nars.NAR;
import nars.Narsese;
import nars.Symbols;
import nars.Task;
import nars.table.BeliefTable;
import nars.table.DefaultBeliefTable;
import nars.task.GeneratedTask;
import nars.term.Compound;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.$.$;


/** TODO make extend SensorConcept and utilize that for feedback control */
public class ActionConcept extends WiredCompoundConcept {


    /** relative temporal delta time for desire/belief prediction */
    final int decisionDT = 0;

    /** relative temporal delta time for feedback occurrence */
    final int feedbackDT = 0;



    private final float feedbackPriority;
    private final float feedbackDurability;

    private Task nextFeedback;

    float feedbackResolution = 0.05f;



    /** determines the feedback belief when desire or belief has changed in a MotorConcept
     *  implementations may be used to trigger procedures based on these changes.
     *  normally the result of the feedback will be equal to the input desired value
     *  although this may be reduced to indicate that the motion has hit a limit or
     *  experienced resistence
     * */
    @FunctionalInterface  public interface MotorFunction {

        /**
         * @param desired current desire - null if no desire Truth can be determined
         * @param believed current belief - null if no belief Truth can be determined
         * @return truth of a new feedback belief, or null to disable the creation of any feedback this iteration
         */
        @Nullable Truth motor(@Nullable Truth believed, @Nullable Truth desired);

        /** all desire passes through to affect belief */
        MotorFunction Direct = (believed, desired) -> desired;

        /** absorbs all desire and doesnt affect belief */
        @Nullable MotorFunction Null = (believed, desired) -> null;
    }


    @NotNull
    private MotorFunction motor;


    public ActionConcept(@NotNull String compoundTermString, @NotNull NAR n) throws Narsese.NarseseException {
        this($(compoundTermString), n, MotorFunction.Direct);
    }

    public ActionConcept(@NotNull String compoundTermString, @NotNull NAR n, @NotNull MotorFunction motor) throws Narsese.NarseseException {
        this($(compoundTermString), n, motor);
    }

    public ActionConcept(@NotNull Compound term, @NotNull NAR n, @NotNull MotorFunction motor) throws Narsese.NarseseException {
        super(term, n);

        //assert (Op.isOperation(this));

        this.feedbackPriority = n.priorityDefault(Symbols.GOAL /* though these will be used for beliefs */);
        this.feedbackDurability = n.durabilityDefault(Symbols.GOAL /* though these will be used for beliefs */);
        this.motor = motor;

        nar.onFrame(nn->{
            run();
        });
    }

//    @Override
//    public boolean validBelief(@NotNull Task t, @NotNull NAR nar) {
//        if (!t.isEternal() && t.occurrence() > nar.time() + 1) {
//            System.err.println("prediction detected: " + (t.occurrence() - nar.time()));
//        }
//        return true;
//    }
//
//    @Override
//    public boolean validGoal(@NotNull Task t, @NotNull NAR nar) {
//        if (!t.isEternal() && t.occurrence() > nar.time() + 1) {
//            System.err.println("prediction detected: " + (t.occurrence() - nar.time()));
//        }
//        return true;
//    }

//    @Override
//    public @NotNull Task filterGoals(@NotNull Task t, @NotNull NAR nar, List<Task> displaced) {
//        return t;
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

    @Override
    protected final boolean runLater(@NotNull Task t, @NotNull NAR nar) {
        //return hasGoals();
        return false; //will run automatically each frame, as set in constructor
    }


    /**
     * change the motor function
     */
    public final void setMotor(@NotNull MotorFunction motor) {
        this.motor = motor;
    }



    @Override
    protected final void update() {

        long now = nar.time();
        @Nullable Truth d = this.desire(now + decisionDT);
        @Nullable Truth b = this.belief(now + decisionDT);

        Truth feedback = motor.motor(b, d);
        if (feedback != null) {
            Task next = feedback(feedback, now + feedbackDT);
            if (nextFeedback == null || !nextFeedback.equalsTruth(next, feedbackResolution)) { //if feedback is different from last
                nextFeedback = next;
                nar.inputLater(next);
            }
        }

    }

    protected final Task feedback(Truth t, long when) {
        return new GeneratedTask(this, Symbols.BELIEF, t)
                .time(when, when)
                .budget(feedbackPriority, feedbackDurability)
                .log("Motor Feedback");
    }


    @NotNull
    @Override
    protected BeliefTable newBeliefTable(int eCap, int tCap) {
        return new SensorBeliefTable(tCap);
    }

    private final class SensorBeliefTable extends DefaultBeliefTable {

        public SensorBeliefTable(int tCap) {
            super(tCap);
        }

        @Override
        public Truth truth(long when, long now) {
//            if (when == now || when == ETERNAL)
//                return sensor.truth();

            // if when is between the last input time and now, evaluate the truth at the last input time
            // to avoid any truth decay across time. this emulates a persistent latched sensor value
            // ie. if it has not changed
            if (nextFeedback !=null && when <= now && when >= nextFeedback.occurrence()) {
                //now = when = sensor.lastInputTime;
                return nextFeedback.truth();
            }

            return super.truth(when, now);
        }

        @Override
        public Task match(@NotNull Task target, long now) {
            long when = target.occurrence();

            Task f = ActionConcept.this.nextFeedback;
            if (f !=null && when <= now && when >= f.occurrence()) {
                return f;
            }

            return super.match(target, now);
        }

        //        @Override
//        public Task match(@NotNull Task target, long now) {
//            long when = target.occurrence();
//            if (when == now || when == ETERNAL) {
//                sensor.
//                return sensor.truth();
//            }
//
//            return super.match(target, now);
//        }
    }




}
