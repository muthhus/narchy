package nars.nal.meta.op;

import nars.Symbols;
import nars.nal.meta.AtomicBoolCondition;
import nars.nal.meta.PremiseEval;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by me on 8/27/15.
 */
final public class TaskPunctuation extends AtomicBoolCondition {

    public final char punc;
    public final String id;


    public static final TaskPunctuation TaskJudgment = new TaskPunctuation('.');

    public static final @Nullable AtomicBoolCondition TaskQuestion = new AtomicBoolCondition() {

        @Override
        public boolean booleanValueOf(@NotNull PremiseEval o) {
            char taskPunc = o.punct.get();
            return taskPunc == Symbols.QUESTION || taskPunc == Symbols.QUEST;
        }

        @Override
        public String toString() {
            return "task:\"?\"";
        }

    };

//    public static final TaskPunctuation TaskNotQuestion = new TaskPunctuation(
//            ' ' /* this char wont be used */, "Punc{.|!}") {
//        @Override protected final boolean test(char taskPunc) {
//            return taskPunc != Symbols.QUESTION && taskPunc != Symbols.QUEST;
//        }
//
//    };



    public static final TaskPunctuation TaskGoal = new TaskPunctuation('!');

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
    public final boolean booleanValueOf(@NotNull PremiseEval m) {
        return m.punct.get() == punc;
    }

    //    @NotNull
//    @Override
//    public String toJavaConditionString() {
//        return "'" + punc + "' == p.getTask().getPunctuation()";
//    }
}
