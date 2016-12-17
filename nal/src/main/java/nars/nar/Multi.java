//package nars.nar;
//
//import jcog.data.random.XorShift128PlusRandom;
//import nars.NARLoop;
//import nars.Param;
//import nars.budget.control.DepthFirstActivation;
//import nars.budget.ObjectFloatHashMapPriorityAccumulator;
//import nars.concept.Concept;
//import nars.index.term.TermIndex;
//import nars.index.term.tree.TreeTermIndex;
//import nars.nar.exe.SynchronousExecutor;
//import nars.nar.util.DefaultConceptBuilder;
//import nars.time.RealTime;
//import nars.time.Time;
//import org.jetbrains.annotations.NotNull;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.util.Random;
//import java.util.stream.Stream;
//
///**
// * Created by me on 10/19/16.
// */
//public class Multi {
//
//    @NotNull
//    private final NARLoop[] loop;
//    @NotNull
//    public final Default[] core;
//
//    public interface ConnectivityFunction {
//        /**
//         * priority multiplier of activation a core produced in a processed task,
//         * shared to another core (possibly 0)
//         */
//        float link(int from, int to);
//    }
//
//    final static Logger logger = LoggerFactory.getLogger(Multi.class);
//
//    public Multi(int numCores) {
//        this(numCores, (i,j)->0);
//    }
//
//    public Multi(int numCores, @NotNull ConnectivityFunction conn /*, TermIndex index, Clock c*/) {
//        Time time = new RealTime.DS(true);
//
//        TermIndex index = new TreeTermIndex.L1TreeIndex(new DefaultConceptBuilder(), 1024 * 1024, 8192, numCores);
//
//        this.core = new Default[numCores];
//        this.loop = new NARLoop[numCores];
//        for (int i = 0; i < numCores; i++) {
//            Random random = new XorShift128PlusRandom(i);
//            SynchronousExecutor exe = new SynchronousExecutor() {
//
//                @Override
//                public boolean concurrent() {
//                    return true; //force use of concurrent data structures
//                }
//
//            };
//            final int ii = i;
//            Default ci = new Default(
//                    256, 2, 1, 3,
//                    random, index, time, exe);
//
//            this.core[ii] = ci;
//
//            ci.onTask(tt -> {
//
//                float pri = tt.priActive(0);
//                if (pri > Param.BUDGET_EPSILON) {
//
//                    logger.info("task: {}", tt);
//
//                    for (int j = 0; j < numCores; j++) {
//
//                        if (ii == j)
//                            continue; //prevent self loop
//
//                        float p = pri * conn.link(ii, j);
//                        if (p > Param.BUDGET_EPSILON) {
//                            Default cj = core[j];
//                            ObjectFloatHashMapPriorityAccumulator<Concept> aa = new ObjectFloatHashMapPriorityAccumulator<>();
//                            cj.runLater(()->new DepthFirstActivation(tt, p, tt.concept(ci), cj, 2,2, aa));
//                            cj.priorityAdd(aa.commit(), null);
//                            //cj.core.active.add(tt.term(), p);
//                        }
//                    }
//                }
//            });
//
//            loop[i] = ci.loop(0);
//        }
//
//
//    }
//
//    public static void main(String[] args) {
//        Multi m = new Multi(5, (i, j) -> {
//            //feedforward
//            if (j == i+1)
//                return 0.5f;
//            else
//                return 0f;
//        });
//        m.core[0].input("a:b.", "b:c.", "c:d.", "d:e.");
//        m.core[m.core.length-1].log();
//
//
//    }
//
//    private static Default newNAR3(int cores) {
//        Multi m = new Multi(cores, (i, j) -> {
//            //feedforward
//            if (i + 1 == j)
//                return 0.9f; //decay
//
//            //if ((i + 1) % cores == j)
//            // return 0.9f / (j - i);
//
//            return 0;
//            //return Math.random() < 0.5f ? 0.8f : 0f;
//        });
//
//        Default in = m.core[0];
//
//        SpaceGraph.window(grid(Stream.of(m.core).map(c ->
//                Vis.items(c.core.active, c, 32)).toArray(Surface[]::new)), 900, 700);
//
//        return in;
//    }
//
//
//}
