package nars.concept;

import jcog.math.FloatSupplier;
import nars.*;
import nars.task.Revision;
import nars.term.Compound;
import nars.term.Term;
import nars.truth.Truth;
import nars.truth.TruthFunctions;
import nars.util.signal.ScalarSignal;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

import static nars.$.$;
import static nars.$.t;
import static nars.Op.IMPL;
import static nars.Op.NEG;


/** TODO make extend SensorConcept and utilize that for feedback control */
public class ActionConcept extends WiredConcept implements WiredConcept.Prioritizable, Runnable, FloatFunction<Term>, Consumer<Task> {


    /** relative temporal delta time for desire/belief prediction */
    static final int decisionDT = 0;

    public final ScalarSignal feedback;
    private float feedbackConf;

    private float currentFeedback;

    public FloatSupplier pri;

    private final boolean updateOnBeliefChange = false;

    @Override
    public void pri(FloatSupplier v) {
        this.pri = v;
    }
    private Truth lastGoal, lastBelief;

    @Override
    public void accept(Task feedback) {
        nar.input(feedback);
    }

    public ActionConcept feedbackResolution(float res) {
        feedback.resolution(res);
        return this;
    }


    /** determines the feedback belief when desire or belief has changed in a MotorConcept
     *  implementations may be used to trigger procedures based on these changes.
     *  normally the result of the feedback will be equal to the input desired value
     *  although this may be reduced to indicate that the motion has hit a limit or
     *  experienced resistence
     * */
    @FunctionalInterface  public interface MotorFunction  {

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

    public ActionConcept(@NotNull Compound term, @NotNull NAR n, @NotNull MotorFunction motor) {
        super(term, n);

        //assert (Op.isOperation(this));

        this.motor = motor;
        this.goals = newBeliefTable(nar, false); //pre-create

        //this.commonEvidence = Param.SENSOR_TASKS_SHARE_COMMON_EVIDENCE ? new long[] { n.time.nextStamp() } : LongArrays.EMPTY_ARRAY;

        feedback = new ScalarSignal(n, term, this, (x) ->
            t(x, feedbackConf),
            this
        );
        feedback.pri(
                () -> n.priorityDefault(Op.BELIEF)
        );
    }

    @Override
    public final float floatValueOf(Term anObject) {
        return this.currentFeedback;
    }


    Truth[] truthLinked(long when, long now, float minConf) {
        List<Truth> belief = $.newArrayList(0);
        List<Truth> goal = $.newArrayList(0);

        float dur = nar.time.dur();

        termlinks().forEach(tll->{
            Term t = tll.get();
            if (t.op() == IMPL) {
                //    B, (A ==> C), task(positive), time(decomposeBelief) |- subIfUnifiesAny(C,A,B), (Belief:Deduction, Goal:Induction)

                Compound ct = (Compound) t;
                Term postCondition = ct.term(1);
                if (postCondition.equals(term())) {
                    //a termlink to an implication in which the postcondition is this concept
                    Concept implConcept = nar.concept(t);
                    if (implConcept!=null) {

                        Truth it = implConcept.belief(when, now, dur); //belief truth of the implication
                        if (it!=null) {

                            Term preCondition = ct.term(0);
                            boolean preCondNegated = preCondition.op()==NEG;


                            Concept preconditionConcept = nar.concept(preCondition);
                            if (preconditionConcept != null) {

                                //belief = deduction(pbt, it)
                                Truth pbt = preconditionConcept.belief(when, now, dur);
                                if (pbt!=null) {
                                    Truth y = TruthFunctions.deduction(pbt.negIf(preCondNegated), it, minConf);
                                    if (y!=null)
                                        belief.add(y);
                                }

                                //goal = induction(pgt, it)
                                Truth pgt = preconditionConcept.goal(when, now, dur);
                                if (pgt!=null) {
                                    Truth y = TruthFunctions.induction(pgt.negIf(preCondNegated), it, minConf);
                                    if (y!=null)
                                        goal.add(y);
                                }

                            }
                        }
                    }
                }
            }
        });

        return new Truth[] {Revision.revise(belief, minConf), Revision.revise(goal, minConf)};
    }



//    @Override
//    protected BeliefTable newBeliefTable(NAR nar, boolean beliefOrGoal, int eCap, int tCap) {
//        if (beliefOrGoal) {
//            //belief
//            return super.newBeliefTable(nar, beliefOrGoal, eCap, tCap);
//        } else {
//            //goal
//            return new DefaultBeliefTable(
//                    newEternalTable(eCap),
//                    newTemporalTable(tCap, nar)
//            ) {
//
//        }
//
//    }

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

//    @Override
//    protected final boolean runLater(@NotNull Task t, @NotNull NAR nar) {
//        //return hasGoals();
//        return true; //will run automatically each frame, as set in constructor
//    }


    /**
     * change the motor function
     */
    public final void setMotor(@NotNull MotorFunction motor) {
        this.motor = motor;
    }


    @Override
    public void run() {
        long now = nar.time();


        long then = now + decisionDT;

        Truth[] td = truthLinked(then, now, nar.confMin.floatValue());
        Truth tdb = td[0];

        float dur = nar.time.dur();

        @Nullable Truth b = this.belief(then, now, dur);
        if (tdb != null) {
            b = (b != null) ? Revision.revise(b, tdb) : tdb;
        }

        @Nullable Truth d = this.goal(then, now, dur);
        Truth tdg = td[1];
        if (tdg!=null) {
            d = (d != null) ? Revision.revise(d, tdg) : tdg;
        }



        boolean noDesire = d == null;
        boolean goalChange =   (noDesire ^ lastGoal == null) || (!noDesire && !d.equals(lastGoal));
        lastGoal = d;

        boolean noBelief = b == null;
        boolean beliefChange = (noBelief ^ lastBelief == null) || (!noBelief && !b.equals(lastBelief));
        lastBelief = b;


        if (goalChange || (updateOnBeliefChange && beliefChange)) {

            Truth f = this.motor.motor(b, d);
            if (f!=null) {
                this.currentFeedback = f.freq(); //HACK ignores the conf component
                this.feedbackConf = f.conf();
            } else {
                this.currentFeedback = Float.NaN;
            }


//            if (feedback != null) {
//                //if feedback is different from last
//                if (nextFeedback == null || !nextFeedback.equalsTruth(feedback, feedbackResolution)) {
//                    this.nextFeedback = feedback(feedback, now + feedbackDT);
//                    nar.input(nextFeedback);
//                }
//            }
        }

        feedback.accept(nar);
    }



//    @NotNull
//    @Override
//    protected BeliefTable newBeliefTable(int eCap, int tCap) {
//        return new SensorBeliefTable(tCap);
//    }
//
//    private final class SensorBeliefTable extends DefaultBeliefTable {
//
//        public SensorBeliefTable(int tCap) {
//            super(tCap);
//        }
//
//        @Override
//        public Truth truth(long when, long now) {
////            if (when == now || when == ETERNAL)
////                return sensor.truth();
//
//            // if when is between the last input time and now, evaluate the truth at the last input time
//            // to avoid any truth decay across time. this emulates a persistent latched sensor value
//            // ie. if it has not changed
//            if (nextFeedback !=null && when <= now && when >= nextFeedback.occurrence()) {
//                //now = when = sensor.lastInputTime;
//                return nextFeedback.truth();
//            } else {
//                return super.truth(when, now);
//            }
//        }
//
//        @Override
//        public Task match(@NotNull Task target, long now) {
//            long when = target.occurrence();
//
//            Task f = ActionConcept.this.nextFeedback;
//            if (f !=null && when <= now && when >= f.occurrence()) {
//                //but project it to the target time unchanged
//                return MutableTask.clone(f, now);
//            }
//
//            return super.match(target, now);
//        }
//
//        //        @Override
////        public Task match(@NotNull Task target, long now) {
////            long when = target.occurrence();
////            if (when == now || when == ETERNAL) {
////                sensor.
////                return sensor.truth();
////            }
////
////            return super.match(target, now);
////        }
//    }
//



}
