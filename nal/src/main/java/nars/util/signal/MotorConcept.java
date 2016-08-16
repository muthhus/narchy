package nars.util.signal;

import nars.NAR;
import nars.Narsese;
import nars.Symbols;
import nars.Task;
import nars.task.GeneratedTask;
import nars.term.Compound;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.$.$;


public class MotorConcept extends WiredConcept  {


    /** relative temporal delta time for desire/belief prediction */
    final int decisionDT = 0;

    /** relative temporal delta time for feedback occurrence */
    final int feedbackDT = 1;



    private final float feedbackPriority;
    private final float feedbackDurability;

    private Task lastFeedback;

    float feedbackResolution = 0.05f;


    /** determines the feedback belief when desire or belief has changed in a MotorConcept
     *  implementations may be used to trigger procedures based on these changes.
     *  normally the result of the feedback will be equal to the input desired value
     *  although this may be reduced to indicate that the motion has hit a limit or
     *  experienced resistence
     * */
    @FunctionalInterface  public interface MotorFunction {

        /**
         * @param desired current desire
         * @param believed current belief
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


    public MotorConcept(@NotNull String compoundTermString, @NotNull NAR n) throws Narsese.NarseseException {
        this($(compoundTermString), n, MotorFunction.Direct);
    }

    public MotorConcept(@NotNull String compoundTermString, @NotNull NAR n, @NotNull MotorFunction motor) throws Narsese.NarseseException {
        this($(compoundTermString), n, motor);
    }

    public MotorConcept(@NotNull Compound term, @NotNull NAR n, @NotNull MotorFunction motor) throws Narsese.NarseseException {
        super(term, n);

        //assert (Op.isOperation(this));

        this.feedbackPriority = n.priorityDefault(Symbols.GOAL /* though these will be used for beliefs */);
        this.feedbackDurability = n.durabilityDefault(Symbols.GOAL /* though these will be used for beliefs */);
        this.motor = motor;

        nar.onFrame(nn->{
            run();
        });
    }

    @Override
    public boolean validBelief(@NotNull Task belief, @NotNull NAR nar) {
        //TODO only allow motor feedback?
        //return futureDerivationsOnly(belief, nar);
        return true;
    }

    @Override
    public boolean validGoal(@NotNull Task belief, @NotNull NAR nar) {
        return true;
    }

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

        if (!hasGoals())
            return;

        long now = nar.time();
        @Nullable Truth d = this.desire(now+ decisionDT);
        @Nullable Truth b = this.belief(now+ decisionDT);

        Truth feedback = motor.motor(b, d);
        if (feedback!=null) {
            Task next = feedback(feedback, now);
            if (lastFeedback==null || !lastFeedback.equalsTruth(next, feedbackResolution)) { //if feedback is different from last
                lastFeedback = next;
                nar.inputLater(next);
            }
        }
    }

    protected final Task feedback(Truth t, long when) {
        return new GeneratedTask(this, Symbols.BELIEF, t)
                .time(when, when+ feedbackDT)
                .budget(feedbackPriority, feedbackDurability)
                .log("Motor Feedback");
    }






}
