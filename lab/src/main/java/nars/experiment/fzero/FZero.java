package nars.experiment.fzero;

import nars.$;
import nars.NAR;
import nars.NAgentX;
import nars.nar.NARBuilder;
import nars.time.RealTime;

import static nars.$.t;

/**
 * Created by me on 3/21/17.
 */
public class FZero extends NAgentX {


    private final FZeroGame fz;

    public FZero(NAR nar) {
        super("fz", nar);

        this.fz =  new FZeroGame();

        senseCamera("fz", ()->fz.image, 80, 80, (v) -> t(v, alpha()));

        actionToggle($.inh($.the("fwd"), $.the("fz")), (b)->{ fz.thrust = b; });
        actionToggle($.inh($.the("left"), $.the("fz")), (b)->{ fz.left = b; });
        actionToggle($.inh($.the("right"), $.the("fz")), (b)->{ fz.right = b; });

        NAgentX.chart(this);
    }

    @Override
    protected float act() {
        return (float)fz.power;
    }

    public static void main(String[] args) {
        new FZero(NARBuilder.newMultiThreadNAR(2, new RealTime.DSHalf(true).durSeconds(0.1f))).runRT(10f);
    }
}
