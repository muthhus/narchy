package nars.experiment.fzero;

import jcog.Util;
import nars.*;
import nars.concept.ScalarConcepts;
import nars.gui.BeliefTableChart;
import nars.gui.Vis;
import nars.video.Scale;
import org.apache.commons.math3.util.MathUtils;
import org.jetbrains.annotations.NotNull;
import spacegraph.layout.Grid;

import static java.util.stream.Collectors.toList;
import static nars.term.atom.Atomic.the;
import static spacegraph.SpaceGraph.window;

/**
 * Created by me on 3/21/17.
 */
public class FZero extends NAgentX {

    private final FZeroGame fz;

    public static void main(String[] args) {

        float fps = 25f;


        NAgentX.runRT((n)->{

            FZero a = null;
            try {
                //n.truthResolution.setValue(0.05f);
                a = new FZero(n);
            } catch (Narsese.NarseseException e) {
                e.printStackTrace();
            }
            a.trace = true;

            return a;

        }, fps);



    }

    public FZero(NAR nar) throws Narsese.NarseseException {
        super("fz", nar);

        this.fz = new FZeroGame();

        senseCamera("fz", new Scale(() -> fz.image, 32, 24)/*.blur()*/)
                //.resolution(0.01f)
        ;

//        PixelBag cc = PixelBag.of(()->fz.image, 32, 24);
//        cc.addActions($.the("fz"), this, false, false, true);
//        CameraSensor<PixelBag> sc = senseCamera("fz" /*"(nario,local)"*/, cc)
//                .resolution(0.05f);


        actionBipolar($.inh(the("fwd"), id), (f) -> {
            fz.vehicleMetrics[0][6] += (f) * 0.5f;
            return f;
        });//.resolution.setValue(0.02f);
        actionBipolar($.inh(the("rot"), id), (r) -> {
            fz.playerAngle += (r) * 0.05f;
            return r;
        });//.resolution.setValue(0.01f);

        //keyboard-ish controls:
//actionToggle($.inh(Atomic.the("fwd"),id), (b)-> fz.thrust = b );
//        actionTriState($.inh(Atomic.the("rot"), id ), (dh) -> {
//            switch (dh) {
//                case +1: fz.left = false; fz.right = true; break;
//                case 0: fz.left = fz.right = false; break;
//                case -1: fz.left = true; fz.right = false; break;
//            }
//        });

        //senseNumberDifference($.inh(the("joy"), id), happy).resolution.setValue(0.02f);
//        senseNumberDifference($.prop(the("angVel"), id), () -> (float) fz.playerAngle).resolution.setValue(0.02f);
//        senseNumberDifference($.prop(the("accel"), id), () -> (float) fz.vehicleMetrics[0][6]).resolution.setValue(0.02f);
        @NotNull ScalarConcepts ang = senseNumber($.inh(the("ang"), id), () ->
                        (float) (0.5f + 0.5f * MathUtils.normalizeAngle(fz.playerAngle, 0) / (Math.PI * 2)),
                8,
                //ScalarConcepts.Hard
                ScalarConcepts.FuzzyTriangle
        ).resolution(0.33f);
        window(
                Vis.conceptBeliefPlots(this, ang, 8), 500, 500);

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
        deltaDistance = (distance - lastDistance) / 25f;
        if (deltaDistance > 1f) deltaDistance = 1f;
        if (deltaDistance < -1f) deltaDistance = -1f;

        lastDistance = distance;

        //lifesupport
        fz.power = Math.max(FZeroGame.FULL_POWER * 0.5f, Math.min(FZeroGame.FULL_POWER, fz.power * 1.15f));

        //System.out.println("head=" + fz.playerAngle%(2*3.14f) + " pow=" + fz.power + " vel=" + fz.vehicleMetrics[0][6] + " deltaDist=" + deltaDistance);


        return Util.clamp(
                //-0.5f /* bias */ +
                (float) (-(FZeroGame.FULL_POWER - ((float) fz.power)) / FZeroGame.FULL_POWER +
                //((float)fz.vehicleMetrics[0][6]/100f)+
                deltaDistance), -1f, +1f);
    }


}
