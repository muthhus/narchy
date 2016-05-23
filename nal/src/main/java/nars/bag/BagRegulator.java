package nars.bag;

import nars.nar.Default;
import nars.util.experiment.DeductiveChainTest;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

/**
 * Controller for attempting to ensure certain bag performance metrics
 */
public class BagRegulator<X> {

    private final Bag<X> bag;
    private final MutableFloat forgetRate;

    final SummaryStatistics pri = new SummaryStatistics();

    //next state vector to input
    final float[] input;

    private final MutableFloat perfection;

    public BagRegulator(Bag<X> x,

                              //CONTROL
                              MutableFloat forgetRate, MutableFloat perfection) {
        this.bag = x;
        this.forgetRate = forgetRate;
        this.perfection = perfection;

        //mean, variance, forgetRate
        input = new float[  5 ];
    }

    public void commit() {
        pri.clear();
        //bag.commit();
        bag.forEach(x -> pri.addValue(x.pri()));
    }

    public static void main(String[] args) {
        Default n = new Default();

        BagRegulator r = new BagRegulator(n.core.active, n.conceptRemembering, n.perfection);
        n.onFrame(f -> {
           if (f.time() % 25 == 0) {
               r.commit();
               System.out.println(f.time() + " " + r.pri);
           }
        });

        n.log();

        DeductiveChainTest test = new DeductiveChainTest(n, 6, 100, DeductiveChainTest.inh);
        n.run(100);
    }
}
