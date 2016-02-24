package nars.nal.nal8;

import nars.$;
import nars.Memory;
import nars.NAR;
import nars.Op;
import nars.budget.Budget;
import nars.budget.UnitBudget;
import nars.nal.Tense;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.variable.GenericVariable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.Op.PRODUCT;

/**
 * Execution context which Operator implementations
 * receive, supporting any variety of synchronous/
 * asynchronous feedback, access to the invoking NAR,
 * and utility methods for extracting features
 * of the operation task in the context of the executing NAR.
 */
public interface Execution  {

    //private static final Logger logger = LoggerFactory.getLogger(Execution.class);

    float feedbackPriorityMultiplier = 1.0f;
    float feedbackDurabilityMultiplier = 1.0f;
    GenericVariable defaultResultVariable = $.varDep("defaultResultVariable");
//    public final NAR nar;
//    public final Task task;

//    public Execution(NAR nar, Task task, Topic<Execution> listeners) {
//        this.nar = nar;
//        this.task = task;
//

//    }

    static MutableTask result(@NotNull NAR nar, @NotNull Task goal, Term y/*, Term[] x0, Term lastTerm*/, @NotNull Tense tense) {

        Compound operation = goal.term();

        //Variable var=new Variable("$1");
        //  Term actual_part = Similarity.make(var, y);
        //  Variable vardep=new Variable("#1");
        //Term actual_dep_part = Similarity.make(vardep, y);
//        operation=(Operation) operation.setComponent(0,
//                ((Compound)operation.getSubject()).setComponent(
//                        numArgs, y));

        //Examples:
        //      <3 --> (/,^add,1,2,_,SELF)>.
        //      <2 --> (/,^count,{a,b},_,SELF)>. :|: %1.00;0.99%
        //transform to image for perception variable introduction rule (is more efficient representation


        //final int numArgs = x0.length;

        Term inh = result(operation, y);
        if ((!(inh instanceof Compound))) {
            //TODO wrap a non-Compound result as some kind of statement
            return null;
        }

        if (goal.isDeleted()) {
            throw new RuntimeException("goal Deleted");
        }

        return (MutableTask) new MutableTask(inh)
                .judgment()  //with default truth value
                .time(tense, nar.memory)
                .budget(goal.budget())
                .budgetScaled(feedbackPriorityMultiplier, feedbackDurabilityMultiplier)
                .log("Execution Result")
                ;


        //Implication.make(operation, actual_part, TemporalRules.ORDER_FORWARD);
            /*float equal = equals(lastTerm, y);
            ArrayList<Task> rt = Lists.newArrayList(
                    m.newTask(actual, Symbols.JUDGMENT_MARK,
                            1.0f, confidence,
                            Global.DEFAULT_JUDGMENT_PRIORITY,
                            Global.DEFAULT_JUDGMENT_DURABILITY,
                            operation.getTask()));

            if (equal < 1.0f) {
                rt.add(m.newTask(operation, Symbols.JUDGMENT_MARK,
                            equal, confidence,
                            Global.DEFAULT_JUDGMENT_PRIORITY,
                            Global.DEFAULT_JUDGMENT_DURABILITY,
                            operation.getTask()));
            }
            return rt;
            */


    }

    /**
     * creates a result term in the conventional format.
     * the final term in the product (x) needs to be a variable,
     * which will be replaced with the result term (y)
     */
    @Nullable
    static Term result(@NotNull Compound operation, @Nullable Term y) {

        Compound x = (Compound) operation.term(0);
        if (!(x.op() == PRODUCT))
            throw new RuntimeException("invalid operation");

        //add var dep as last term if missing
        Term xLast = x.last();
        if (!(xLast.op() == Op.VAR_DEP)) {
            //logger.warn(
            throw new RuntimeException(
                "feedback requires variable in last position: " + operation);
            //return;
        }
        //x = $.p(Terms.concat(x.terms(), y)); //defaultResultVariable));

        //        } else {
        //            //TODO more efficient than subterm sequencing it:
        //            x = $.p( Terms.concat(x.terms(0, x.size()-1), y));
        //        }


        //default case: use the trailing dependent variable for the result term
        if (y == null)
            y = xLast;

        return $.inhImageExt(operation, y, x);

        //return $.exec(Operator.operatorTerm(operation), x);
    }


//    public final Compound term() {
//        return task.term();
//    }

    /**
     * unwrapped (without ^)
     */
//    @NotNull
//    public final Operator operator() {
//        return Operator.operator(term());
//    }
//
//    public final Term[] argArray() {
//        return Operator.opArgsArray(term());
//    }


    //arg(int i)

    //feedback(Term t)
    //feedback(Task t)
    //feedback(Task[] t)
    //feedback(Object o)

    static void feedback(Task cause, Task feedback, NAR n) {
        n.input(
            noticeExecuted(n, cause),
            feedback
        );
    }

    /**
     * internal notice of the execution
     *
     * @param operation
     */
    static Task noticeExecuted(@NotNull NAR nar, @NotNull Task operation) {

        Budget b = !operation.isDeleted() ? operation.budget() : UnitBudget.Zero;

        Memory memory = nar.memory;

        return $.belief(operation.term(),

                operation.truth()). //equal to input, balanced
                //1f, DEFAULT_EXECUTION_CONFIDENCE).

                        budget(b).
                        present(memory).
                //parent(operation). //https://github.com/opennars/opennars/commit/23d34d5ddaf7c71348d0a70a88e2805ec659ed1c#diff-abb6b480847c96e2dbf488d303fb4962L235
                        because("Executed")
                ;

    }


}
