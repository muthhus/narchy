//package nars.task.flow;
//
//import nars.task.Task;
//import nars.util.data.buffer.Source;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.function.Consumer;
//import java.util.function.Supplier;
//
///**
// * Input percept buffer
// */
//public interface Perception extends Consumer<Source<Task>>,Supplier<Task> {
//    @Override
//    void accept(Source<Task> input);
//
//
//    @NotNull
//    @Override
//    Task get();
//
//    @NotNull
//    default Task pop() {
//        return get();
//    }
//
//    @Nullable
//    default Task pop(float minPriority) {
//        Task t;
//        while ((t = get())!=null) {
//            if (t.pri() >= minPriority)
//                return t;
//        }
//        return null;
//    }
//
//
//    void clear();
//
//    boolean isEmpty();
//}
