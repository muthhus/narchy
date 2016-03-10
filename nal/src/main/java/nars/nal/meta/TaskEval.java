//package nars.nal.meta;
//
//import nars.Memory;
//import nars.concept.Concept;
//import nars.task.Task;
//import nars.term.Compound;
//import nars.term.Termed;
//
//import java.util.function.Supplier;
//
///**
// * task evaluation (process) context
// * re-cycled
// UNTESTED
// */
//public class TaskEval  {
//
//    private final Memory mem;
//    public Task task = null;
//
//    public TaskEval(Memory m) {
//        this.mem = m;
//    }
//
//    /** upon reinitialize */
//    public void start(Task t) {
//        this.task = t;
//    }
//
//    /** returns:
//     *      the existing unified concept in which the task belongs, OR
//     *      a new concept created for this task, OR
//     *      null if the task was invalid
//     */
//    public Termed run(Supplier<Concept> ifNonExistent) {
//        Compound content = task.term();
//        //mem.index.theTerm(...)
//        return null;
//    }
//}
