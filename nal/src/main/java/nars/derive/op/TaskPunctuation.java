package nars.derive.op;

import nars.Op;
import nars.control.Derivation;
import nars.derive.AbstractPred;
import nars.derive.PrediTerm;
import org.jetbrains.annotations.NotNull;

import static nars.Op.*;

/**
 * Created by me on 8/27/15.
 */
final public class TaskPunctuation extends AbstractPred<Derivation> {

    public final byte punc;

    TaskPunctuation(byte p) {
        this(p, "task:\"" + ((char) p) + '\"');
    }

    TaskPunctuation(byte p, String id) {
        super(id);
        this.punc = (byte) p;
    }


    @Override
    public final boolean test(@NotNull Derivation m) {
        return m.taskPunct == punc;
    }

    @Override
    public float cost() {
        return 0.1f;
    }

    public static final PrediTerm<Derivation> Belief = new TaskPunctuation(BELIEF);
    public static final PrediTerm<Derivation> Goal = new TaskPunctuation(GOAL);

    public static final PrediTerm<Derivation> BeliefOrGoal = new AbstractPred<Derivation>("task:\".!\"") {
        @Override
        public boolean test(@NotNull Derivation o) {
            byte c = o.taskPunct;
            return c == BELIEF || c == GOAL;
        }

        @Override
        public float cost() {
            return 0.1f;
        }
    };

    public static final PrediTerm<Derivation> QuestionOrQuest = new AbstractPred<Derivation>("task:\"?@\"") {
        @Override
        public boolean test(@NotNull Derivation o) {
            byte c = o.taskPunct;
            return c == Op.QUESTION || c == QUEST;
        }

        @Override
        public float cost() {
            return 0.1f;
        }
    };

    public static final PrediTerm<Derivation> Question = new TaskPunctuation(QUESTION);

    public static final PrediTerm<Derivation> Quest = new TaskPunctuation(QUEST);

    //    /** only belief, not goal or question */
//    public static final AtomicBoolCondition NotGoal = new AtomicBoolCondition()  {
//        @Override public boolean booleanValueOf(@NotNull PremiseEval o) {
//            return (o.premise.task().punc() != Symbols.GOAL);
//        }
//        @Override public String toString() { return "task:\".\""; }
//    };
//    public static final AtomicPred<Derivation> NotQuestion = new AtomicPred<>() {
//        @Override
//        public boolean test(@NotNull Derivation o) {
//            byte p = o.taskPunct;
//            return (p != Op.QUESTION && p != Op.QUEST);
//        }
//
//        @Override
//        public String toString() {
//            return "task:\".!\"";
//        }
//    };
//    public static final AtomicBoolCondition NotBelief = new AtomicBoolCondition()  {
//        @Override public boolean booleanValueOf(@NotNull PremiseEval o) {
//            return (o.premise.task().punc() != Symbols.BELIEF);
//        }
//        @Override public String toString() { return "task:\"!?@\""; }
//    };


    //    @NotNull
//    @Override
//    public String toJavaConditionString() {
//        return "'" + punc + "' == p.getTask().getPunctuation()";
//    }
}
