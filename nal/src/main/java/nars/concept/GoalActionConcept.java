package nars.concept;

import jcog.Util;
import jcog.data.FloatParam;
import nars.$;
import nars.NAR;
import nars.NAct;
import nars.Task;
import nars.term.Compound;
import nars.truth.Truth;
import nars.util.signal.Signal;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.LongSupplier;
import java.util.stream.Stream;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;


/**
 * ActionConcept which is driven by Goals that are interpreted into feedback Beliefs
 */
public class GoalActionConcept extends ActionConcept {

    public static final float CURIOSITY_CONF_FACTOR = 0.5f;
    public final Signal feedback;
    public final Signal feedbackGoal;

    private final FloatParam curiosity;


    @NotNull
    private MotorFunction motor;

    public GoalActionConcept(@NotNull Compound c, @NotNull NAct act, @NotNull MotorFunction motor) {
        this(c, act.nar(), act.curiosity(), motor);
    }

    public GoalActionConcept(@NotNull Compound c, @NotNull NAR n, FloatParam curiosity, @NotNull MotorFunction motor) {
        super(c,
                new SensorBeliefTable(n.terms.conceptBuilder().newTemporalBeliefTable(c)),
                new SensorBeliefTable(n.terms.conceptBuilder().newTemporalBeliefTable(c)),
                //null,
                n);

        this.curiosity = curiosity;
        this.feedback = new Signal(BELIEF, resolution).pri(() -> n.priorityDefault(BELIEF));
        this.feedbackGoal = new Signal(GOAL, resolution).pri(() -> n.priorityDefault(GOAL));

        this.motor = motor;
        //this.goals = newBeliefTable(nar, false); //pre-create

    }


    public GoalActionConcept resolution(float r) {
        resolution.setValue(r);
        return this;
    }

    @Override
    public Stream<Task> apply(NAR nar) {


        //int dur = nar.dur();
        long now = nar.time();

        Truth goal = this.goals().truth(now, nar);

        //float curiPeriod = 2; //TODO vary this
        float cur = curiosity.floatValue();
        if (nar.random().nextFloat() < cur) {
            // curiosity override

            float curiConf =
                    //nar.confDefault(GOAL);
                    Math.max(goal!=null ? Math.min(nar.confDefault(GOAL),goal.conf()) * CURIOSITY_CONF_FACTOR : 0, nar.confMin.floatValue());

                    //nar.confMin.floatValue()*2f;

//            float cc =
//                    //curiConf;
//                    curiConf - (goal != null ? goal.conf() : 0);
//            if (cc > 0) {

            float f =
                    Util.round(nar.random().nextFloat(), resolution.floatValue());
//                    ((float)Math.sin(
//                        hashCode() /* for phase shift */
//                            + now / (curiPeriod * (2 * Math.PI) * dur)) + 1f)/2f;

            goal = $.t(f, curiConf);

//                Truth ct = $.t(f, cc);
//                goal = ct; //curiosity overrides goal

//                if (goal == null) {
//                    goal = ct;
//                } else {
//                    goal = Revision.revise(goal, ct);
//                }
        }


        Truth belief = this.beliefs().truth(now, nar);

//        //HACK try to improve this
//        //if (goal == null) goal = belief; //use belief state, if exists (latch)
//        boolean kickstart;
//        if (((goal == null) || (goal.conf() < nar.confMin.floatValue())) && (feedback.current!=null)) {
//            goal = $.t(feedback.current.truth.freq, nar.confMin.floatValue()); //if null, use the last feedback value (latch)
//            kickstart = true;
//        } else {
//            kickstart = false;
//        }

        Truth beliefFeedback = this.motor.motor(belief, goal);
//        if (fbt==null && belief!=null) {
//            fbt = belief;
//        }

        //1. check feedback
        //2. check current belief
        //3. check previous signal belief
        //beliefFeedback != null ? beliefFeedback : belief; //latch

        LongSupplier stamper = nar.time::nextStamp;

        Task fb = feedback.set(this, beliefFeedback, stamper, nar);
        ((SensorBeliefTable) beliefs).commit(fb);


//        //HACK insert shadow goal
        Task fg = feedbackGoal.set(this, goal, stamper, nar);
        ((SensorBeliefTable) goals).commit(fg);

        //Task fg = null;

        //apply additional forgetting for past goals and beliefs, to promote future predictions
//        float decayFactor = 1f - 1f/Math.max(1 + beliefs().capacity(), goals().capacity());
//        Consumer<Task> decay = x -> {
//            if (x.end() < now) {
//                x.priMult(decayFactor);
//            }
//        };
//        beliefs().forEachTask(decay);
//        goals().forEachTask(decay);

        return Stream.of(fb, fg).filter(Objects::nonNull);
        //return Stream.of(fb, fg).filter(Objects::nonNull);
    }



    //    Truth[] linkTruth(long when, long now, float minConf) {
//        List<Truth> belief = $.newArrayList(0);
//        List<Truth> goal = $.newArrayList(0);
//
//        int dur = nar.dur();
//
//        int numTermLinks = termlinks().size();
//        if (numTermLinks > 0) {
//            float termLinkFeedbackRate = 1f / numTermLinks; //conf to priority boost conversion rate
//            termlinks().forEach(tll -> {
//                float g = linkTruth(tll.get(), belief, goal, when, now, dur);
//                if (g > 0)
//                    tll.priAdd(g * termLinkFeedbackRate);
//            });
//        }
//        int numTaskLinks = tasklinks().size();
//        if (numTaskLinks > 0) {
//            float taskLinkFeedbackRate = 1f / numTaskLinks; //conf to priority boost conversion rate
//            tasklinks().forEach(tll -> {
//                Task task = tll.get();
//                if (!task.isDeleted()) {
//                    float g = linkTruth(task.term(), belief, goal, when, now, dur);
//                    if (g > 0)
//                        tll.priAdd(g * taskLinkFeedbackRate);
//                }
//            });
//        }
//
//
//        Truth b = Revision.revise(belief, minConf);
//        Truth g = Revision.revise(goal, minConf);
//        //System.out.println(belief.size() + "=" + b + "\t" + goal.size() + "=" + g);
//
//        return new Truth[]{b, g};
//
//    }
//
//    private float linkTruth(Term t, List<Truth> belief, List<Truth> goal, long when, long now, int dur) {
//        float gain = 0;
//
//        t = nar.post(t);
//
//        Compound thisTerm = term();
//        if (t.op() == IMPL) {
//            //    B, (A ==> C), task(positive), time(decomposeBelief) |- subIfUnifiesAny(C,A,B), (Belief:Deduction, Goal:Induction)
//
//            Compound ct = (Compound) t;
//            Term postCondition = ct.term(1);
//
//            if (postCondition.equals(thisTerm)) {
//
//
//                //a termlink to an implication in which the postcondition is this concept
//                Concept implConcept = nar.concept(t);
//                if (implConcept != null) {
//
//                    //TODO match the task and subtract the dt
//                    Task it = implConcept.beliefs().match(when, now, dur); //implication belief
//                    if (it != null) {
//                        int dt = it.dt();
//                        if (dt == DTERNAL)
//                            dt = 0;
//
//                        Truth itt = it.truth();
//                        Term preCondition = nar.post(ct.term(0));
//
//                        gain += linkTruthImpl(itt, preCondition, when - dt, now, belief, goal, nar);
//                    }
//                }
//            }
//        } else if (t.op() == CONJ) {
//            //TODO
//        } else if (t.op() == EQUI) {
//            //TODO
//            Compound c = (Compound) t;
//            Term other = null;
//            boolean first = false;
//
//            //TODO handle negated case
//
//            if (c.term(0).equals(thisTerm)) {
//                other = c.term(1);
//                first = true;
//            } else if (c.term(1).equals(thisTerm)) {
//                other = c.term(0);
//                first = false;
//            }
//
//            if (other != null && !other.equals(thisTerm)) {
//
//                //a termlink to an implication in which the postcondition is this concept
//                Concept equiConcept = nar.concept(t);
//                if (equiConcept != null) {
//
//                    //TODO refactor to: linkTruthEqui
//
//
//                    //TODO match the task and subtract the dt
//                    Task it = equiConcept.beliefs().match(when, now, dur); //implication belief
//                    if (it != null) {
//                        int dt = it.dt();
//                        if (dt == DTERNAL)
//                            dt = 0;
//                        if (!first)
//                            dt = -dt;
//
//                        long whenActual = when + dt;
//
//                        Truth itt = it.truth();
//
//
//                        Concept otherConcept = nar.concept(other);
//                        if (otherConcept != null) {
//                            //    B, (A <=> C), belief(positive), time(decomposeBelief), neqCom(B,C) |- subIfUnifiesAny(C,A,B), (Belief:Analogy, Goal:Deduction)
//
//                            Truth pbt = otherConcept.belief(whenActual, now, nar.dur());
//                            if (pbt != null) {
//                                Truth y = TruthFunctions.analogy(pbt, itt, 0);
//                                if (y != null) {
//                                    belief.add(y);
//                                    gain += y.conf();
//                                }
//                            }
//
//                            Truth pgt = otherConcept.belief(whenActual, now, nar.dur());
//                            if (pgt != null) {
//                                Truth y = TruthFunctions.deduction(pbt, itt, 0);
//                                if (y != null) {
//                                    goal.add(y);
//                                    gain += y.conf();
//                                }
//
//                            }
//
//                        }
//                    }
//                }
//            }
//        }
//
//        return gain;
//    }
//
//
//    public static float linkTruthImpl(Truth itt, Term preCondition, long when, long now, List<Truth> belief, List<Truth> goal, NAR nar) {
//        float gain = 0;
//
//        boolean preCondNegated = preCondition.op() == NEG;
//
//        Concept preconditionConcept = nar.concept(preCondition);
//        if (preconditionConcept != null) {
//
//            //belief = deduction(pbt, it)
//            Truth pbt = preconditionConcept.belief(when, now, nar.dur());
//            if (pbt != null) {
//                Truth y = TruthFunctions.deduction(pbt.negIf(preCondNegated), itt, 0 /* gather anything */);
//                if (y != null) {
//                    belief.add(y);
//                    gain += y.conf();
//                }
//            }
//
//            //goal = induction(pgt, it)
//            Truth pgt = preconditionConcept.goal(when, now, nar.dur());
//            if (pgt != null) {
//                Truth y = TruthFunctions.induction(pgt.negIf(preCondNegated), itt, 0, /* gather anything */dur);
//                if (y != null) {
//                    goal.add(y);
//                    gain += y.conf();
//                }
//            }
//
//        }
//
//        return gain;
//    }

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
