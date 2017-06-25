package nars.budget;

import nars.util.exe.TaskExecutor;



public class TestDerivationBudgeting {

    static class InstrumentedExecutor extends TaskExecutor {

        double totalIn = 0;
        double totalExe = 0;

        public InstrumentedExecutor(int capacity, float rate) {
            super(capacity, rate);
        }

        //TODO keep this consistent with superclass's method which has changed since this was written:
//        protected void actuallyRun(ITask x) {
//
//            try {
//                //super.run(x);
//
//                System.out.println(x);
//
//                float start = x.priSafe(0);
//                if (x.isInput()) totalIn += start;
//                totalExe += start;
//
//                ITask[] next = x.run(nar);
//
//                //float afterExe = x.pri();
//
//                if (forgetEachPri > 0)
//                    x.priSub(forgetEachPri);
//
//                float afterForget = x.pri();
//
//                float spawnPri = 0;
//                int spawnCount = 0;
//                if (next != null) {
//                    spawnCount += next.length;
//                    for (ITask y : next) {
//                        spawnPri += y.priSafe(0);
//
//                        if (y == null || !run(y))
//                            break;
//                    }
//                }
//
//                System.out.println("\t" +
//                    n4(start) +
//                        /*"\t.." + n4(afterExe) + " --" + n4(afterForget) + */
//                        "\t\t**" + n4(spawnPri) + "/" + spawnCount
//                );
//
//            } catch (Throwable e) {
//                NAR.logger.error("{} {}", x, e.getMessage());
//                toRemove.add(x); //TODO add to a 'bad' bag?
//            }
//
//            System.out.println("\t in=" + n4(totalIn) + " exe=" + n4(totalExe));
//            System.out.println();
//        }
    }
//
//    @Test
//    public void testSimpleBudgetChain() throws Narsese.NarseseException {
//        int activeConcepts = 8;
//        Default d = new Default(
//                new Default.DefaultTermIndex(activeConcepts * INDEX_TO_CORE_INITIAL_SIZE_RATIO),
//                new CycleTime(),
//                new InstrumentedExecutor(activeConcepts, 0.5f)
//        );
//        d.input("a:b.","b:c.","c:d.");
//        //d.log();
//        d.run(50);
//
//    }

//    @Test
//    public void testWonderAboutIncompletePlan() {
//
//        O o = O.of(
//            BufferedSynchronousExecutor.class,
//            CycleTime.class,
//            new Default.DefaultTermIndex(1024)
//        );
//        System.out.println(o);
//
//        //assertEquals(1, h.unknown.size());
//
//        //@Nullable Default d = h.get();
//
//    }
//
//    @Test
//    public void testComplete() {
//
//        O of = O.of(
//                BufferedSynchronousExecutor.class,
//                CycleTime.class,
//
//                new Default.DefaultTermIndex(1024),
//                new NullTermIndex(new DefaultConceptBuilder()),
//
//                new XorShift128PlusRandom(1),
//                Random.class
//
//        );
//        for (int i = 0; i < 100; i++) {
//            Default d = of.a(Default.class, new O.How<>() {
//
//                @Override
//                public int impl(List choose) {
//                    return ThreadLocalRandom.current().nextInt(choose.size()); //random
//                }
//
//                @Override
//                public Object value(Parameter inConstructor) {
//
//                    Class<?> tt = inConstructor.getType();
//                    //System.out.println(inConstructor + " " + tt);
//                    if (tt == int.class) {
//                        return 1;
//                    } else if (tt == float.class) {
//                        return 0.5f;
//                    } else if (tt == long.class) {
//                        return 1L;
//                    }
//                    return null;
//                }
//            });
//            System.out.println();
//        }
//
//    }
}
