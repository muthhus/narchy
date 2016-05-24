package nars.bag;

import com.gs.collections.api.block.function.primitive.FloatFunction;
import nars.$;
import nars.NAR;
import nars.nar.Default;
import nars.task.Task;
import nars.term.Term;
import nars.term.Termed;
import nars.util.data.Sensor;
import nars.util.experiment.DeductiveChainTest;
import nars.util.experiment.DeductiveMeshTest;
import nars.util.signal.Emotion;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.jetbrains.annotations.NotNull;
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


    public static class FrustrationSensor extends Sensor {

        public FrustrationSensor(@NotNull NAR n) {
            super(n, $.p($.the("frustration")), (x) -> {
                @NotNull Emotion emotion = n.emotion;
                return (float) (emotion.frustration.getSum()  / emotion.busy.getSum());
            } );
        }

        @Override
        protected @NotNull void commit(float v) {
            System.out.println("frustration=" + v);
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

        FrustrationSensor fr = new FrustrationSensor(n);

        n.log();


        //DeductiveChainTest test = new DeductiveChainTest(n, 6, 100, DeductiveChainTest.inh);
        DeductiveMeshTest test = new DeductiveMeshTest(n, new int[] { 3, 3}, 2500);
        n.run(100);
    }
}
