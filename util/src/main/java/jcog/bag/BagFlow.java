package jcog.bag;

import jcog.meter.event.PeriodMeter;

import java.util.concurrent.Executor;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

import static java.lang.System.nanoTime;

/**
 * Managed flow control for a pair of bags
 */
public class BagFlow<X,Y> {
    public final Bag<?,X> in;
    public final Bag<?,Y> out;
    private final Executor exe;

    final PeriodMeter /*inRate,*/ outRate, inRate;
    private final BiPredicate<X, Bag<?,Y>> inToOut;
    private final Consumer<Y> eachOut;

    public BagFlow(Bag in, Bag out, Executor e, BiPredicate<X,Bag /* output bag to insert result */> inToOut, Consumer<Y> eachOut) {
        this.in = in;
        this.out = out;
        this.exe = e;
        //inRate = new PeriodMeter("", 64);
        outRate = new PeriodMeter("", 8);
        inRate = new PeriodMeter("", 8);

        this.inToOut = (x, o) -> {
            long start = nanoTime();
            boolean b;
            {
                b = inToOut.test(x, o);
            }
            long end = nanoTime();
            inRate.hitNano(end-start);
            return b;
        };
        this.eachOut = (x) -> {
            long start = nanoTime();
            {
                eachOut.accept(x);
            }
            long end = nanoTime();
            outRate.hitNano(end-start);
        };
    }


    /**
     * bags may need to be commit() before calling this
     *
     * decide how many items to transfer from in to out (through a function),
     * and how many to process through a post-process function
     * then process for the specified time duration
     */
    public void update() {



        //total time=
        //      inAvg * I + outAvg * O
        //      I <= in.size
        //      O <= out.size

        double totalSec = 0.01;

        double iAvg = inRate.mean();
        double oAvg = outRate.mean();

        //TODO calculate
        int toTransfer, toDrain;
        toTransfer = Math.min(out.capacity(), Math.max(1, (int)Math.ceil((totalSec/2)/iAvg)));
        toDrain = Math.max(1, (int)Math.ceil((totalSec/2)/oAvg));
//        if (iAvg <= oAvg) {
//            //equal or input faster than output; output limits
//        } else {
//            //input slower than output; input limits
//        }

        System.out.println(inRate + "-> " + toTransfer + "\t\t" + outRate + "->" + toDrain);

        transfer(toTransfer);

        drain(toDrain);

    }

    protected void transfer(int num) {
        exe.execute(()-> {
            in.sample /* pop */(
                    num,
                    x -> inToOut.test(x, out)
            );
        });
    }

    protected void drain(int num) {
        exe.execute(()-> {
            out.pop(num, x -> {
                eachOut.accept(x);
                return true;
            });
        });
    }

}
