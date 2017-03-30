package nars.experiment.fzero;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.graph.MutableGraph;
import jcog.Util;
import nars.*;
import nars.nar.NARBuilder;
import nars.term.Compound;
import nars.term.Term;
import nars.time.RealTime;
import nars.task.util.TaskRule;
import nars.util.graph.TermGraph;
import org.jetbrains.annotations.NotNull;
import org.jgrapht.DirectedGraph;

import static nars.$.t;

/**
 * Created by me on 3/21/17.
 */
public class FZero extends NAgentX {


    private final FZeroGame fz;

    public FZero(NAR nar) {
        super("fz", nar);

        this.fz =  new FZeroGame();

        senseCamera("fz", ()->fz.image, 40, 32, (v) -> t(v, alpha()))
                .setResolution(0.05f)
                .priTotal(4f);

        senseNumberDifference($.inh($.the("joy"), $.the("fz")), happy);
        senseNumberDifference($.func("angVel", $.the("fz")), ()->(float)fz.playerAngle).resolution(0.02f);
        senseNumberDifference($.func("accel", $.the("fz")), ()->(float)fz.vehicleMetrics[0][6]).resolution(0.02f);
        //senseNumberTri("rot", new FloatNormalized(() -> (float)fz.playerAngle%(2*3.14f)));

        TermGraph.StatementGraph tg = new TermGraph.StatementGraph(nar);
        nar.onCycle(r -> {
            MutableGraph<Term> s = tg.snapshot(
                    $.newArrayList(actions.get(0).term(), actions.get(1).term(), happy.term()),
                    nar);
            System.out.println(s);
        });


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

        actionTogglePWM($.func($.the("fwd"), $.the("fz")),
            //(b)->{ fz.thrust = b; }
            () -> fz.thrust = true, () -> fz.thrust = false
        );
        actionTriStatePWM($.func($.the("rot"), $.the("fz")), (dh) -> {
            switch (dh) {
                case +1: fz.left = false; fz.right = true; break;
                case 0: fz.left = fz.right = false; break;
                case -1: fz.left = true; fz.right = false; break;
            }
        });

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

        NAgentX.chart(this);
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



        return (float) Math.pow(Util.clamp((float) (-(FZeroGame.FULL_POWER - ((float)fz.power))/FZeroGame.FULL_POWER +
                        //((float)fz.vehicleMetrics[0][6]/100f)+
                        deltaDistance), -1f, +1f), 3);
    }

    public static void main(String[] args) {
        new FZero(NARBuilder.newMultiThreadNAR(
                3, new RealTime.CS(true).durSeconds(0.05f), true))
                .runRT(0);
    }
}
