package nars.exe;

import jcog.bag.Bag;
import jcog.bag.impl.CurveBag;
import jcog.bag.impl.hijack.PriorityHijackBag;
import jcog.pri.op.PriMerge;
import nars.NAR;
import nars.concept.Concept;
import nars.control.Activate;
import nars.task.ITask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * unified executor
 * concurrent, thread-safe. one central concept bag
 */
public class UniExec extends Exec {

    static final Logger logger = LoggerFactory.getLogger(UniExec.class);

    private final int CAPACITY;

    protected Bag<Activate, Activate> active;
    //protected Baggie<Concept> active;


    public UniExec(int capacity) {

        CAPACITY = capacity;
    }

    @Override
    protected synchronized void clear() {
        if (active != null)
            active.clear();
    }

    @Override
    public void activate(Concept c, float activationApplied) {
        active.putAsync(new Activate(c, activationApplied * nar.conceptActivation.floatValue()));
        //active.put(c, activationApplied);
    }

    @Override
    public void add(ITask t) {
        execute(t);
    }

    @Override
    public void fire(Predicate<Activate> each) {

        active.sample(each);

//        float forgetRate = nar.forgetRate.floatValue();
//        active.sample(nar.random(), (l) -> {
//            float pri = l.pri();
//            boolean cont = each.test(new Activate(l.get(), pri)); //TODO eliminate Activate class middle-man
//            //l.set(0.5f * pri); //TODO continuous rate forget based on pressure release
//            l.forget(forgetRate);
//            return cont;
//        });
//        active.depressurize();
    }

    @Override
    public synchronized void start(NAR nar) {

        active =
//                new Baggie(CAPACITY);
                concurrent() ?
                        //new ConcurrentCurveBag<>(PriMerge.plus, new HashMap<>(), nar.random(), CAPACITY)
                        //new ConcurrentArrayBag<ITask,ITask>(this, new ConcurrentHashMap(), CAPACITY) {
                        new PriorityHijackBag<Activate, Activate>(CAPACITY, 5) {

                            @Override
                            public Activate key(Activate value) {
                                return value;
                            }
                        }
                        :
                        new CurveBag<>(PriMerge.plus, new HashMap(CAPACITY),
                                nar.random(), CAPACITY);

        super.start(nar);

    }


    @Override
    public void cycle() {

        active.commit(active.forget(nar.forgetRate.floatValue()));

    }

    @Override
    public synchronized void stop() {
        if (active != null) {
            active.clear();
        }
    }

    @Override
    public int concurrency() {
        return 1;
    }

    @Override
    public Stream<Activate> active() {

        return active.stream();
        //return active.stream().map(x -> new Activate(x.getOne(), x.getTwo()));
    }

//    final int WINDOW_SIZE = 32;
//    final int WINDOW_RATIO = 8;
//    private final TopN<ITask> top = new TopN<>(new ITask[WINDOW_SIZE], this::pri);
//    int windowTTL = WINDOW_RATIO;
//
//    private BagSample top(ITask x) {
//
//        top.add(x);
//
//        if (--windowTTL <= 0) {
//            windowTTL = WINDOW_RATIO;
//            ITask t = top.pop();
//            if (t!=null)
//                exeSample((ITask) t);
//        }
//
//        return BagSample.Next;
//    }

//    public static void main(String... args) {
//        NARS n = NARS.realtime();
//        n.exe(new Execontinue());
//
////        new Loop(1f) {
////
////            @Override
////            public boolean next() {
////                System.out.println();
////                System.out.println( Joiner.on(" ").join(exe.plan) );
////                return true;
////            }
////        };
//
//        NAR nn = n.get();
//
//        try {
//            nn.log();
//            nn.input("a:b. b:c. c:d.");
//        } catch (Narsese.NarseseException e) {
//            e.printStackTrace();
//        }
//
//    }


}
