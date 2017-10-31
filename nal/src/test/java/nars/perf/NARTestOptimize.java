package nars.perf;

import jcog.Optimize;
import nars.nal.nal1.NAL1MultistepTest;
import nars.util.NALTest;

public class NARTestOptimize {

    public static void main(String[] args) {

        System.setProperty("junit.jupiter.extensions.autodetection.enabled", "true");


        Optimize.Result r = new Optimize<NALTest>(() -> {

            return new NAL1MultistepTest();

        }).tweak("ttlFactor", 4, 64, (float x, NALTest t) -> {

            t.test.nar.matchTTLmin.set(x);
            t.test.nar.matchTTLmax.set(x*2);

        }).tweak("termVolumeMax", 8, 32, (float x, NALTest t) -> {

            t.test.nar.termVolumeMax.set(x);

        }).run(129, (n) -> {


            try {
                //((NAL1Test)n).backwardInference();
                //((NAL1Test) n).abduction();

                ((NAL1MultistepTest)n).multistepSim4();
                n.end(null);
                return 1f / (1 + n.test.time());

            } catch (Throwable e) {
                e.printStackTrace();
                return Float.NEGATIVE_INFINITY;
            }


        });

        r.print();


    }
}
