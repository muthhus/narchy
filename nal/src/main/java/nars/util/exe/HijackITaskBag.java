//package nars.util.exe;
//
//import jcog.bag.impl.hijack.PriorityHijackBag;
//import jcog.pri.PForget;
//import nars.task.ITask;
//import org.jetbrains.annotations.NotNull;
//
//import java.util.function.Consumer;
//
///**
// * Created by me on 5/19/17.
// */
//public class HijackITaskBag extends TaskBag {
//
//    final int maxActive = 1 * 1024;
//    final PriorityHijackBag<ITask, ITask> bag = new PriorityHijackBag<ITask, ITask>(maxActive, 3) {
//        @Override
//        protected ITask merge(@NotNull ITask existing, @NotNull ITask incoming, float scale) {
////            existing.priMax(incoming.priSafe(0)*scale);
////            return existing;
//            return super.merge(existing, incoming, scale);
//        }
//
//        @Override
//        protected Consumer<ITask> forget(float rate) {
//            return new PForget(rate);
//        }
//
//        @NotNull
//        @Override
//        public ITask key(ITask value) {
//            return value;
//        }
//    };
//
//    @Override
//    public boolean put(ITask t) {
//        return bag.put(t, 1f, null) != null;
//    }
//
//    @Override
//    public float load() {
//        return ((float) bag.size()) / maxActive;
//    }
//
//    @Override
//    public void forEach(Consumer<ITask> t) {
//        bag.forEach(t);
//    }
//
//    @Override
//    public ITask next() {
//        return bag.sample(); //HACK
////             active.sample((next) -> {
////                        executed.increment();
////                        next.run(nar);
////                        return Bag.BagCursorAction.Next;
////                    }, true);
//
//    }
//
//    @Override
//    public int size() {
//        return bag.size();
//    }
//
//    @Override
//    public void commit() {
//        bag.commit();
//    }
//
//    @Override
//    public void clear() {
//        bag.clear();
//    }
//}
