package nars.nar;

import jcog.Util;
import jcog.event.On;
import jcog.random.XorShift128PlusRandom;
import nars.$;
import nars.NAR;
import nars.NARLoop;
import nars.Narsese;
import nars.conceptualize.DefaultConceptBuilder;
import nars.index.term.TermIndex;
import nars.index.term.map.CaffeineIndex;
import nars.task.ITask;
import nars.test.DeductiveMeshTest;
import nars.time.RealTime;
import nars.time.Time;
import nars.util.exe.BufferedSynchronousExecutor;
import nars.util.exe.SynchronousExecutor;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

/**
 * cluster of NAR's
 */
public class NARS extends NAR {

    final List<NAR> nar = $.newArrayList();
    private List<On> observers = $.newArrayList();

    private ExecutorService pool;
    private List<NARLoop> loops;


    @Override
    public void input(ITask... t) {

        assert (!nar.isEmpty());

        for (ITask x : t) {
            NAR target = this.nar.get(random().nextInt(this.nar.size())); //random distribution TODO abstract to other striping policies
            target.input(x);
        }
    }

    @FunctionalInterface  public interface NARSSupplier {
        NAR build(Time time, TermIndex terms, Random rng);
    }

    public void addNAR(NARSSupplier n) {
        synchronized (terms) {
            assert (!running());
            NAR x = n.build(time, terms, random());
            nar.add(x);
            observers.add(x.eventTaskProcess.on(eventTaskProcess::emit)); //proxy
        }
    }

    /** default implementation convenience method */
    public void addNAR(int concepts) {
        addNAR((time, terms, rng) ->
            new Default(concepts, rng, terms, time, new BufferedSynchronousExecutor())
        );
    }


    public NARS(@NotNull Time time, @NotNull TermIndex terms, @NotNull Random rng) {
        super(time, terms, rng, new SynchronousExecutor() {
                @Override
                public boolean concurrent() {
                    return true;
                }
            });

    }

    public boolean running() {
        return this.pool != null;
    }

    public void start() {
        synchronized (terms) {
            assert (!running());
            this.pool = Executors.newFixedThreadPool(nar.size());
            this.loops = $.newArrayList();
            all(n -> loops.add(new NARLoop(n)));
            loops.forEach(pool::execute);
        }
    }

    public void stop() {
        synchronized (terms) {
            assert (running());
            all(NAR::stop);
            this.pool.shutdownNow();
            this.pool = null;
            this.loops = null;
        }
    }

    public void all(Consumer<NAR> n) {
        nar.forEach(n);
    }

    public static void main(String[] args) throws Narsese.NarseseException {

        NARS n = new NARS(
                new RealTime.DSHalf(true),
                new CaffeineIndex(new DefaultConceptBuilder(), 128 * 1024, ForkJoinPool.commonPool()),
                new XorShift128PlusRandom(1));


        n.addNAR(1024);
        n.addNAR(1024);

        //n.log();

        new DeductiveMeshTest(n, 5, 5);

        n.start();

        for (int i = 0; i < 10; i++) {
            System.out.println(n.stats());
            Util.sleep(500);
        }

        n.stop();
    }

    private TreeMap<String,Object> stats() {
        synchronized (terms) {
            TreeMap<String, Object> m = new TreeMap();

            m.put("now", new Date());

            for (NAR n : nar) {
                m.put(n.self() + "_emotion", n.emotion.summary());
            }
            if (loops != null) {
                for (NARLoop l : loops) {
                    NAR n = l.nar;
                    m.put(n.self() + "_cycles", l.cycleCount());
                }
            }
            m.put("terms", terms.summary());
            return m;
        }
    }


}
