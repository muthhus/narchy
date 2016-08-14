///*
// * Here comes the text of your license
// * Each line should be prefixed with  *
// */
//package nars.util.event;
//
//import org.eclipse.collections.api.tuple.Twin;
//import nars.NAR;
//import nars.Narsese;
//import nars.concept.Concept;
//import nars.task.Task;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.function.Consumer;
//
///**
// *
// * @author me
// */
//public abstract class AnswerReaction implements Consumer<Twin<Task>> {
//
//    @Nullable
//    private final Task question;
//    @NotNull
//    private final NAR nar;
//    @Nullable
//    private volatile On reg;
//
//
//    /** reacts to all questions */
//    protected AnswerReaction(@NotNull NAR n) {
//        this(n, (Task)null);
//    }
//
//
//    protected AnswerReaction(@NotNull NAR n, @NotNull String questionTask) throws Narsese.NarseseException {
//        this(n, n.task(questionTask));
//    }
//
//    /** reacts to a specific question or quest */
//    protected AnswerReaction(@NotNull NAR n, @Nullable Task question) {
//
//        nar = n;
//        this.question = question;
//
//        reg = n.eventAnswer.on(this);
//
//        if (question!=null) {
//            reportAnyExistingSolutions();
//            n.input(question);
//        }
//    }
//
//    public void off() {
//        if (reg!=null) {
//            reg.off();
//            reg = null;
//        }
//    }
//
//    protected boolean reportAnyExistingSolutions() {
//        Concept c = nar.concept(question.get());
//        if (c == null) return false;
//
//        Task top = c.beliefs().top(nar.time());
//        if (top!=null) {
//            onSolution(top);
//            /*
//            for (Task s : c.getBeliefs())
//                onSolution(s);
//                */
//
//            return true;
//        }
//        return false;
//    }
//
//    @Override
//    public void accept(@NotNull Twin<Task> taskTwin) {
//        Task questionTask = taskTwin.getOne();
//        Task belief = taskTwin.getTwo();
//
//        if ((question == null) || questionTask.equals(question)) {
//            onSolution(belief);
//        }
//        else if (questionTask.hasParent(question)) {
//            onChildSolution(questionTask, belief);
//        }
//
//    }
//
//
//    /** called when the question task has been solved directly */
//    public abstract void onSolution(Task belief);
//
//    /** called when a subtask of the question has been solved */
//    public void onChildSolution(Task question, Task belief) {
//
//    }
//
//
//}
