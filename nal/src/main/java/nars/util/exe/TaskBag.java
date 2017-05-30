//package nars.util.exe;
//
//import nars.task.ITask;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.function.Consumer;
//
///**
// * Created by me on 5/19/17.
// */
//abstract public class TaskBag {
//
//    /** returns a removed task, or null if nothing removed */
//    abstract public ITask put(@NotNull ITask x);
//
//    @Nullable
//    abstract public ITask next();
//
//    abstract public int size();
//
//    abstract public void commit();
//
//    abstract public void clear();
//
//    abstract public float load();
//
//    abstract public void forEach(Consumer<? super ITask> t);
//
//}
