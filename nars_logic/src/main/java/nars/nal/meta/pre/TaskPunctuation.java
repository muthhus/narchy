package nars.nal.meta.pre;

import nars.Symbols;
import nars.nal.meta.AtomicBooleanCondition;
import nars.nal.meta.PremiseMatch;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 8/27/15.
 */
final public class TaskPunctuation extends AtomicBooleanCondition<PremiseMatch> {

    public final char punc;
    public final String id;


    public static final TaskPunctuation TaskJudgment = new TaskPunctuation('.');

    public static final AtomicBooleanCondition<PremiseMatch> TaskQuestion = new AtomicBooleanCondition<PremiseMatch>() {

        @Override
        public boolean booleanValueOf(@NotNull PremiseMatch o) {
            char taskPunc = o.punc();
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

    @Override
    public final String toString() {
        return id;
    }

    @Override
    public final boolean booleanValueOf(@NotNull PremiseMatch m) {
        return m.punc() == punc;
    }

    //    @NotNull
//    @Override
//    public String toJavaConditionString() {
//        return "'" + punc + "' == p.getTask().getPunctuation()";
//    }
}
