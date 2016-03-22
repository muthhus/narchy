package nars.concept;

import nars.$;
import nars.Global;
import nars.NAR;
import nars.bag.Bag;
import nars.nal.Tense;
import nars.nal.nal8.Execution;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.util.event.Topic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.Op.NEGATE;
import static nars.nal.Tense.ETERNAL;

/**
 * Has ability to measure (and cache) belief and desire state in order to execute Operations
 * and negations of Operations
 *
 * motivation = desire - belief
 * motivation = (desirePositive - desireNegative) - (beliefPositive - beliefNegative)
 */
public class OperationConcept extends CompoundConcept {

    /**
     * cache for motivation calculation; set to NaN to invalidate
     */
    protected transient float believed = Float.NaN;
    protected transient float desired = Float.NaN;

    /**
     * set to Tense.ETERNAL to invalidate
     */
    protected long lastMotivationUpdate = Tense.ETERNAL;


    public OperationConcept(@NotNull Compound term, Bag<Termed> termLinks, Bag<Task> taskLinks) {
        super(term, termLinks, taskLinks);
    }

    @Nullable
    @Override
    public final Task processGoal(@NotNull Task inputGoal, @NotNull NAR nar) {
        Task g = super.processGoal(inputGoal, nar);
        if (g!=null) {
            if ((op() != NEGATE)) { //negated operation TODO term it as 'opposite' field
                execute(g, this, negative(nar), true, nar);
            } else { //unwrapped operation TODO term it as 'opposite' field
                execute(g, positive(nar), this, false, nar);
            }
        }
        return g;
    }

    /**
     * Entry point for all potentially executable tasks.
     * Enters a task and determine if there is a decision to execute.
     * The goal has already successfully been inserted to belief table.
     * the job here is to update the resulting motivation state
     */
    static final void execute(Task goal, OperationConcept positive, Concept negative, boolean polarity, NAR nar) {

        float b, d;

        long now = nar.time();
        if (positive.updateNecessary(now)) {

            b = d = 0;

            if (positive != null) {
                //measure contributed positive state
            }

            if (negative != null) {
                //measure contributed negative state
            }

            positive.update(b, d, now); //only necessary to update the state in the positive only
        } else {
            //use cached value
            b = positive.believed;
            d = positive.desired;
        }

        if (Global.DEBUG)
            goal.log("execute(b=" + b + ",d=" + d + ')');


        //        if (motivation < executionThreshold.floatValue())
//            return false;

        if (positive!=null) {
            Topic<Task> tt = positive.get(Execution.class);
            if (tt != null && !tt.isEmpty()) {
                //beforeNextFrame( //<-- enqueue after this frame, before next
                tt.emit(goal);
            }
        }

        goal.execute(b, d, nar); //call the task's custom event handler
    }

    private final boolean updateNecessary(long now) {
        long last = this.lastMotivationUpdate;
        return (last == ETERNAL) || ((now - last) > 0);
    }

    private final void update(float b, float d, long now) {
        this.believed = b;
        this.desired = d;
        this.lastMotivationUpdate = now;
    }

    public OperationConcept positive(NAR n) {
        return op() == NEGATE ? (OperationConcept) n.concept(term(0)) : this;
    }
    public Concept negative(NAR n) {
        return op() != NEGATE ? n.concept($.neg(this)) : this;
    }

    public float believed(NAR n) {
        return positive(n).believed;
    }
    public float desired(NAR n) {
        return positive(n).desired;
    }



    //        if (!Op.isOperation(goalTerm))
//            return false;
//            if (goalTerm.op()==Op.PRODUCT) {
//                @NotNull Compound x = inputGoal.term();
//                try {
//                    Term y = rt.eval(x);
//                    if (y != null) {
//                        logger.info("(eval( {} , {} )", x, y); //mooseboobs
//                        return true;
//                    }
//                }
//                /*catch (VerifyError vex) {
//                    //ex: java.lang.VerifyError: (class: clojure/core$eval1, method: invokeStatic signature: ()Ljava/lang/Object;) Unable to pop operand off an empty stack
//                }*/ catch (Throwable e) {
//                    //HACK
//                    logger.warn("eval {}", e);
//
//                }
//            }



//
//        //Normal Goal
//        long now = nar.time();
//        Task projectedGoal = goals().top(now);
//        float motivation = projectedGoal.motivation();
//
//        //counteract with content from any (--, concept
//        Term antiTerm = $.neg(projectedGoal.term());
//        Concept antiConcept = nar.concept(antiTerm);
//        if (antiConcept!=null)
//            motivation -= antiConcept.motivationElse(now, 0);
//

//
//        long occ = projectedGoal.occurrence();
//        if ((!((occ == ETERNAL) || (Math.abs(occ-now) < nar.duration()*2)))//right timing
//                ) { //sufficient motivation
//            return false;
//        }
//
//        goal = projectedGoal;


    //DEFAULT EXECUTION PROCEDURE: trigger listener reactions
//        Topic<Task> tt = n.exe.get(
//            Operator.operator(term())
//        );


    //float delta = updateSuccess(goal, successBefore, memory);

        //&& (goal.state() != Task.TaskState.Executed)) {

            /*if (delta >= Global.EXECUTION_SATISFACTION_TRESHOLD)*/

        //Truth projected = goal.projection(now, now);


//                        LongHashSet ev = this.lastevidence;
//
//                        //if all evidence of the new one is also part of the old one
//                        //then there is no need to execute
//                        //which means only execute if there is new evidence which suggests doing so1
//                        if (ev.addAll(input.getEvidence())) {

//                            //TODO more efficient size limiting
//                            //lastevidence.toSortedList()
//                            while(ev.size() > max_last_execution_evidence_len) {
//                                ev.remove( ev.min() );
//                            }
//                        }


}
