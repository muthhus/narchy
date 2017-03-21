package nars.experiment.fzero;

import jcog.Util;
import jcog.math.FloatNormalized;
import nars.$;
import nars.NAR;
import nars.NAgentX;
import nars.Narsese;
import nars.nar.NARBuilder;
import nars.time.RealTime;

import static nars.$.t;

/**
 * Created by me on 3/21/17.
 */
public class FZero extends NAgentX {


    private final FZeroGame fz;

    public FZero(NAR nar) throws Narsese.NarseseException {
        super("fz", nar);

        this.fz =  new FZeroGame();

        senseCamera("fz", ()->fz.image, 40, 40, (v) -> t(v, alpha())).setResolution(0.05f).priTotal(32f);

        //senseNumberTri("rot", new FloatNormalized(() -> (float)fz.playerAngle%(2*3.14f)));

        actionToggle($.inh($.the("fwd"), $.the("fz")), (b)->{ fz.thrust = b; });
        actionTriState($.inh($.the("rot"), $.the("fz")), (dh) -> {
            switch (dh) {
                case +1: fz.left = false; fz.right = true; break;
                case 0: fz.left = fz.right = false; break;
                case -1: fz.left = true; fz.right = false; break;
            }
        });
//        actionBipolar($.inh($.the("rot"), $.the("fz")), (dh) -> {
//           fz.playerAngle += dh * 2f;
//           return true;
//        });
//        actionToggle($.inh($.the("left"), $.the("fz")), (b)->{ fz.left = b; });
//        actionToggle($.inh($.the("right"), $.the("fz")), (b)->{ fz.right = b; });

        NAgentX.chart(this);
    }

    double lastDistance = 0;

    @Override
    protected float act() {

        double distance = fz.vehicleMetrics[0][1];
        double deltaDistance;
        deltaDistance = (distance - lastDistance) / 40f;
        if (deltaDistance > 1f) deltaDistance = 1f;
        if (deltaDistance < -1f) deltaDistance = -1f;

        lastDistance = distance;

        //lifesupport
        fz.power = Math.max(FZeroGame.FULL_POWER*0.5f, Math.min(FZeroGame.FULL_POWER, fz.power * 1.1f));

        //System.out.println("head=" + fz.playerAngle%(2*3.14f) + " pow=" + fz.power + " vel=" + fz.vehicleMetrics[0][6] + " deltaDist=" + deltaDistance);

        return Util.clamp(-(FZeroGame.FULL_POWER - ((float)fz.power))/FZeroGame.FULL_POWER +
                //((float)fz.vehicleMetrics[0][6]/100f)+
                (float) (deltaDistance), -1f, +1f);
    }

    public static void main(String[] args) throws Narsese.NarseseException {
        new FZero(NARBuilder.newMultiThreadNAR(3, new RealTime.DSHalf(true).durSeconds(0.1f))).runRT(20f);
    }
}
