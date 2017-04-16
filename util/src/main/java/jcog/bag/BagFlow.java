package jcog.bag;

import jcog.meter.event.PeriodMeter;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;

import java.util.concurrent.Executor;
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

    public interface TransferFn<X,Y> {
        void transfer(X object, int hits, Bag<?,Y> target);
    }

    private final TransferFn<X,Y> inToOut;
    private final Consumer<Y> eachOut;
    private final float batchDivisor;

    public BagFlow(Bag in, Bag out, Executor e, TransferFn<X, Y> inToOut, Consumer<Y> eachOut) {

        this.exe = e;

        //determine batch granularity
        int procs = Runtime.getRuntime().availableProcessors();
        this.batchDivisor = procs <= 1 ? 1f : (procs * 4f);

        this.in = in;
        inRate = new PeriodMeter("in->out", 32);
        this.inToOut = inToOut;

        this.out = out;
        outRate = new PeriodMeter("output", 32);
        this.eachOut = eachOut;
    }


    /**
     * bags may need to be commit() before calling this
     *
     * decide how many items to transfer from in to out (through a function),
     * and how many to process through a post-process function
     * then process for the specified time duration
     * @param totalSec shared among the total of time used by whatever concurrency is dispatched to execute these
     * @param ioBalance higher value = more priority to input than output
     */
    public void update(double totalSec, float ioBalance) {


        //total time=
        //      inAvg * I + outAvg * O
        //          I <= in.size
        //          O <= out.size



        double iAvg = inRate.mean();
        double oAvg = outRate.mean();

        int toTransfer, toDrain;
        toTransfer = Math.min(in.size(), Math.max(1, (int)Math.ceil((totalSec*ioBalance)/iAvg)));

        double secRemain = Math.max(0, totalSec - (toTransfer * iAvg));
        toDrain = Math.min(out.size(), Math.max(1, (int)Math.ceil(secRemain/oAvg)));

//        System.out.println(
//                "(" +
//                    inRate + " -> n=" + toTransfer +
//                    "\t\t" +
//                    outRate + "-> n=" + toDrain +
//                ") per batches of size=" + batchDivisor );

        if (toTransfer > 0)
            transfer(toTransfer);

        if (toDrain > 0)
            drain(toDrain);

    }

    protected void transfer(int remain) {
        drain(remain, this.inRate, (h,x) -> {
            inToOut.transfer(x, h, out);
            return h;
        }, this.in, batchDivisor, exe);
    }

    protected void drain(int remain) {
        drain(remain, this.outRate, (h,x) -> {
            eachOut.accept(x);
            return -1;
        }, this.out, batchDivisor, exe);
    }


    private static <X,Y> void drain(int remain, PeriodMeter rate, IntObjectToIntFunction<? super X> f, Bag<?, X> sampled, float batchDivisor, Executor exe) {
        int batchSize = (int)Math.ceil(remain/batchDivisor);
        while (remain > 0) {
            int nextBatchSize = Math.min(remain, batchSize);
            exe.execute(() -> {
                long start = nanoTime();

                sampled.sample(nextBatchSize, f);

                long end = nanoTime();
                double dt = (end - start) / ((float)nextBatchSize);
                rate.hitNano(dt);
            });
            remain -= nextBatchSize;
        }
    }


}
