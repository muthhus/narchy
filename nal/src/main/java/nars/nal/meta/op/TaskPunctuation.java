package nars.nal.meta.op;

import nars.Symbols;
import nars.nal.meta.AtomicBoolCondition;
import nars.nal.meta.PremiseEval;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 8/27/15.
 */
final public class TaskPunctuation extends AtomicBoolCondition {

    public final char punc;
    public final String id;


    public static final TaskPunctuation Belief = new TaskPunctuation('.');
    public static final TaskPunctuation Goal = new TaskPunctuation('!');

    public static final AtomicBoolCondition Question = new AtomicBoolCondition() {
        @Override public boolean run(@NotNull PremiseEval o, int now) {
            char c = o.taskPunct;
            return c == Symbols.QUESTION || c == Symbols.QUEST;
        }
        @Override public String toString() {
            return "task:\"?@\"";
        }
    };

//    /** only belief, not goal or question */
//    public static final AtomicBoolCondition NotGoal = new AtomicBoolCondition()  {
//        @Override public boolean booleanValueOf(@NotNull PremiseEval o) {
//            return (o.premise.task().punc() != Symbols.GOAL);
//        }
//        @Override public String toString() { return "task:\".\""; }
//    };
    public static final AtomicBoolCondition NotQuestion = new AtomicBoolCondition()  {
        @Override public boolean run(@NotNull PremiseEval o, int now) {
            char p = o.taskPunct;
            return (p != Symbols.QUESTION && p!= Symbols.QUEST);
        }
        @Override public String toString() { return "task:\".!\""; }
    };
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
        this.punc = p;
        this.id = id;
    }

    @NotNull
    @Override
    public final String toString() {
        return id;
    }

    @Override
    public final boolean run(@NotNull PremiseEval m, int now) {
        return m.taskPunct == punc;
    }

    //    @NotNull
//    @Override
//    public String toJavaConditionString() {
//        return "'" + punc + "' == p.getTask().getPunctuation()";
//    }
}
