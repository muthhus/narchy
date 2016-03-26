package nars.concept;

import nars.*;
import nars.bag.Bag;
import nars.nal.Tense;
import nars.nal.nal8.Execution;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Operator;
import nars.term.Termed;
import nars.util.data.MutableInteger;
import nars.util.event.Topic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;



/**
 * Has ability to measure (and cache) belief and desire state in order to execute Operations
 * and negations of Operations
 * <p>
 * motivation = desire - belief
 * motivation = (desirePositive - desireNegative) - (beliefPositive - beliefNegative)
 */
public class OperationConcept extends CompoundConcept implements Runnable {

    /**
     * cache for motivation calculation; set to NaN to invalidate
     */
    protected transient float believed;
    protected transient float desired;

    /**
     * set to Tense.ETERNAL to invalidate
     */
    protected long lastMotivationUpdate = Tense.ETERNAL;

    //TODO allocate this only for Operation (not negations)
    transient private final List<Task> pending = Global.newArrayList(0);

    public transient NAR nar;


    public OperationConcept(@NotNull Compound term, Bag<Termed> termLinks, Bag<Task> taskLinks) {
        super(term, termLinks, taskLinks);
    }

    public OperationConcept(@NotNull String compoundTermString, @NotNull NAR n) throws Narsese.NarseseException {
        super(compoundTermString, n);
        assert(Op.isOperation(term));
        n.on(this);
    }

    @Nullable
    @Override
    public final Task processGoal(@NotNull Task goal, @NotNull NAR nar) {
        return executeLater(super.processGoal(goal, nar), nar);
    }

    @Nullable
    @Override
    public Task processBelief(@NotNull Task belief, @NotNull NAR nar) {
        return executeLater(super.processBelief(belief, nar), nar);
    }

    @Nullable
    private final Task executeLater(@Nullable Task t, @NotNull NAR nar) {
        if (t == null) return null;

        //if (op()!=NEGATE) {
            pending.add(t);
            nar.runOnceLater(this);
            this.nar = nar;
        /*} else {
            nar.runOnceLater(positive(nar)); //queue an update on the positive concept but dont queue the negation task
        }*/
        return t;
    }

    @Override
    public void run() {
        final NAR nar = this.nar;

        if (update(nar)) {
            List<Task> pending = this.pending;
            for (int i = 0, pendingSize = pending.size(); i < pendingSize; i++) {
                execute(pending.get(i), nar);
            }
        }

        pending.clear();
    }

    public boolean update(@NotNull NAR nar) {
        long now = nar.time();

        float b = 0, d = 0;

        if (now != lastMotivationUpdate) { //update once per cycle TODO parameter for this limitation min/max


            int dur = nar.duration();

            //OperationConcept p = positive(nar);
            //Concept n = negative(nar);


            //if (p != null) { //measure contributed positive state
                d += goalMotivation(now, dur);
                b += beliefMotivation(now, dur);
            //}

//            if (n != null) {  //measure contributed negative state
//                d -= n.goalMotivation(now, dur);
//                b -= n.beliefMotivation(now, dur);
//            }

            update(b, d, now); //only necessary to update the state in the positive only
        } else {
            d = desired;
            b = believed;
        }

        return (d - b) > nar.executionThreshold.floatValue();
    }


    /**
     * Entry point for all potentially executable tasks.
     * Enters a task and determine if there is a decision to execute.
     * The goal has already successfully been inserted to belief table.
     * the job here is to update the resulting motivation state
     */
    final void execute(@NotNull Task task, @NotNull NAR nar) {


        //        if (motivation < executionThreshold.floatValue())
//            return false;

        //if (task.op() != NEGATE) {

            //emit for both beliefs and goals
            Topic<Task> tt = nar.concept(Operator.operator(this)).get(Execution.class);
            if (tt != null && !tt.isEmpty()) {
                //beforeNextFrame( //<-- enqueue after this frame, before next
                tt.emit(task);
            }


            //call Task.execute only for goals
            if (task.isGoal()) {
                float b = believed;
                float d = desired;
                if (Global.DEBUG)
                    task.log("execute(b=" + b + ",d=" + d + ')');
                task.execute(b, d, nar); //call the task's custom event handler
            }
        //}
    }

    //    private final boolean updateNecessary(long now) {
//        long last = this.lastMotivationUpdate;
//        return (last == ETERNAL) || ((now - last) > 0);
//    }
//
    /** set the belief/desire state */
    protected void update(float b, float d, long now) {
        this.believed = b;
        this.desired = d;
        this.lastMotivationUpdate = now;
    }

//    public OperationConcept positive(NAR n) {
//        return op() != NEGATE ? this : (OperationConcept) n.concept(term(0));
//    }
//
//    public Concept negative(NAR n) {
//        //TODO cache the opposite term
//        return op() != NEGATE ? n.concept($.neg(this)) : this;
//    }

//    public final float believed() {
//        return believed;
//    }
//
//    public final float desired() {
//        return desired;
//    }

    public final float motivation() {
        float b = this.believed;
        float d = this.desired;
        return (d - b); // / (Math.abs(d) + Math.abs(b));
    }

    /** provide motivation value after triggering an update */
    public final float motivation(@NotNull NAR nar) {
        update(nar);
        return motivation();
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
