package nars.experiment.fzero;

import jcog.Util;
import jcog.math.FloatNormalized;
import jcog.random.XorShift128PlusRandom;
import nars.*;
import nars.concept.SensorConcept;
import nars.nar.Default;
import nars.nar.NARBuilder;
import nars.nar.NARS;
import nars.op.stm.MySTMClustered;
import nars.term.atom.Atomic;
import nars.time.RealTime;
import nars.time.Time;
import org.apache.commons.math3.util.MathUtils;
import org.jetbrains.annotations.NotNull;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;

/**
 * Created by me on 3/21/17.
 */
public class FZero extends NAgentX {



    private final FZeroGame fz;

    public static void main(String[] args) throws Narsese.NarseseException {

        float fps = 10f;
        Time clock = new RealTime.DSHalf(true).durFPS(fps*2);

//        Default n = NARBuilder.newMultiThreadNAR(
//                4,
//                clock
//                        , true);
        NARS n = new NARS(clock, new XorShift128PlusRandom(1), 2);
        n.beliefConfidence(0.95f);
        n.goalConfidence(0.9f);
        n.termVolumeMax.setValue(48);
        n.DEFAULT_QUESTION_PRIORITY = 0.25f;
        n.DEFAULT_QUEST_PRIORITY = 0.25f;
        n.addNAR(1024);
        n.addNAR(512);
        n.addNAR(256);
        MySTMClustered stm = new MySTMClustered(n, 64, BELIEF, 3, true, 16);
        MySTMClustered stmGoal = new MySTMClustered(n, 32, GOAL, 2, true, 16);

        FZero a = new FZero(n);
        a.trace = true;
        a.startRT(fps);


        NAgentX.chart(a);

    }

    public FZero(NAR nar) throws Narsese.NarseseException {
        super("fz", nar);

        this.fz = new FZeroGame();

        senseCamera("fz", () -> fz.image, 32, 24)
                .setResolution(0.01f);


        actionBipolar($.inh(Atomic.the("fwd"), id), (r) -> {
            fz.vehicleMetrics[0][6] += (r*r*r) * 1f;
            return true;
        });
        actionBipolar($.inh(Atomic.the("rot"), id), (r) -> {
            fz.playerAngle += (r*r*r) * 0.15f;
            return true;
        });

        //keyboard-ish controls:
//actionToggle($.inh(Atomic.the("fwd"),id), (b)-> fz.thrust = b );
//        actionTriState($.inh(Atomic.the("rot"), id ), (dh) -> {
//            switch (dh) {
//                case +1: fz.left = false; fz.right = true; break;
//                case 0: fz.left = fz.right = false; break;
//                case -1: fz.left = true; fz.right = false; break;
//            }
//        });

        senseNumberDifference($.inh(Atomic.the("joy"), id), happy);
        SensorConcept sensorConcept1 = senseNumberDifference($.inh(Atomic.the("angVel"), id), () -> (float) fz.playerAngle);
        SensorConcept sensorConcept = senseNumberDifference($.inh(Atomic.the("accel"), id), () -> (float) fz.vehicleMetrics[0][6]);
        senseNumberBi($.inh(Atomic.the("rot"), id), new FloatNormalized(() ->
                (float) (MathUtils.normalizeAngle(fz.playerAngle % (2 * 3.14f), 0) / Math.PI)
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
        deltaDistance = (distance - lastDistance) / 10f;
        if (deltaDistance > 1f) deltaDistance = 1f;
        if (deltaDistance < -1f) deltaDistance = -1f;

        lastDistance = distance;

        //lifesupport
        fz.power = Math.max(FZeroGame.FULL_POWER * 0.5f, Math.min(FZeroGame.FULL_POWER, fz.power * 1.15f));

        //System.out.println("head=" + fz.playerAngle%(2*3.14f) + " pow=" + fz.power + " vel=" + fz.vehicleMetrics[0][6] + " deltaDist=" + deltaDistance);


        return Util.clamp((float) (-(FZeroGame.FULL_POWER - ((float) fz.power)) / FZeroGame.FULL_POWER +
                //((float)fz.vehicleMetrics[0][6]/100f)+
                deltaDistance), -1f, +1f);
    }


}
