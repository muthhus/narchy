//package nars.util.exe;
//
//import com.google.common.collect.MinMaxPriorityQueue;
//import jcog.pri.Priority;
//import nars.task.ITask;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.function.Consumer;
//
///**
// * Created by me on 5/19/17.
// */
//public abstract class UniquePriorityQueue extends TaskBag {
//
//    public final int cap;
//    public final ConcurrentHashMap<ITask, ITask> active;
//
//    public UniquePriorityQueue(int cap) {
//        active = new ConcurrentHashMap(cap);
//        this.cap = cap;
//    }
//
//    @Override
//    public int size() {
//        return active.size();
//    }
//
//    @Override
//    public void commit() {
//
//    }
//
//    @Override
//    public void clear() {
//        active.clear();
//    }
//
//    @Override
//    public float load() {
//        return ((float) active.size()) / cap;
//    }
//
//    @Override
//    public void forEach(Consumer<? super ITask> t) {
//        active.values().forEach(t);
//    }
//
//
//    public static class MinMaxTaskBag extends UniquePriorityQueue {
//
//        private final MinMaxPriorityQueue<ITask> q;
//
//        public MinMaxTaskBag(int cap) {
//            super(cap);
//            this.q = MinMaxPriorityQueue.orderedBy(Priority.COMPARATOR).create();
//        }
//
//        @Override
//        public ITask put(@NotNull ITask x) {
//            ITask y = active.compute(x, (X, p) -> {
//                if (p == null) {
//                    return X;
//                } else {
//                    return p;
//                }
//            });
//
//            if (y == x) { //new insertion, check for overflow
//                synchronized (q) {
//                    q.add(y);
//                    if (q.size() > cap) {
//                        ITask rr = q.pollFirst();
//                        if (rr!=null) {
//                            active.remove(rr);
//                        }
//
//                    }
//                    //System.out.println(active.size() + " map, " +  q.size() + " queue");
//                    assert(active.size() < 2 * q.size()); //2x is safety limit threshold, beyond that we can assume they became unsynchronized
//                }
//            } else {
//                //TODO priority merge
//            }
//            return y; //HACK
//        }
//
//        @Override
//        public @Nullable ITask next() {
//            ITask x;
//            synchronized (q) {
//                x = q.pollLast();
//            }
//            if (x != null)
//                active.remove(x);
//            return x;
//        }
//    }
//}
