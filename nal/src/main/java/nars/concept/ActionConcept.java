package nars.concept;

import nars.*;
import nars.table.HijackTemporalBeliefTable;
import nars.table.HijackTemporalExtendedBeliefTable;
import nars.task.Revision;
import nars.term.Compound;
import nars.term.Term;
import nars.truth.Truth;
import nars.truth.TruthDelta;
import nars.truth.TruthFunctions;
import nars.util.signal.ScalarSignal;
import nars.util.signal.SignalTask;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.list.MutableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;
import java.util.function.Function;

import static nars.$.$;
import static nars.$.t;
import static nars.Op.*;
import static nars.time.Tense.DTERNAL;


/** TODO make extend SensorConcept and utilize that for feedback control */
public class ActionConcept extends WiredConcept implements FloatFunction<Term>, Function<NAR,Task> {


    /** relative temporal delta time for desire/belief prediction */
    static final int decisionDT = 0;

    public final ScalarSignal feedback;

    private float feedbackConf;

    private float currentFeedback;


    private final boolean updateOnBeliefChange = false;


    @NotNull
    @Deprecated protected final NAR nar;

    private Truth lastGoal, lastBelief;


    protected class MyListTemporalBeliefTable extends HijackTemporalExtendedBeliefTable {

        public MyListTemporalBeliefTable(int tCap, int historicCap, Random r) {
            super(tCap, historicCap, r);
        }

        @Override
        protected Task ressurect(Task t) {
            t.budget().setPriority(feedback.pri.asFloat());
            return t;
        }

        @Override
        protected boolean save(Task t) {
            if (t.isBelief())
                return t instanceof SignalTask;
            else
                return true; //accept all goals
        }

        @Override
        protected void feedback(MutableList<Task> l, @NotNull Task inserted) {
            //ignore feedback here
        }
    }
    @Override
    public HijackTemporalBeliefTable newTemporalTable(int tCap, NAR nar) {
        //TODO only for Beliefs; Goals can remain normal
        return new MyListTemporalBeliefTable(tCap, tCap * 2, nar.random);
    }

    @Override
    public Task apply(NAR nar) {

        long now = nar.time();


        long then = now + decisionDT;

        Truth tdb, tdg;
        if (Param.ACTION_CONCEPT_LINK_TRUTH) {
            Truth[] td = linkTruth(then, now, nar.confMin.floatValue());
            tdb = td[0]; //NOT NECESSARY, SHOULD ONLY RELY ON THE FEEDBACK THIS ACTIONCONCEPT GENERATES ITSELF
            tdg = td[1];
        } else {
            tdb = tdg = null;
        }

        float dur = nar.time.dur();

        @Nullable Truth b = this.belief(then, now, dur);
//        if (tdb != null) {
//            b = (b != null) ? Revision.revise(b, tdb) : tdb;
//        }

        @Nullable Truth d = this.goal(then, now, dur);
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

        return feedback.apply(nar);
    }


//    @Override
//    protected void feedback(@NotNull Task input, @NotNull TruthDelta delta, @NotNull NAR nar, float deltaSatisfaction, float deltaConf) {
//        if (!input.isInput() && !(input instanceof AnswerTask))
//            System.out.println(input + ": " + this + ": " + delta + " " + deltaSatisfaction + " " + deltaConf);
//        super.feedback(input, delta, nar, deltaSatisfaction, deltaConf);
//    }

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

        this.nar = n;

        //assert (Op.isOperation(this));

        this.motor = motor;
        this.goals = newBeliefTable(nar, false); //pre-create

        //this.commonEvidence = Param.SENSOR_TASKS_SHARE_COMMON_EVIDENCE ? new long[] { n.time.nextStamp() } : LongArrays.EMPTY_ARRAY;

        feedback = new ScalarSignal(n, term, this, (x) ->
            t(x, feedbackConf)
        );
        feedback.pri(
            () -> n.priorityDefault(Op.BELIEF)
        );
    }


    @Override
    public @Nullable TruthDelta processBelief(@NotNull Task belief, @NotNull NAR nar) {
        //return super.processBelief(belief, nar);
        if (belief instanceof SignalTask) {
            return super.processBelief(belief, nar);
        }

        return null; //reject non-feedback generated beliefs

    }

    @Override
    public final float floatValueOf(Term anObject) {
        return this.currentFeedback;
    }


    Truth[] linkTruth(long when, long now, float minConf) {
        List<Truth> belief = $.newArrayList(0);
        List<Truth> goal = $.newArrayList(0);

        float dur = nar.time.dur();

        int numTermLinks = termlinks().size();
        if (numTermLinks > 0) {
            float termLinkFeedbackRate = 1f / numTermLinks; //conf to priority boost conversion rate
            termlinks().forEach(tll -> {
                float g = linkTruth(tll.get(), belief, goal, when, now, dur);
                if (g > 0)
                    tll.priAdd(g * termLinkFeedbackRate);
            });
        }
        int numTaskLinks = tasklinks().size();
        if (numTaskLinks > 0) {
            float taskLinkFeedbackRate = 1f / numTaskLinks; //conf to priority boost conversion rate
            tasklinks().forEach(tll -> {
                Task task = tll.get();
                if (!task.isDeleted()) {
                    float g = linkTruth(task.term(), belief, goal, when, now, dur);
                    if (g > 0)
                        tll.priAdd(g * taskLinkFeedbackRate);
                }
            });
        }


        Truth b = Revision.revise(belief, minConf);
        Truth g = Revision.revise(goal, minConf);
        //System.out.println(belief.size() + "=" + b + "\t" + goal.size() + "=" + g);

        return new Truth[] {b, g};

    }

    private float linkTruth(Term t, List<Truth> belief, List<Truth> goal, long when, long now, float dur) {
        float gain = 0;

        t = nar.post(t);

        Compound thisTerm = term();
        if (t.op() == IMPL) {
            //    B, (A ==> C), task(positive), time(decomposeBelief) |- subIfUnifiesAny(C,A,B), (Belief:Deduction, Goal:Induction)

            Compound ct = (Compound) t;
            Term postCondition = ct.term(1);

            if (postCondition.equals(thisTerm)) {


                //a termlink to an implication in which the postcondition is this concept
                Concept implConcept = nar.concept(t);
                if (implConcept!=null) {

                    //TODO match the task and subtract the dt
                    Task it = implConcept.beliefs().match(now, dur); //implication belief
                    if (it!=null) {
                        int dt = it.dt();
                        if (dt == DTERNAL)
                            dt = 0;

                        Truth itt = it.truth();
                        Term preCondition = nar.post(ct.term(0));

                        gain += linkTruthImpl(itt, preCondition, when - dt, now, belief, goal, nar);
                    }
                }
            }
        } else if (t.op() == CONJ) {
            //TODO
        } else if (t.op() == EQUI) {
            //TODO
            Compound c = (Compound)t;
            Term other = null;
            boolean first = false;

            //TODO handle negated case

            if (c.term(0).equals(thisTerm)) {
                other = c.term(1);
                first = true;
            } else if (c.term(1).equals(thisTerm)) {
                other = c.term(0);
                first = false;
            }

            if (other != null && !other.equals(thisTerm)) {

                //a termlink to an implication in which the postcondition is this concept
                Concept equiConcept = nar.concept(t);
                if (equiConcept!=null) {

                    //TODO refactor to: linkTruthEqui


                    //TODO match the task and subtract the dt
                    Task it = equiConcept.beliefs().match(now, dur); //implication belief
                    if (it!=null) {
                        int dt = it.dt();
                        if (dt == DTERNAL)
                            dt = 0;
                        if (!first)
                            dt = -dt;

                        long whenActual = when + dt;

                        Truth itt = it.truth();



                        Concept otherConcept = nar.concept(other);
                        if (otherConcept!=null) {
                            //    B, (A <=> C), belief(positive), time(decomposeBelief), neqCom(B,C) |- subIfUnifiesAny(C,A,B), (Belief:Analogy, Goal:Deduction)

                            Truth pbt = otherConcept.belief(whenActual, now, nar.time.dur());
                            if (pbt!=null) {
                                Truth y = TruthFunctions.analogy(pbt, itt, 0);
                                if (y!=null) {
                                    belief.add(y);
                                    gain += y.conf();
                                }
                            }

                            Truth pgt = otherConcept.belief(whenActual, now, nar.time.dur());
                            if (pgt!=null) {
                                Truth y = TruthFunctions.deduction(pbt, itt, 0);
                                if (y!=null) {
                                    goal.add(y);
                                    gain += y.conf();
                                }

                            }

                        }
                    }
                }
            }
        }

        return gain;
    }


    public static float linkTruthImpl(Truth itt, Term preCondition, long when, long now, List<Truth> belief, List<Truth> goal, NAR nar) {
        float gain = 0;

        boolean preCondNegated = preCondition.op()==NEG;

        Concept preconditionConcept = nar.concept(preCondition);
        if (preconditionConcept != null) {

            //belief = deduction(pbt, it)
            Truth pbt = preconditionConcept.belief(when, now, nar.time.dur());
            if (pbt!=null) {
                Truth y = TruthFunctions.deduction(pbt.negIf(preCondNegated), itt, 0 /* gather anything */);
                if (y!=null) {
                    belief.add(y);
                    gain += y.conf();
                }
            }

            //goal = induction(pgt, it)
            Truth pgt = preconditionConcept.goal(when, now, nar.time.dur());
            if (pgt!=null) {
                Truth y = TruthFunctions.induction(pgt.negIf(preCondNegated), itt, 0 /* gather anything */);
                if (y!=null) {
                    goal.add(y);
                    gain += y.conf();
                }
            }

        }

        return gain;
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
