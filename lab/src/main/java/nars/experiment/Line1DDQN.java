package nars.experiment;

import jcog.learn.ql.HaiQAgent;
import nars.NAR;
import nars.NAgentX;
import nars.Param;
import nars.nar.Default;
import nars.nar.Terminal;
import nars.test.agent.Line1DSimplest;

/**
 * Created by me on 4/28/17.
 */
public class Line1DDQN {

    public static void main(String[] args) {
        Param.DEBUG = true;


        NAR n =
                //new Terminal();
                new Default();
        n.time.dur(1);

        Line1DSimplest a = new Line1DSimplest(n);


        a.onFrame((z) -> {
            long now = n.time();
            a.i.setValue(
                    0.5f * (Math.sin(now / 200f) + 1f)
                    //Util.sqr((float) (0.5f * (Math.sin(n.time()/90f) + 1f)))
                    //(0.5f * (Math.sin(n.time()/90f) + 1f)) > 0.5f ? 1f : 0f
            );
        });


        NAgentX.chart(a);

        a.runCycles(500000);


    }

}
