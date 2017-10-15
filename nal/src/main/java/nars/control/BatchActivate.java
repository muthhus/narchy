package nars.control;

import jcog.map.SaneObjectFloatHashMap;
import nars.NAR;
import nars.concept.Concept;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectFloatHashMap;

public class BatchActivate {

    final ObjectFloatHashMap<Concept> a = new SaneObjectFloatHashMap<>(64);

    final static ThreadLocal<BatchActivate> the = ThreadLocal.withInitial(BatchActivate::new);

    //final static LongHashSet active = new LongHashSet();

    public static BatchActivate get() {
        return the.get();
    }

    BatchActivate() {

    }



    public void commit(NAR nar) {
        if (!a.isEmpty()) {
            try {
                a.forEachKeyValue((c, p) -> nar.input(new Activate(c, p)));
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                a.clear();
            }
        }
    }

    public void put(Concept c, float pri) {
        a.addToValue(c, pri);
    }


    public void clear() {
        a.clear();
    }

//        public static class BatchActivateCommit extends NativeTask {
//
//            private final Activate[] activations;
//
//            public BatchActivateCommit(Activate[] l) {
//                this.activations = l;
//            }
//
//            @Override
//            public String toString() {
//                return "ActivationBatch x" + activations.length;
//            }
//
//            @Override
//            public @Nullable Iterable<? extends ITask> run(NAR n) {
//                n.input(activations);
//                return null;
//            }
//        }
}
