//package nars.util.exe;
//
//import lbmq.LinkedBlockingMultiQueue;
//import nars.task.ITask;
//
//import java.util.Collections;
//import java.util.Set;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.function.Consumer;
//
///**
// * uses: LinkedBlockingMultiQueue
// */
//public class LBMQTaskBag extends UniquePriorityQueue {
//
//    final int GRANULARITY = 4;
//    final LinkedBlockingMultiQueue<Integer, ITask> q;
//
//    public LBMQTaskBag(int capacity) {
//        super(capacity);
//
//        q = new LinkedBlockingMultiQueue();
//
//        int subCap = capacity / GRANULARITY;
//
//        for (int i = 0; i < GRANULARITY; i++) {
//            q.addSubQueue(i, i, subCap);
//        }
//
//    }
//
//    @Override
//    public boolean put(ITask x) {
//        float p = x.pri();
//        if (p != p)
//            return false;
//
//        if (active.contains(x))
//            return false;
//
//        int pi = (int) (p * GRANULARITY);
//        if (pi >= GRANULARITY) pi = GRANULARITY - 1; //?
//
//
//        if (q.offer(pi, x)) {
//            active.add(x);
//            return true;
//        }
//        return false;
//    }
//
//    @Override
//    public ITask next() {
////            ITask x = q.poll(); //non-blocking
//
//        ITask x = null;
//        try {
//            x = q.take(); //blocking
//        } catch (InterruptedException e) {
//            return null;
//        }
//        if (x != null) {
//            active.remove(x);
//        }
//        return x;
//    }
//
//
//
//    @Override
//    public void clear() {
//        super.clear();
//        q.clear();
//    }
//
//
//}
