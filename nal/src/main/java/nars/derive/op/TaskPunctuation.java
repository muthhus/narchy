package nars.derive.op;

import nars.Op;
import nars.control.Derivation;
import nars.derive.AbstractPred;
import nars.derive.PrediTerm;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 8/27/15.
 */
final public class TaskPunctuation extends AbstractPred<Derivation> {

    public final byte punc;

    public static final PrediTerm Belief = new TaskPunctuation('.');
    public static final PrediTerm Goal = new TaskPunctuation('!');

    public static final PrediTerm<Derivation> QuestionOrQuest = new AbstractPred<Derivation>("task:\"?@\"") {
        @Override
        public boolean test(@NotNull Derivation o) {
            byte c = o.taskPunct;
            return c == Op.QUESTION || c == Op.QUEST;
        }
    };

    public static final PrediTerm<Derivation> Question = new AbstractPred<Derivation>("task:\"?\"") {
        @Override
        public boolean test(@NotNull Derivation o) {
            return o.taskPunct == Op.QUESTION;
        }

    };

    public static final PrediTerm<Derivation> Quest = new AbstractPred<Derivation>("task:\"@\"") {
        @Override
        public boolean test(@NotNull Derivation o) {
            return o.taskPunct == Op.QUEST;
        }

    };

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


    TaskPunctuation(char p) {
        this(p, "task:\"" + p + '\"');
    }

    TaskPunctuation(char p, String id) {
        super(id);
        this.punc = (byte) p;
    }


    @Override
    public final boolean test(@NotNull Derivation m) {
        return m.taskPunct == punc;
    }

    //    @NotNull
//    @Override
//    public String toJavaConditionString() {
//        return "'" + punc + "' == p.getTask().getPunctuation()";
//    }
}
