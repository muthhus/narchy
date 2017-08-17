package nars.exe;

import com.google.common.base.Joiner;
import jcog.Loop;
import jcog.Util;
import jcog.bag.Bag;
import jcog.bag.impl.ConcurrentCurveBag;
import jcog.data.FloatParam;
import jcog.pri.PLink;
import jcog.pri.op.PriMerge;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.control.NARService;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMapUnsafe;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static jcog.bag.Bag.*;
import static jcog.bag.Bag.BagSample.*;
import static nars.time.Tense.ETERNAL;

/** probabalistic continuation kernel */
public class Execontinue<X> extends NARService implements Runnable {

    /** cpu throttle: 100% = full speed, 0 = paused */
    public final FloatParam cpu = new FloatParam(0.5f, 0f, 1f);

    /** fundamental frequency */
    static int cycleMS = 50;
    private long nextCycle;

    /** a procedure which can/may be executed.
     * competes for execution time among other
     * items
     *
     * @param X identifier key
     * */
    abstract public static class Can<X> extends PLink<X> {

        public Can(X id, float pri) {
            super(id, pri);
        }

        /** what is recommended to be done next after a Can has executed. */
        abstract public Iterable<Can> run();

    }

    final Bag<X,Can<X>> plan;

    public Execontinue(NAR nar, int cap) {
        super(nar);
        PriMerge priMerge = PriMerge.plus; /*(a,b) -> {
            return a.priAdd(b.priElseZero());
        };*/
        plan = new ConcurrentCurveBag(priMerge, new ConcurrentHashMapUnsafe<>(cap), nar.random(), cap);
    }

    public void want(Can x) {
        plan.put(x);
    }

    public void run() {

        //long idleSince = ETERNAL;
        int idleCount = -1;
        while (true) {

            if (plan.isEmpty()) {
                if (idleCount == -1) {
                    //idleSince = System.currentTimeMillis();
                    idleCount = 0;
                } else {
                    idleCount++;
                }
                Util.pauseNext(idleCount);
            } else {
                idleCount = -1;

                int awakeTime = (int) (cycleMS * cpu.asFloat());
                if (awakeTime > 0) {
                    nextCycle = System.currentTimeMillis() + awakeTime;
                    plan.commit().sample(this::run);
                }
                int sleepTime = (int) (cycleMS * (1f - cpu.asFloat()));
                if (sleepTime > 0) {
                    Util.stall(sleepTime);
                }
            }


        }
    }

    private BagSample run(Can<X> x) {
        Iterable<Can> next = x.run();
        if (next == null) {
            x.delete();
            return Remove;
        } else {
            next.forEach(this::want);
        }
        if (System.currentTimeMillis() > nextCycle)
            return Stop;
        else
            return Next;
    }

    public static void main(String... args) {
        NAR n = NARS.tmp();
        Execontinue exe = new Execontinue(n, 16);
        new Thread(exe).start();
        new Loop(1f) {

            @Override
            public boolean next() {
                System.out.println();
                System.out.println( Joiner.on(" ").join(exe.plan) );
                return true;
            }
        };
        exe.want(new UselessFork(2, 0.5f));
    }

    private static class UselessFork extends Can<String> {

        public static final AtomicInteger serial = new AtomicInteger();
        private final int degree;
        private final float momentum;

        public UselessFork(int degree, float momentum) {
            super(Util.uuid64(), (float)(Math.random()*1f));
            this.degree = degree;
            this.momentum = momentum;
        }

        @Override
        public Iterable<Can> run() {
            List<Can> next = $.newArrayList();
            for (int i = 0; i < degree; i++)
                next.add(new UselessFork(degree, momentum));
            priMult(1f - momentum);
            return next;
        }
    }
}
