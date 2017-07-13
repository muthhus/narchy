//package nars.control;
//
//import jcog.pri.mix.control.CLink;
//import nars.task.ITask;
//import nars.task.NALTask;
//import nars.util.exe.TaskExecutor;
//import org.jetbrains.annotations.NotNull;
//
//public class SynchTaskExecutor extends TaskExecutor {
//
//    public SynchTaskExecutor(int capacity, float rate) {
//        super(capacity, capacity, rate);
//    }
//
//    @Override
//    public boolean run(@NotNull CLink<ITask> input) {
//        if (input.ref instanceof NALTask) {
//            input.ref.run(nar); //immediate belief insert
//        }
//        return super.run(input);
//    }
//
//    //        @Override
////        protected void actuallyFeedback(CLink<ITask> x, ITask[] next) {
////            if (next!=null) {
////                for (ITask i : next) {
////                    if (i==null)
////                        continue;
////
////                    if (i instanceof NALTask)
////                        actuallyRun(new CLink(i));
////                    else
////                        nar.input(i);
////                }
////                //super.actuallyFeedback(x, next);
////            }
////        }
//}
