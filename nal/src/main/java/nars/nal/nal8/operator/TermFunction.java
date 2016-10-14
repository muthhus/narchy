package nars.nal.nal8.operator;

import nars.$;
import nars.Narsese;
import nars.Op;
import nars.Task;
import nars.budget.Budgeted;
import nars.concept.OperationConcept;
import nars.index.term.TermIndex;
import nars.nal.nal8.AbstractOperator;
import nars.nal.nal8.Execution;
import nars.table.BeliefTable;
import nars.task.MutableTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.var.Variable;
import nars.time.Tense;
import nars.truth.DefaultTruth;
import nars.truth.Truth;
import nars.util.Texts;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static nars.Op.INH;


/**
 * Superclass of functions that execute synchronously (blocking, in thread) and take
 * N input Global and one variable argument (as the final argument), generating a new task
 * with the result of the function substituted in the variable's place.
 */
public abstract class TermFunction<O> extends AbstractOperator {

    private static final Logger logger = LoggerFactory.getLogger(TermFunction.class);

    protected TermFunction() {
    }


    protected TermFunction(String name) {
        super(name);
    }

    public static int integer(@NotNull Term x, int defaultValue) {
        try {
            return integer(x);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static int integer(@NotNull Term x) throws NumberFormatException {
        return Texts.i(Atom.unquote(x));
    }

    //TODO supply the execution instead of the TermBuilder which is referenced from it. in TermBuilder, supply a dummy Execution context for the ImmediateTransforms that need it

    /**
     * y = function(x)
     *
     * @param arguments - the product subject of an Operation
     * @param i
     * @return null if unsuccessful (generates no feedback), or the non-null value with which to construct a feedback task
     */
    @Nullable
    public abstract O function(Compound arguments, TermIndex i);


    @Nullable
    protected MutableTask result(@NotNull OperationConcept goal, Term y/*, Term[] x0, Term lastTerm*/) {
        return Execution.resultTerm(nar, goal, y, getResultTense());
    }

    /**
     * default tense applied to result tasks
     */
    @NotNull
    public Tense getResultTense() {
        //return goal.isEternal() ? Tense.Eternal : Tense.Present;
        return Tense.Present;
    }

//    /** default confidence applied to result tasks */
//    public float getResultFrequency() {
//        return 1.0f;
//    }
//
//
//    /** default confidence applied to result tasks */
//    public float getResultConfidence() {
//        return 0.99f;
//    }


//    protected ArrayList<Task> result2(Operation operation, Term y, Term[] x0, Term lastTerm) {
//        //since Peis approach needs it to directly generate op(...,$output) =|> <$output <-> result>,
//        //which wont happen on temporal induction with dependent variable for good rule,
//        //because in general the two dependent variables of event1 and event2
//        //can not be assumed to be related, but here we have to assume
//        //it if we don't want to use the "resultof"-relation.
//
//
//        Compound actual = Implication.make(
//                operation.cloneWithArguments(x0, var),
//                Similarity.make(var, y),
//                TemporalRules.ORDER_FORWARD);
//
//        if (actual == null) return null;
//
//        Compound actual_dep_part = lastTerm!=null ? Similarity.make(lastTerm, y) : null;
//
//
//        float confidence = operation.getTask().getConfidence();
//        //TODO add a delay discount/projection for executions that happen further away from creation time
//
//        return Lists.newArrayList(
//
//                TaskSeed.make(nar.memory, actual).judgment()
//                        .budget(Global.DEFAULT_JUDGMENT_PRIORITY, Global.DEFAULT_JUDGMENT_DURABILITY)
//                        .truth(getResultFrequency(), getResultConfidence())
//                        .parent(operation.getTask())
//                        .tense(getResultTense()),
//
//                actual_dep_part != null ?
//                        TaskSeed.make(nar.memory, actual_dep_part).judgment()
//                                .budget(Global.DEFAULT_JUDGMENT_PRIORITY, Global.DEFAULT_JUDGMENT_DURABILITY)
//                                .truth(1f, confidence)
//                                .present()
//                                .parent(operation.getTask()) : null
//
//        );
//
//    }

//    @Override
//    protected void noticeExecuted(Task<Operation> operation) {
//        //no notice
//    }

    @NotNull static final Variable feedbackVarAuto = $.varDep("F");

    @Override
    public void execute(@NotNull OperationConcept exec) {



        final Compound ttt = exec.term();
        assert(ttt.op()==INH);
        Compound args = (Compound) ttt.term(0);
        //if (!tt.isCommand()) {

        boolean feedback = true;
        if (!validArgs(args)) {
            if (autoReturnVariable()) {
                args = $.p(ArrayUtils.add(args.terms(), feedbackVarAuto)); //HACK
                feedback = false; //HACK
            } else {
                //args = null;
            }
        }
        //}

        O y = function(args, nar.concepts);
        if (y != null) {

            //if (!tt.isCommand()) {
            if (feedback)
                feedback(exec, y);
            //}
        }

        //prevent re-occurring goals
        @NotNull BeliefTable g = exec.goals();
        g.forEach(Budgeted::delete);
    }

    /** if true, then an operation without a trailing variable will be replaced with it appended */
    public boolean autoReturnVariable() {
        return false;
    }

    protected void feedback(@NotNull OperationConcept cause, @Nullable Object y) {

        if (y == null || (y instanceof Term)) {
            Execution.feedback(cause, result(cause, (Term) y), nar);
            return;
        }

        if (y instanceof Boolean) {
            boolean by = (Boolean) y;
            y = new DefaultTruth(by ? 1 : 0, 0.99f);
        }

        if (y instanceof Truth) {
            Execution.feedback(cause, result(cause, null).truth((Truth) y), nar);
            return;
        }

        if (y instanceof Task) {
            //Task ty = (Task) y;
            //if (ty.pri() == 0) {
                //set a resulting zero budget to the input task's
                //ty.budget().set(cause);
            //}
            Execution.feedback(cause, (Task) y, nar);
            return;
        }

        if (y instanceof Number) {
            y = ($.the((Number) y));
        }


        String ys = y.toString();


        //1. try to parse as task
        char possibleTaskPunc = ys.charAt(ys.length() - 1); //early prevention from invoking parser
        if (Narsese.isPunctuation(possibleTaskPunc) || possibleTaskPunc == ':' /* tense ending character */) {
            try {
                Task t = nar.task(ys);
                if (t != null) {
                    Execution.feedback(cause, t, nar);
                    return;
                }
            } catch (Throwable t) {
                logger.error("execution {} threw {}", cause, t);
            }
        }

        //2. try to parse as term

        Term t = $.the(ys, true);

        Execution.feedback(cause, result(cause, t/*, x, lastTerm*/), nar);
    }

    ///** the term that the output will inherit from; analogous to the 'Range' of a function in mathematical terminology */
    //protected Term getRange() {        return null;    }

    //protected int getMinArity() {        return 0;    }
    //abstract protected int getMaxArity();


    /**
     * (can be overridden in subclasses) the extent to which it is truth
     * that the 2 given terms are equal.  in other words, a distance metric
     */
    public static float equals(@NotNull Term a, Term b) {
        //default: Term equality
        return a.equals(b) ? 1.0f : 0.0f;
    }

    private static boolean validArgs(@NotNull Compound args) {
        //TODO filtering
        return args.size() >= 1 && args.last().op() == Op.VAR_DEP;
    }
}


//if (variable) {
//}
//else {
            /*float equal = equals(lastTerm, y);



            float confidence = 0.99f;
            ArrayList<Task> rt = Lists.newArrayList(
                    m.newTaskAt(actual, Symbols.JUDGMENT,
                            1.0f, confidence,
                            Global.DEFAULT_JUDGMENT_PRIORITY,
                            Global.DEFAULT_JUDGMENT_DURABILITY,
                            operation.getTask()));

            if (equal < 1.0f) {
                rt.add(m.newTaskAt(operation, Symbols.JUDGMENT,
                            equal, confidence,
                            Global.DEFAULT_JUDGMENT_PRIORITY,
                            Global.DEFAULT_JUDGMENT_DURABILITY,
                            operation.getTask()));
            }
            return rt;
            */

//   return null;

//}
