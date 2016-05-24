package nars.bag;

import nars.nar.Default;
import nars.util.experiment.DeductiveChainTest;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Controller for attempting to ensure certain bag performance metrics
 */
public class BagRegulator<X> {

    static final Logger logger = LoggerFactory.getLogger(BagRegulator.class);

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

        double priMean = pri.getMean();
        if (priMean < 0.25f) {
            logger.warn("Bag drained: priMean={}", priMean);
        }
        if (priMean > 0.75f) {
            logger.warn("Bag saturated: priMean={}", priMean);
        }
        double priVar = pri.getVariance();
        if (priVar < 0.005) {
            logger.warn("Bag flat: priVar={}", priVar);
        }
    }

    public static void main(String[] args) {
        Default n = new Default(512, 4, 3, 3);
        n.conceptRemembering.setValue(400f);

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
