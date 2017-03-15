package nars.experiment;

import nars.NAR;
import nars.Param;
import nars.test.agent.Line1DSimplest;

import static nars.NAgentX.runRT;

/**
 * Created by me on 3/15/17.
 */
public class Line1D {

    public static void main(String[] args) {
        Param.DEBUG = true;


        NAR nar = runRT((NAR n) -> {

            n.onTask(x -> {
                if (x.isGoal() && !x.isInput()) {
                    System.err.println(x.proof());
                }
            });

            n.termVolumeMax.setValue(16);

            Line1DSimplest a = new Line1DSimplest(n);
            n.onCycle(() -> {
                a.i.setValue( 0.5f * (Math.sin(n.time()/1000f) + 1f) );
            });
            return a;

        }, 50, 5, -1);

    }
}
