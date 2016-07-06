package nars.concept;

import nars.$;
import nars.NAR;
import nars.Narsese;
import nars.bag.Bag;
import nars.nal.nal8.Execution;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.container.TermContainer;
import nars.util.event.Topic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;


/**
 * Has ability to measure (and cache) belief and desire state in order to execute Operations
 * and negations of Operations

 */
public class OperationConcept extends CompoundConcept implements Consumer<NAR> {

    protected volatile boolean pendingRun;


    public OperationConcept(@NotNull Compound term, Bag<Term> termLinks, Bag<Task> taskLinks) {
        super(term, termLinks, taskLinks);
    }

    public OperationConcept(@NotNull Compound term, @NotNull NAR n) throws Narsese.NarseseException {
        super(term, n);
        n.on(this);
    }

    public OperationConcept(@NotNull String compoundTermString, @NotNull NAR n) throws Narsese.NarseseException {
        this($.$(compoundTermString), n);
    }


    /* subj contains the parameter product */
    public final TermContainer parameters() {
        return ((Compound)term().term(0)).subterms();
    }

    @Nullable
    @Override
    public Task processGoal(@NotNull Task goal, @NotNull NAR nar) {
        return executeLater(super.processGoal(goal, nar), nar);
    }

    @Nullable
    @Override
    public Task processBelief(@NotNull Task belief, @NotNull NAR nar) {
        return executeLater(super.processBelief(belief, nar), nar);
    }

    @Nullable
    private final Task executeLater(@Nullable Task t, @NotNull NAR nar) {
        if (t != null) {

            if (!pendingRun && beliefModificationRequiresUpdate(t, nar)) {
                pendingRun = true;
                nar.runLater(this);
            }
        }

        return t;
    }

    protected boolean beliefModificationRequiresUpdate(@NotNull Task t, NAR nar) {
        return hasGoals() && operationExec(operationConcept(nar))!=null;
    }

    /** called between frames if belief or goal state has changed */
    @Override public void accept(@NotNull NAR nar) {
        pendingRun = false;

        Topic<OperationConcept> tt = operationExec(nar);
        if (tt != null) {
            //beforeNextFrame( //<-- enqueue after this frame, before next
            tt.emit(this);
        }

    }

    public @Nullable Topic<OperationConcept> operationExec(NAR nar) {
        return operationExec(operationConcept(nar));
    }

    public @Nullable
    static Topic<OperationConcept> operationExec(Concept<?> c) {
        return c != null ? c.get(Execution.class) : null;
    }

    public @Nullable final Concept operationConcept(NAR nar) {
        return nar.concept(
            term().term(1) //operator is the predicate
        );
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


//    /** provide motivation value after triggering an update */
//    public final float motivation(@NotNull NAR nar) {
//        update(nar);
//
////        float bf = believed.freq();
////        float bc = believed.conf();
////        float df = desired.freq();
////        float dc = desired.conf();
//
//        //expectation = (confidence * (frequency - 0.5f) + 0.5f);
//
//
//        /*return
//                 UtilityFunctions.or(
//                    ((desired.conf() * (desired.freq()-0.5f)) + 0.5f),
//                    1f - ((believed.conf() * ((believed.freq())-0.5f ))  + 0.5f)
//                 );*/
//
////        return UtilityFunctions.or(desired.conf(), believed.conf()) *
////                ((UtilityFunctions.aveAri(desired.freq(), (1f - believed.freq())) - 0.5f)
////                ) + 0.5f;
//
//
//        float d = (desired.expectation()-0.5f);
//        if (d < 0) return d;
//        float b = (believed.expectation()-0.5f);
//        /*if (b > 0)*/ d-=b;
//        //float beliefAttenuation = 1f - Math.max(0, ((believed.expectation()) - 0.5f) * 2f);
//        //d *= beliefAttenuation;
//        return d*2;
//    }




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
