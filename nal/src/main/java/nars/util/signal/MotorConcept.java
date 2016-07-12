package nars.util.signal;

import com.gs.collections.api.block.function.primitive.FloatFunction;
import com.gs.collections.api.block.function.primitive.FloatToFloatFunction;
import com.gs.collections.api.block.procedure.primitive.FloatFloatProcedure;
import nars.*;
import nars.budget.policy.ConceptPolicy;
import nars.concept.OperationConcept;
import nars.task.MutableTask;
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
import static nars.budget.policy.DefaultConceptPolicy.goalCapacityOneEternal;


public class MotorConcept extends OperationConcept  {




    /** relative temporal lookahead for desire/belief prediction */
    final int motorDT = 1;

    private final float feedbackPriority;
    private final float feedbackDurability;


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
        MotorFunction Null = (believed, desired) -> null;
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

        assert (Op.isOperation(this));

        this.feedbackPriority = n.priorityDefault(Symbols.GOAL /* though these will be used for beliefs */);
        this.feedbackDurability = n.durabilityDefault(Symbols.GOAL /* though these will be used for beliefs */);
        this.motor = motor;

    }

    /** allow no eternal beliefs, and ONE eternal goal */
    @Override protected void beliefCapacity(ConceptPolicy p) {
        beliefCapacityNonEternal(this, p, 1);
        goalCapacityOneEternal(this, p, 1);
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




    /**
     * change the motor function
     */
    public final void setMotor(@NotNull MotorFunction motor) {
        this.motor = motor;
    }



    @Override
    public final void accept(@NotNull NAR nar) {
        //super.accept(nar);
        pendingRun = false; //HACK

        long now = nar.time();
        @Nullable Truth d = this.desire(now+motorDT);
        @Nullable Truth b = this.belief(now+motorDT);

        Truth feedback = motor.motor(b, d);
        if (feedback!=null)
            nar.input(feedback(feedback, now));
    }

    protected final Task feedback(Truth t, long when) {
        return new MutableTask(term(), Symbols.BELIEF, t)
                .time(when, when)
                .budget(feedbackPriority, feedbackDurability)
                .log("Motor Feedback");
    }

    @Override
    protected boolean beliefModificationRequiresUpdate(@NotNull Task t, @NotNull NAR nar) {
        //always update (calling .accept(nar) ) after every change
        return true;
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
