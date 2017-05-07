/*
 * Operator.java
 *
 * Copyright (C) 2008  Pei Wang
 *
 * This file is part of Open-NARS.
 *
 * Open-NARS is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Open-NARS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Open-NARS.  If not, see <http://www.gnu.org/licenses/>.
 */

package nars.op;

import javafx.util.Pair;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.container.TermContainer;
import nars.term.container.TermVector;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

import static nars.Op.*;
import static nars.time.Tense.ETERNAL;

/**
 * interface which defines the behavior for processing functor tasks
 */
@FunctionalInterface public interface Operator {

    static Term[] args(Task t) {
        return ((Compound)(t.term(0)/*subject*/)).toArray();
    }

    /**
     *
     * @param t task being processed
     * @return the task to continue processing, or null to cancel
     */
    @Nullable Task run(@NotNull Task t, @NotNull NAR nar);


    /** if goal, automatically generates a corresponding feedback belief in the eternal, present, or future. */
    static Operator auto(BiConsumer<Task /* Goal */, Task /* Belief (feedback) */> onExec) {
        return (g, nar) -> {
            if (g.punc() == GOAL) {
                Compound c = g.term();
                Task b = null;
                if (c.vars() == 0 && c.varPattern() == 0) {
                    long occurrence = g.start();
                    if (occurrence == ETERNAL || occurrence >= nar.time()) {
                        b = nar.believe(g.priSafe(0), c, occurrence, g.freq(), nar.confDefault(BELIEF));
                    }
                }
                onExec.accept(g, b);
            }
            return g;
        };
    }


//    @Override
//    public void accept(@NotNull OperationConcept exec) {
//
//        //only proceed with execution if positively motivated
//        if ((exec.goals().motivation(nar.time()) > 0))
//            execute(exec);
//
////        if (async()) {
////            //asynch
////            NAR.runAsync(() -> execute(exec));
////        } else {
//            //synchronous
//
//
//    }
//
//    /**
//     * Required method for every operate, specifying the corresponding
//     * operation
//     *
//     * @return The direct collectable results and feedback of the
//     * reportExecution
//     */
//
//    public abstract void execute(OperationConcept exec);
//
//
//
//    public final @Nullable Atomic operator() {
//        return atomicTerm;
//    }
//
//    /** this will be called prior to any execution */
//    public void init(NAR nar) {
//        this.nar = nar;
//    }


    /*
    <patham9_> when a goal task is processed, the following happens: In order to decide on whether it is relevant for the current situation, at first it is projected to the current time, then it is revised with previous "desires", then it is checked to what extent this projected revised desire is already fullfilled (which revises its budget) , if its below satisfaction threshold then it is pursued, if its an operation it is additionally checked if
    <patham9_> executed
    <patham9_> the consequences of this, to give examples, are a lot:
    <patham9_> 1 the system wont execute something if it has a strong objection against it. (example: it wont jump down again 5 meters if it previously observed that this damages it, no matter if it thinks about that situation again or not)
    <patham9_> 2. the system wont lose time with thoughts about already satisfied goals (due to budget shrinking proportional to satisfaction)
    <patham9_> 3. the system wont execute and pursue what is already satisfied
    <patham9_> 4. the system wont try to execute and pursue things in the current moment which are "sheduled" to be in the future.
    <patham9_> 5. the system wont pursue a goal it already pursued for the same reason (due to revision, it is related to 1)
    */



}


//package nars.term.atom;
//
//import nars.Op;
//import nars.term.Compound;
//import nars.term.Term;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
///**
// * the 1-arity '^' compound which wraps a term to
// * indicate an operator that can be used as the predicate
// * of an Operation, ex:
// *
// *      <(arg0, arg1) --> ^operator>
// *
// * This class also includes static utility methods for
// * working with Operation terms (which include an "Operator",
// * as shown above, but is not an "Operator").
// *
// */
//public class Operator<T extends Term> extends AtomicStringConstant {
//
//
//    public Operator(@NotNull T the) {
//        this(the.toString());
//    }
//
//    public Operator(@NotNull String id) {
//        super(id);
//        //super((id.charAt(0)!=Op.OPER.ch ? Op.OPER.ch + id : id));
//    }
//
//
//    /** returns the Product arguments compound of an operation. does not check if the input is actually an operation */
//    public static @Nullable Compound opArgs(@NotNull Compound operation) {
//        return (Compound) operation.term(0);
//    }
////    @Nullable
////    public static Compound opArgs(@NotNull Term x) {
////        if (x instanceof Compound)
////            return opArgs((Compound)x);
////        return null;
////    }
//
//
//    /** returns the terms array of the arguments of an operation. does not check if the input is actually an operation */
//    @NotNull public static Term[] argArray(@NotNull Compound term) {
//        return opArgs(term).terms();
//    }
//
//    /** returns the Operator predicate of an operation. */
//    @Nullable public static Atomic operator(@NotNull Compound operation) {
//        Term o = operation.term(1);
//        return (o.op() == Op.OPER) ? ((Atomic) o) : null;
//    }
//    @Nullable public static Atomic operator(@NotNull Term maybeOperation) {
//        if (maybeOperation instanceof Compound) {
//            Compound c = (Compound)maybeOperation;
//            if (c.size() == 2) {
//                if (c.isTerm(1,Op.OPER) && c.isTerm(0, Op.PROD)) {
//                    return (Atomic) c.term(1);
//                }
//            }
//        }
//        return null;
//    }
//
//    @NotNull
//    @Override
//    public Op op() {
//        return Op.OPER;
//    }
//
//}
//package nars.nal.nal8;
//
//import nars.*;
//import nars.concept.OperationConcept;
//
//import nars.term.Compound;
//import nars.term.Term;
//import nars.term.atom.Atomic;
//import nars.time.Tense;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import static nars.Op.PROD;
//
///**
// * Execution context which Operator implementations
// * receive, supporting any variety of synchronous/
// * asynchronous feedback, access to the invoking NAR,
// * and utility methods for extracting features
// * of the operation task in the context of the executing NAR.
// */
//public interface Execution  {
//
//    //private static final Logger logger = LoggerFactory.getLogger(Execution.class);
//
//    //float feedbackPriorityMultiplier = 1.0f;
//    //float feedbackDurabilityMultiplier = 1.0f;
//    //Variable defaultResultVariable = $.varDep("defaultResultVariable");
////    public final NAR nar;
////    public final Task task;
//
////    public Execution(NAR nar, Task task, Topic<Execution> listeners) {
////        this.nar = nar;
////        this.task = task;
////
//
////    }
//
//    static MutableTask resultTerm(@NotNull NAR nar, @NotNull OperationConcept operation, Term y/*, Term[] x0, Term lastTerm*/, @NotNull Tense tense) {
//
//
//        //Variable var=new Variable("$1");
//        //  Term actual_part = Similarity.make(var, y);
//        //  Variable vardep=new Variable("#1");
//        //Term actual_dep_part = Similarity.make(vardep, y);
////        operation=(Operation) operation.setComponent(0,
////                ((Compound)operation.getSubject()).setComponent(
////                        numArgs, y));
//
//        //Examples:
//        //      <3 --> (/,^add,1,2,_,SELF)>.
//        //      <2 --> (/,^count,{a,b},_,SELF)>. :|: %1.00;0.99%
//        //transform to image for perception variable introduction rule (is more efficient representation
//
//
//        //final int numArgs = x0.length;
//
//        Compound operationTerm = operation.term();
//        Compound x = (Compound) operationTerm.term(0);
//        if (x.op() != PROD)
//            throw new RuntimeException("invalid operation");
//
//        Term inh = resultTerm((Atomic) operationTerm.term(1), x, y);
//        if ((!(inh instanceof Compound))) {
//            //TODO wrap a non-Compound result as some kind of statement
//            return null;
//        }
//
//
//        return new TaskBuilder(inh, Symbols.BELIEF, 1f, nar)  //with default truth value
//                .time(tense, nar)
//                //.budget(goal.budget())
//                //.budgetScaled(feedbackPriorityMultiplier, feedbackDurabilityMultiplier)
//                .log("Execution Result")
//                ;
//
//
//        //Implication.make(operation, actual_part, TemporalRules.ORDER_FORWARD);
//            /*float equal = equals(lastTerm, y);
//            ArrayList<Task> rt = Lists.newArrayList(
//                    m.newTask(actual, Symbols.JUDGMENT_MARK,
//                            1.0f, confidence,
//                            Global.DEFAULT_JUDGMENT_PRIORITY,
//                            Global.DEFAULT_JUDGMENT_DURABILITY,
//                            operation.getTask()));
//
//            if (equal < 1.0f) {
//                rt.add(m.newTask(operation, Symbols.JUDGMENT_MARK,
//                            equal, confidence,
//                            Global.DEFAULT_JUDGMENT_PRIORITY,
//                            Global.DEFAULT_JUDGMENT_DURABILITY,
//                            operation.getTask()));
//            }
//            return rt;
//            */
//
//
//    }
//
//    /**
//     * creates a result term in the conventional format.
//     * the final term in the product (x) needs to be a variable,
//     * which will be replaced with the result term (y)
//     */
//    @Nullable
//    static Term resultTerm(@NotNull Atomic oper, @NotNull Compound x, @Nullable Term y) {
//
//        //add var dep as last term if missing
//        Term xLast = x.last();
//        if (xLast.op() != Op.VAR_DEP) {
//            throw new RuntimeException("feedback requires variable in last position: " + oper + " :: " + x);
//        }
//
//        //x = $.p(Terms.concat(x.terms(), y)); //defaultResultVariable));
//
//        //        } else {
//        //            //TODO more efficient than subterm sequencing it:
//        //            x = $.p( Terms.concat(x.terms(0, x.size()-1), y));
//        //        }
//
//
//        //default case: use the trailing dependent variable for the result term
//        if (y == null)
//            y = xLast;
//
//        return $.inhImageExt(x, y, oper);
//
//        //return $.exec(Operator.operatorTerm(operation), x);
//    }
//
//
////    public final Compound term() {
////        return task.term();
////    }
//
//    /**
//     * unwrapped (without ^)
//     */
////    @NotNull
////    public final Operator operator() {
////        return Operator.operator(term());
////    }
////
////    public final Term[] argArray() {
////        return Operator.opArgsArray(term());
////    }
//
//
//    //arg(int i)
//
//    //feedback(Term t)
//    //feedback(Task t)
//    //feedback(Task[] t)
//    //feedback(Object o)
//
//    static void feedback(@NotNull OperationConcept cause, Task feedback, @NotNull NAR n) {
//        n.inputLater(
//            noticeExecuted(n, cause),
//            feedback
//        );
//    }
//
//    /**
//     * internal notice of the execution
//     *
//     * @param operation
//     */
//    static Task noticeExecuted(@NotNull NAR nar, @NotNull OperationConcept operation) {
//
//        //Budgeted b = !operation.isDeleted() ? operation : UnitBudget.Zero;
//
//        return $.belief(operation.term(), operation.goal(nar.time())). //equal to input, balanced
//                //1f, DEFAULT_EXECUTION_CONFIDENCE).
//
//                        //budget(b).
//                        occurr(nar.time()). //operation.isEternal() ? Tense.ETERNAL :
//                //parent(operation). //https://github.com/opennars/opennars/commit/23d34d5ddaf7c71348d0a70a88e2805ec659ed1c#diff-abb6b480847c96e2dbf488d303fb4962L235
//                        because("Executed")
//                ;
//
//    }
//
//
//}
////package nars.concept;
////
////import jcog.event.Topic;
////import nars.$;
////import nars.NAR;
////import nars.Narsese;
////import nars.Task;
////import nars.bag.Bag;
////import nars.nal.nal8.Execution;
////import nars.term.Compound;
////import nars.term.Term;
////import nars.term.container.TermContainer;
////import nars.truth.TruthDelta;
////import org.jetbrains.annotations.NotNull;
////import org.jetbrains.annotations.Nullable;
////
////import java.util.function.Consumer;
////
////
/////**
//// * Has ability to measure (and cache) belief and desire state in order to execute Operations
//// * and negations of Operations
////
//// * TODO inherit from WiredConcept which improves on this functionality
//// */
////@Deprecated public class OperationConcept extends CompoundConcept<Compound> implements Consumer<NAR> {
////
////    protected volatile boolean pendingRun;
////
////
////    public OperationConcept(@NotNull Compound term, @NotNull Bag<Term> termLinks, @NotNull Bag<Task> taskLinks, @NotNull NAR nar) {
////        super(term, termLinks, taskLinks, nar);
////    }
////
////    public OperationConcept(@NotNull Compound term, @NotNull NAR n) throws Narsese.NarseseException {
////        super(term, n);
////        n.on(this);
////    }
////
////    public OperationConcept(@NotNull String compoundTermString, @NotNull NAR n) throws Narsese.NarseseException {
////        this($.$(compoundTermString), n);
////    }
////
////
////    /* subj contains the parameter product */
////    public final TermContainer parameters() {
////        return ((Compound)term().term(0)).subterms();
////    }
////
////    @Override
////    public TruthDelta processGoal(@NotNull Task goal, @NotNull NAR nar) {
////        TruthDelta truthDelta = super.processGoal(goal, nar);
////        if (truthDelta!=null){
////            executeLater(goal, nar);
////            return truthDelta;
////        }
////        return null;
////    }
////
////    @Override
////    public TruthDelta processBelief(@NotNull Task belief, @NotNull NAR nar) {
////        TruthDelta truthDelta = super.processBelief(belief, nar);
////        if (truthDelta!=null){
////            executeLater(belief, nar);
////            return truthDelta;
////        }
////        return null;
////    }
////
////    @Nullable
////    private Task executeLater(@Nullable Task t, @NotNull NAR nar) {
////        if (t != null) {
////
////            if (!pendingRun && runLater(t, nar)) {
////                pendingRun = true;
////                nar.runLater(this);
////            }
////        }
////
////        return t;
////    }
////
////    protected boolean runLater(@NotNull Task t, @NotNull NAR nar) {
////        return !goals().isEmpty() && operationExec(operationConcept(nar))!=null;
////    }
////
////    /** called between frames if belief or goal state has changed */
////    @Override public void accept(@NotNull NAR nar) {
////        pendingRun = false;
////
////        Topic<OperationConcept> tt = operationExec(nar);
////        if (tt != null) {
////            //beforeNextFrame( //<-- enqueue after this frame, before next
////            tt.emit(this);
////        }
////
////    }
////
////    public @Nullable Topic<OperationConcept> operationExec(@NotNull NAR nar) {
////        return operationExec(operationConcept(nar));
////    }
////
////    public @Nullable
////    static Topic<OperationConcept> operationExec(@Nullable Concept c) {
////        return c != null ? c.get(Execution.class) : null;
////    }
////
////    public @Nullable final Concept operationConcept(@NotNull NAR nar) {
////        return nar.concept(
////            term().term(1) //operator is the predicate
////        );
////    }
////
////
//////    public OperationConcept positive(NAR n) {
//////        return op() != NEGATE ? this : (OperationConcept) n.concept(term(0));
//////    }
//////
//////    public Concept negative(NAR n) {
//////        //TODO cache the opposite term
//////        return op() != NEGATE ? n.concept($.neg(this)) : this;
//////    }
////
//////    public final float believed() {
//////        return believed;
//////    }
//////
//////    public final float desired() {
//////        return desired;
//////    }
////
////
//////    /** provide motivation value after triggering an update */
//////    public final float motivation(@NotNull NAR nar) {
//////        update(nar);
//////
////////        float bf = believed.freq();
////////        float bc = believed.conf();
////////        float df = desired.freq();
////////        float dc = desired.conf();
//////
//////        //expectation = (confidence * (frequency - 0.5f) + 0.5f);
//////
//////
//////        /*return
//////                 UtilityFunctions.or(
//////                    ((desired.conf() * (desired.freq()-0.5f)) + 0.5f),
//////                    1f - ((believed.conf() * ((believed.freq())-0.5f ))  + 0.5f)
//////                 );*/
//////
////////        return UtilityFunctions.or(desired.conf(), believed.conf()) *
////////                ((UtilityFunctions.aveAri(desired.freq(), (1f - believed.freq())) - 0.5f)
////////                ) + 0.5f;
//////
//////
//////        float d = (desired.expectation()-0.5f);
//////        if (d < 0) return d;
//////        float b = (believed.expectation()-0.5f);
//////        /*if (b > 0)*/ d-=b;
//////        //float beliefAttenuation = 1f - Math.max(0, ((believed.expectation()) - 0.5f) * 2f);
//////        //d *= beliefAttenuation;
//////        return d*2;
//////    }
////
////
////
////
////    //        if (!Op.isOperation(goalTerm))
//////            return false;
//////            if (goalTerm.op()==Op.PRODUCT) {
//////                @NotNull Compound x = inputGoal.term();
//////                try {
//////                    Term y = rt.eval(x);
//////                    if (y != null) {
//////                        logger.info("(eval( {} , {} )", x, y); //mooseboobs
//////                        return true;
//////                    }
//////                }
//////                /*catch (VerifyError vex) {
//////                    //ex: java.lang.VerifyError: (class: clojure/core$eval1, method: invokeStatic signature: ()Ljava/lang/Object;) Unable to pop operand off an empty stack
//////                }*/ catch (Throwable e) {
//////                    //HACK
//////                    logger.warn("eval {}", e);
//////
//////                }
//////            }
////
////
//////
//////        //Normal Goal
//////        long now = nar.time();
//////        Task projectedGoal = goals().top(now);
//////        float motivation = projectedGoal.motivation();
//////
//////        //counteract with content from any (--, concept
//////        Term antiTerm = $.neg(projectedGoal.term());
//////        Concept antiConcept = nar.concept(antiTerm);
//////        if (antiConcept!=null)
//////            motivation -= antiConcept.motivationElse(now, 0);
//////
////
//////
//////        long occ = projectedGoal.occurrence();
//////        if ((!((occ == ETERNAL) || (Math.abs(occ-now) < nar.duration()*2)))//right timing
//////                ) { //sufficient motivation
//////            return false;
//////        }
//////
//////        goal = projectedGoal;
////
////
////    //DEFAULT EXECUTION PROCEDURE: trigger listener reactions
//////        Topic<Task> tt = n.exe.get(
//////            Operator.operator(term())
//////        );
////
////
////    //float delta = updateSuccess(goal, successBefore, memory);
////
////    //&& (goal.state() != Task.TaskState.Executed)) {
////
////            /*if (delta >= Global.EXECUTION_SATISFACTION_TRESHOLD)*/
////
////    //Truth projected = goal.projection(now, now);
////
////
//////                        LongHashSet ev = this.lastevidence;
//////
//////                        //if all evidence of the new one is also part of the old one
//////                        //then there is no need to execute
//////                        //which means only execute if there is new evidence which suggests doing so1
//////                        if (ev.addAll(input.getEvidence())) {
////
//////                            //TODO more efficient size limiting
//////                            //lastevidence.toSortedList()
//////                            while(ev.size() > max_last_execution_evidence_len) {
//////                                ev.remove( ev.min() );
//////                            }
//////                        }
////
////
////}
