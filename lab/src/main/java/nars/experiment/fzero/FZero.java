package nars.experiment.fzero;

import georegression.metric.UtilAngle;
import jcog.Util;
import jcog.math.FloatNormalized;
import nars.*;
import nars.concept.SensorConcept;
import nars.nar.Default;
import nars.nar.NARBuilder;
import nars.term.atom.Atomic;
import nars.time.RealTime;
import org.apache.commons.math3.util.MathUtils;
import org.jetbrains.annotations.NotNull;
import org.joml.GeometryUtils;

import static nars.$.t;

/**
 * Created by me on 3/21/17.
 */
public class FZero extends NAgentX {


    private final FZeroGame fz;

    public FZero(NAR nar) throws Narsese.NarseseException {
        super("fz", nar);

        this.fz =  new FZeroGame();

        senseCamera("fz", ()->fz.image, 30, 20, (v) -> t(v, alpha()))
                .setResolution(0.03f);

        actionToggle($.inh(Atomic.the("fwd"),id),
                //(b)->{ fz.thrust = b; }
                () -> fz.thrust = true, () -> fz.thrust = false
        );
        actionTriState($.inh(Atomic.the("rot"), id ), (dh) -> {
            switch (dh) {
                case +1: fz.left = false; fz.right = true; break;
                case 0: fz.left = fz.right = false; break;
                case -1: fz.left = true; fz.right = false; break;
            }
        });

        senseNumberDifference($.inh(Atomic.the("joy"), id), happy);
        SensorConcept sensorConcept1 = senseNumberDifference($.inh(Atomic.the("angVel"), id), ()->(float)fz.playerAngle);
        SensorConcept sensorConcept = senseNumberDifference($.inh(Atomic.the("accel"), id), ()->(float)fz.vehicleMetrics[0][6]);
        senseNumberBi($.inh(Atomic.the("rot"), id), new FloatNormalized(() ->
            (float)(MathUtils.normalizeAngle(fz.playerAngle%(2*3.14f), 0 )/Math.PI)
        ));

        //nar.mix.stream("Derive").setValue(1);
        //implAccelerator(nar, this);


//        try {
//            new TaskRule("(%1 &&+0 fz:joy)", "(%1 ==>+0 fz:happy)", nar) {
//                @Override
//                public boolean test(@NotNull Task task) {
//                    return task.isBelief();
//                }
//            };
//            new TaskRule("(%1 &&+5 %2)", "seq(%1,%2)", nar) {
//                @Override
//                public boolean test(@NotNull Task task) {
//                    return polarized(task);
//                }
//            };
//            new TaskRule("(seq(%1,%2) &&+5 %3)", "seq(%1,%2,%3)", nar) {
//                @Override
//                public boolean test(@NotNull Task task) {
//                    return polarized(task);
//                }
//            };
//            new TaskRule("((%1 &&+5 %2) &&+5 %3)", "seq(%1,%2,%3)", nar) {
//                @Override
//                public boolean test(@NotNull Task task) {
//                    return polarized(task);
//                }
//            };
//            new TaskRule("(%1 &&+5 (--,%1))", "neg(%1)", nar) {
//                @Override
//                public boolean test(@NotNull Task task) {
//                    return polarized(task);
//                }
//            };
//            new TaskRule("(%1 &&-5 (--,%1))", "pos(%1)", nar) {
//                @Override
//                public boolean test(@NotNull Task task) {
//                    return polarized(task);
//                }
//            };
//            new TaskRule("(%1 &&+0 (--,(fz)))", "--good(%1)", nar) {
//                @Override
//                public boolean test(@NotNull Task task) {
//                    return polarized(task);
//                }
//            };
//            new TaskRule("(%1 &&+0 (fz))", "good(%1)", nar) {
//                @Override
//                public boolean test(@NotNull Task task) {
//                    return polarized(task);
//                }
//            };

//            new TaskRule("(%1 ==>+0 (fz))", "good(%1)", nar) {
//                @Override
//                public boolean test(@NotNull Task task) {
//                    return polarized(task);
//                }
//            };

//            new TaskRule("(%1 &&+0 %2)", "par:{%1,%2}", nar) {
//                @Override
//                public boolean test(@NotNull Task task) {
//                    return polarized(task);
//                }            };
//            new TaskRule("((%1 &| %2) &| %3)", "par:{%1,%2,%3}", nar) {
//                @Override
//                public boolean test(@NotNull Task task) {
//                    return polarized(task);
//                }            };

//            final Term same = $.the("same");
//            new TaskRule("(%1 <-> %2)", "same:{%1,%2}", nar) {
//                @Override
//                public boolean test(@NotNull Task task) {
//                    return polarized(task) && task.term().containsTermRecursively(same);
//                }
//            };
//        } catch (Narsese.NarseseException e) {
//            e.printStackTrace();
//        }



//        action( new BeliefActionConcept($.inh($.the("fwd"), $.the("fz")), nar, (b) -> {
//            if (b!=null) {
//                float f = b.freq();
//                if (f > 0.75f) {
//                    fz.thrust = true;
//                    return;
//                }
//            }
//            fz.thrust = false;
//        }));
//        action( new BeliefActionConcept($.inh($.the("rot"), $.the("fz")), nar, (b) -> {
//            if (b!=null) {
//                float f = b.freq();
//                if (f > 0.75f) {
//                    fz.left = false; fz.right = true;
//                    return;
//                } else if (f < 0.25f) {
//                    fz.left = true; fz.right = false;
//                    return;
//                }
//            }
//            fz.left = fz.right = false;
//        }));

//        actionBipolar($.inh($.the("rot"), $.the("fz")), (dh) -> {
//           fz.playerAngle += dh * 2f;
//           return true;
//        });
//        actionToggle($.inh($.the("left"), $.the("fz")), (b)->{ fz.left = b; });
//        actionToggle($.inh($.the("right"), $.the("fz")), (b)->{ fz.right = b; });

    }

    protected boolean polarized(@NotNull Task task) {
        if (task.isQuestOrQuestion())
            return true;
        float f = task.freq();
        return f <= 0.2f || f >= 0.8f;
    }

    double lastDistance;

    @Override
    protected float act() {

        double distance = fz.vehicleMetrics[0][1];
        double deltaDistance;
        deltaDistance = (distance - lastDistance) / 20f;
        if (deltaDistance > 1f) deltaDistance = 1f;
        if (deltaDistance < -1f) deltaDistance = -1f;

        lastDistance = distance;

        //lifesupport
        fz.power = Math.max(FZeroGame.FULL_POWER*0.5f, Math.min(FZeroGame.FULL_POWER, fz.power * 1.15f));

        //System.out.println("head=" + fz.playerAngle%(2*3.14f) + " pow=" + fz.power + " vel=" + fz.vehicleMetrics[0][6] + " deltaDist=" + deltaDistance);



        return Util.clamp((float) (-(FZeroGame.FULL_POWER - ((float)fz.power))/FZeroGame.FULL_POWER +
                        //((float)fz.vehicleMetrics[0][6]/100f)+
                        deltaDistance), -1f, +1f);
    }

    public static void main(String[] args) throws Narsese.NarseseException {
        Default n = NARBuilder.newMultiThreadNAR(
                2,
                new RealTime.DSHalf(true)
                        .durFPS(10f), true);

        FZero a = new FZero(n);
        a.runRT(10f);

        NAgentX.chart(a);

    }
}
