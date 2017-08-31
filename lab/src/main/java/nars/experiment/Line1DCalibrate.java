package nars.experiment;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import jcog.Util;
import jcog.math.FloatSupplier;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.Param;
import nars.gui.Vis;
import nars.task.DerivedTask;
import nars.test.agent.Line1DSimplest;
import spacegraph.layout.Grid;
import spacegraph.widget.meta.ReflectionSurface;
import spacegraph.widget.meter.Plot2D;

import java.util.List;

import static java.lang.Math.PI;
import static spacegraph.SpaceGraph.window;
import static spacegraph.layout.Grid.VERTICAL;
import static spacegraph.layout.Grid.col;
import static spacegraph.layout.Grid.row;

public class Line1DCalibrate {



        public static void main(String[] args) {

            Param.DEBUG = true;
            NAR n = NARS.threadSafe();

            //new STMTemporalLinkage(n, 2, false);
            n.time.dur(1);
            n.termVolumeMax.set(16);
            //n.beliefConfidence(0.9f);
            //n.goalConfidence(0.5f);
//            n.onCycle((nn) -> {
//                nn.stats(System.out);
//            });

            //n.truthResolution.setValue(0.05f);

            Line1DSimplest a = new Line1DSimplest(n) {

//                final FloatAveraged rewardAveraged = new FloatAveraged(()->super.act(), 10);

            };

            float tHz = 0.005f; //in time units
            float yResolution = 0.1f; //in 0..1.0
            float periods = 16;

            final int runtime = Math.round(periods / tHz);

            n.onTask(t -> {
               if (t instanceof DerivedTask) {
                   if (t.isGoal()) {
                       System.out.println(t.proof());
                   } else {
                       System.out.println(t.toString(n));
                   }
               }
            });

            a.speed.setValue(yResolution);

            a.happy.resolution.setValue(0.01f);
            a.out.resolution.setValue(yResolution);
            a.in.resolution(yResolution);
            a.curiosity.setValue(
                    0.1f
                    //(2/yResolution)*tHz);
            );

            //            a.in.beliefs().capacity(0, 100, a.nar);
            //            a.out.beliefs().capacity(0, 100, a.nar);
            //            a.out.goals().capacity(0, 100, a.nar);

            //Line1DTrainer trainer = new Line1DTrainer(a);

            //new RLBooster(a, new HaiQAgent(), 5);

            //ImplicationBooster.implAccelerator(a);


            a.onFrame((z) -> {

                a.target(
                        //Math.signum(Math.sin(a.nar.time() * tHz * 2 * PI) ) > 0 ? 1f : -1f
                        Util.round((float) (0.5f + 0.5f * Math.sin(a.nar.time() * tHz * 2 * PI)), yResolution)
                        //(float) ( Math.sin(a.nar.time() * tHz * 2 * PI) )
                        //Util.sqr((float) (0.5f * (Math.sin(n.time()/90f) + 1f)))
                        //(0.5f * (Math.sin(n.time()/90f) + 1f)) > 0.5f ? 1f : 0f
                );

                //Util.pause(1);
            });


//            a.runCycles(runtime);

                    new Thread(() -> {
                        //NAgentX.chart(a);
                        int history = 800;
                        window(
                                row(
                                        conceptPlot(a.nar, Lists.newArrayList(
                                                () -> (float) a.i.floatValue(),
                                                a.o,
                                                //a.out.feedback.current!=null ? a.out.feedback.current.freq() : 0f,
                                                () -> a.reward
                                                //() -> a.rewardSum
                                                )
                                                ,
                                                history),
                                        col(
                                                new Vis.EmotionPlot(history, a),
                                                new ReflectionSurface<>(a),
                                                Vis.beliefCharts(history,
                                                        Iterables.concat(a.sensors.keySet(), a.actions.keySet()), a.nar)
                                        )
                                )
                                , 900, 900);

                    }).start();

            n.startFPS(1);


        }

        public static Grid conceptPlot(NAR nar, Iterable<FloatSupplier> concepts, int plotHistory) {

            //TODO make a lambda Grid constructor
            Grid grid = new Grid(VERTICAL);
            List<Plot2D> plots = $.newArrayList();
            for (FloatSupplier t : concepts) {
                Plot2D p = new Plot2D(plotHistory, Plot2D.Line);
                p.add(t.toString(), t::asFloat, 0f, 1f);
                grid.children.add(p);
                plots.add(p);
            }
            grid.layout();

            nar.onCycle(f -> {
                plots.forEach(Plot2D::update);
            });

            return grid;
        }



}
