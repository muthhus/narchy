package nars;

import jcog.Util;
import jcog.bag.Bag;
import jcog.bag.impl.CurveBag;
import jcog.pri.PLink;
import jcog.pri.op.PriMerge;
import nars.control.Causable;
import nars.exe.Exec;
import nars.term.Term;
import nars.term.atom.Atomic;

import java.util.HashMap;

/**
 * decides mental activity
 */
public class Focus {

    private final Bag<Causable, ProcLink> can;
    final ProcLink IDLE;

    //final Schedulearn sched = new Schedulearn();
    public final Exec.Revaluator revaluator =
            new Exec.DefaultRevaluator();
    private final NAR nar;

    //new MetaGoal.RBMRevaluator(rng);

    public class ProcLink extends PLink<Causable> {

        public ProcLink(Causable can, float p) {
            super(can, p);
        }

        /**
         * calculates next suggested work amount
         */
        public int nextWork() {
            return 1;
        }

        public void work() {
            work(nextWork());
        }

        public void work(int amount) {
            get().run(nar, amount);
        }

    }

    final static Atomic idleTerm = Atomic.the("idle");

    public Focus(NAR n) {
        this.can = new CurveBag(PriMerge.replace, new HashMap(), n.random, 32);
        this.nar = n;
        IDLE = new ProcLink(new Causable(n) {


            @Override
            public Term term() {
                return idleTerm;
            }

            @Override
            protected int next(NAR n, int iterations) {
                Util.sleep(5 * iterations);
                return 0;
            }

            @Override
            public float value() {
                return 0;
            }
        }, 1f);
        can.put(IDLE);
    }

    public void work() {
        can.sample().work();
    }

    public synchronized void update(NAR nar) {
        can.commit(c -> {
            Causable cc = c.get();
            float nextPri = cc.value();
            c.priSet(nextPri);
        });

        can.print();

        revaluator.update(nar.causes, nar.want);

        //sched.
//              try {
//                sched.solve(can, dutyCycleTime);
//
//                //sched.estimatedTimeTotal(can);
//            } catch (InternalSolverError e) {
//                logger.error("{} {}", can, e);
//            }
    }

    public void add(Causable can) {
        synchronized (can) {
            if (this.can.size() == 1) {
                //weaken the idle task
                IDLE.priSet(0f);
            }

            //this.can.remove(IDLE.get());
            this.can.put(new ProcLink(can, 0));
        }
    }

    public void remove(Causable can) {
        synchronized (can) {
            this.can.remove(can);
            if (this.can.isEmpty()) {
                //strengthen the idle task
                IDLE.priMax(1f);
            }
        }
    }

}

//    /**
//     * allocates what can be done
//     */
//    public void cycle(List<Can> can) {
//
//
//        NARLoop loop = nar.loop;
//
//        double nextCycleTime = Math.max(1, concurrency() - 1) * (
//                loop.isRunning() ? loop.periodMS.intValue() * 0.001 : Param.SynchronousExecution_Max_CycleTime
//        );
//
//        float throttle = loop.throttle.floatValue();
//        double dutyCycleTime = nextCycleTime * throttle * (1f - nar.exe.load());
//
//        if (dutyCycleTime > 0) {
//            nar.focus.update(nar);
//
//
//        }
//
//        final double MIN_SLEEP_TIME = 0.001f; //1 ms
//        final int sleepGranularity = 2;
//        int divisor = sleepGranularity * concurrency();
//        double sleepTime = nextCycleTime * (1f - throttle);
//        double sleepEach = sleepTime / divisor;
//        if (sleepEach >= MIN_SLEEP_TIME) {
//            int msToSleep = (int) Math.ceil(sleepTime * 1000);
//            nar.exe.add(new NativeTask.SleepTask(msToSleep, divisor));
//        }
//
//    }