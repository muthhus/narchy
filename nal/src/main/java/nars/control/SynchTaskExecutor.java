package nars.control;

import nars.util.exe.TaskExecutor;

public class SynchTaskExecutor extends TaskExecutor {

    public SynchTaskExecutor(int capacity, float rate) {
        super(capacity, rate);
    }

//        @Override
//        protected void actuallyFeedback(CLink<ITask> x, ITask[] next) {
//            if (next!=null) {
//                for (ITask i : next) {
//                    if (i==null)
//                        continue;
//
//                    if (i instanceof NALTask)
//                        actuallyRun(new CLink(i));
//                    else
//                        nar.input(i);
//                }
//                //super.actuallyFeedback(x, next);
//            }
//        }
}
