package nars.nar;


import nars.NAR;
import nars.conceptualize.DefaultConceptBuilder;
import nars.index.term.map.CaffeineIndex;
import nars.time.Time;
import nars.nar.exe.Executioner;
import org.jetbrains.annotations.NotNull;

import java.util.Random;
import java.util.concurrent.*;

import static java.util.concurrent.ForkJoinPool.defaultForkJoinWorkerThreadFactory;

/**
 * multithreaded recursive cluster of NAR's
 * <sseehh> any hierarchy can be defined including nars within nars within nars
 * <sseehh> each nar runs in its own thread
 * <sseehh> they share concepts
 * <sseehh> but not the importance of concepts
 * <sseehh> each one has its own concept attention
 * <sseehh> link attention is currently shared but ill consider if this needs changing
 */
public class MultiNAR extends NAR {


    /**
     * background: misc tasks to finish before starting next cycle
     */
    final static int passiveThreads = 2;


    MultiNAR(@NotNull Time time, @NotNull Random rng, Executioner e) {
        this(time, rng, new ForkJoinPool(passiveThreads, defaultForkJoinWorkerThreadFactory,
                null, true /* async */), e);
    }

    MultiNAR(@NotNull Time time, @NotNull Random rng, ForkJoinPool passive, Executioner e) {
        super(new CaffeineIndex(new DefaultConceptBuilder(), 256 * 1024, passive) {

//                  @Override
//                  protected void onBeforeRemove(Concept c) {
//
//                      //victimize neighbors
//                      PriReference<Term> mostComplex = c.termlinks().maxBy((x -> x.get().volume()));
//                      if (mostComplex!=null) shrink(mostComplex.get());
//
//                      PriReference<Task> mostComplexTa = c.tasklinks().maxBy((x -> x.get().volume()));
//                      if (mostComplexTa!=null) shrink(mostComplexTa.get());
//
//                  }
//
//                  private void shrink(Term term) {
//                      Concept n = nar.concept(term);
//                      if (n != null) {
//                          shrink(n);
//                      }
//                  }
//
//                  private void shrink(Task task) {
//                      Concept n = task.concept(nar);
//                      if (n != null) {
//                          shrink(n);
//                      }
//                  }
//
//                  private void shrink(Concept n) {
//                      int ntl = n.termlinks().capacity();
//                      if (ntl > 0) {
//                          n.termlinks().setCapacity(ntl - 1);
//                      }
//                  }
//
//

              }, e, time,
                //new HijackTermIndex(new DefaultConceptBuilder(), 128 * 1024, 4),
                rng);


    }

//    @Override
//    protected PSinks newInputMixer() {
//        MixContRL<ITask> r = new MixContRL<>(20f,
//                null,
//
//                FloatAveraged.averaged(emotion.happy.sumIntegrator()::sumThenClear, 1),
//
//                8,
//
//                new EnumClassifier("type", new String[]{
//                        "Belief", "Goal", "Question", "Quest",
//                        "ConceptFire"
//                }, (x) -> {
//
//                    if (x instanceof NALTask) {
//                        //NAL
//                        switch (((Task) x).punc()) {
//                            case BELIEF:
//                                return 0;
//                            case GOAL:
//                                return 1;
//                            case QUESTION:
//                                return 2;
//                            case QUEST:
//                                return 3;
//                        }
//                    } else if (x instanceof ConceptFire) {
//                        return 4;
//                    }
//
//                    return -1;
//                }),
//
//                new EnumClassifier("complexity", 3, (t) -> {
//                    if (t instanceof NALTask) {
//                        int c = ((NALTask) t).
//                                volume();
//                                //complexity();
////                        int m = termVolumeMax.intValue();
////                        assert(m > 5);
//                        if (c < 5) return 0;
//                        if (c < 10) return 1;
//                        return 2;
//                    }
//                    return -1;
//                }),
//
//                new EnumClassifier("when", new String[]{"Present", "Future", "Past"}, (t) -> {
//                    if (t instanceof NALTask) {
//                        long now = time();
//                        int radius = 2;
//                        long h = ((NALTask) t).nearestStartOrEnd(now);
//                        if (Math.abs(h - now) <= dur() * radius) {
//                            return 0; //present
//                        } else if (h > now) {
//                            return 1; //future
//                        } else {
//                            return 2; //past
//                        }
//                    }
//                    return -1;
//                }, true)
//
////            new MixRouter.Classifier<>("original",
////                    (x) -> x.stamp().length <= 2),
////            new MixRouter.Classifier<>("unoriginal",
////                    (x) -> x.stamp().length > 2),
//        );
//
//        r.setAgent(
//                new NARMixAgent<>(new NARBuilder()
//                        .index(
//                                new HijackTermIndex(new DefaultConceptBuilder(), 8*1024, 3)
//                                //new CaffeineIndex(new DefaultConceptBuilder(), -1, MoreExecutors.newDirectExecutorService())
//                        ).get(), r, this)
//
//                //new HaiQMixAgent()
//
//                //new MultiHaiQMixAgent()
//        );
//
//        return r;
//    }


//    @Override
//    public void input(@NotNull ITask partiallyClassified) {
//        ((MixContRL) in).test(partiallyClassified);
//        super.input(partiallyClassified);
//    }

//    /**
//     * default implementation convenience method
//     */
//    public void addNAR(int conceptCapacity, int taskCapacity, float conceptRate) {
//        synchronized (sub) {
//            sub.add( new SubExecutor(conceptCapacity, taskCapacity, conceptRate) );
//        }
//
//    }


    //    /** temporary 1-cycle old cache of truth calculations */
//    final Memoize<Pair<Termed, ByteLongPair>, Truth> truthCache =
//            new HijackMemoize<>(2048, 3,
//                    k -> {
//                        Truth x = super.truth(k.getOne(), k.getTwo().getOne(), k.getTwo().getTwo());
//                        if (x == null)
//                            return Truth.Null;
//                        return x;
//                    }
//            );
//
//    @Override
//    public @Nullable Truth truth(@Nullable Termed concept, byte punc, long when) {
//        Pair<Termed, ByteLongPair> key = Tuples.pair(concept, PrimitiveTuples.pair(punc, when));
//        Truth t = truthCache.apply(key);
//        if (t == Truth.Null) {
//            return null;
//        }
//        return t;
//        //return truthCache.computeIfAbsent(key, k -> super.truth(k.getOne(), k.getTwo().getOne(), k.getTwo().getTwo()));
//        //return super.truth(concept, punc, when);
//    }





//    public static void main(String[] args) {
//
//        NARS n = new NARS(
//                new RealTime.DSHalf(true),
//                new XorShift128PlusRandom(1), 2);
//
//
//        n.addNAR(2048);
//        n.addNAR(2048);
//
//        //n.log();
//
//        new DeductiveMeshTest(n, 5, 5);
//
//        n.start();
//
//        for (int i = 0; i < 10; i++) {
//            System.out.println(n.stats());
//            Util.sleep(500);
//        }
//
//        n.stop();
//    }
//

}
