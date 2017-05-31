package nars.experiment;

import com.google.common.collect.Lists;
import jcog.Optimize;
import jcog.Util;
import jcog.math.FloatSupplier;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.derive.DefaultDeriver;
import nars.gui.Vis;
import nars.nar.Default;
import nars.test.agent.Line1DSimplest;
import nars.util.exe.TaskExecutor;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import spacegraph.layout.Grid;
import spacegraph.widget.meta.ReflectionSurface;
import spacegraph.widget.meter.Plot2D;

import java.util.LinkedHashSet;
import java.util.List;

import static java.lang.Math.PI;
import static spacegraph.SpaceGraph.window;
import static spacegraph.layout.Grid.*;

/**
 * Created by me on 3/15/17.
 */
public class Line1D {


    static FloatFunction<NAR> experiment = (n) -> {


        Line1DSimplest a = new Line1DSimplest(n);
        a.init();

        float freq = 1 / 500f;
        float speed = 0.1f;
        a.speed.setValue(speed);

        a.out.resolution.setValue(speed * 1);
        a.in.resolution.setValue(speed * 1);
        a.curiosity.setValue(0.01f);

        //            a.in.beliefs().capacity(0, 100, a.nar);
        //            a.out.beliefs().capacity(0, 100, a.nar);
        //            a.out.goals().capacity(0, 100, a.nar);

        //Line1DTrainer trainer = new Line1DTrainer(a);

        //new RLBooster(a, new HaiQAgent(), 5);

        //ImplicationBooster.implAccelerator(a);


        a.onFrame((z) -> {

            a.target(
                    (float) (0.5f * (Math.sin(a.nar.time() * freq * 2 * PI) + 1f))
                    //Util.sqr((float) (0.5f * (Math.sin(n.time()/90f) + 1f)))
                    //(0.5f * (Math.sin(n.time()/90f) + 1f)) > 0.5f ? 1f : 0f
            );

            //Util.pause(1);
        });

        int runtime = 100000;

        a.runCycles(runtime);

        return a.rewardSum / runtime;

    };


    public static class Line1DOptimize {
        public static void main(String[] args) {

            Param.ANSWER_REPORTING = false;
            Object d = DefaultDeriver.the;

            int maxIterations = 1000;
            int repeats = 2;

            Optimize.Result r = new Optimize<NAR>(() -> {

                Default n = new Default();

                n.time.dur(1);

                n.DEFAULT_BELIEF_PRIORITY = 0.5f;
                n.DEFAULT_GOAL_PRIORITY = 0.75f;
                n.DEFAULT_QUESTION_PRIORITY = 0.25f;
                n.DEFAULT_QUEST_PRIORITY = 0.5f;

                return n;
            }).tweak("beliefConf", 0.5f, 0.95f, 0.1f, (y, x) -> {
                x.beliefConfidence(y);
            }).tweak("goalConf", 0.5f, 0.95f, 0.1f, (y, x) -> {
                x.goalConfidence(y);
            }).tweak("termVolMax", 7, 32, 4, (y, x) -> {
                x.termVolumeMax.setValue(y);
            }).tweak("exeRate", 0.05f, 0.5f, 0.1f, (y, x) -> {
                ((TaskExecutor) x.exe).exePerCycleMax.setValue(y);
            }).run(maxIterations, repeats, experiment);

            r.print();

        }

        public static class Line1DVis {


            public static void main(String[] args) {

//        a.nar.onTask(t -> {
//            if (!t.isInput() && t instanceof DerivedTask && t.isGoal()) {
//                System.err.println(t.proof());
//            }
//        });
                Default n = new Default();

                Line1DSimplest a = new Line1DSimplest(n);
                a.init();

                float freq = 1 / 350f;
                float speed = 0.1f;
                a.speed.setValue(speed);

                a.out.resolution.setValue(speed * 1);
                a.in.resolution.setValue(speed * 1);
                a.curiosity.setValue(0.01f);

                //            a.in.beliefs().capacity(0, 100, a.nar);
                //            a.out.beliefs().capacity(0, 100, a.nar);
                //            a.out.goals().capacity(0, 100, a.nar);

                //Line1DTrainer trainer = new Line1DTrainer(a);

                //new RLBooster(a, new HaiQAgent(), 5);

                //ImplicationBooster.implAccelerator(a);


                a.onFrame((z) -> {

                    a.target(
                            (float) (0.5f * (Math.sin(a.nar.time() * freq * 2 * PI) + 1f))
                            //Util.sqr((float) (0.5f * (Math.sin(n.time()/90f) + 1f)))
                            //(0.5f * (Math.sin(n.time()/90f) + 1f)) > 0.5f ? 1f : 0f
                    );

                    //Util.pause(1);
                });

                int runtime = 20000;



                new Thread(() -> {
                    //NAgentX.chart(a);
                    int history = 5000;
                    window(
                            row(
                                    conceptPlot(a.nar, Lists.newArrayList(
                                            () -> (float) a.in.sensor.getAsDouble(),
                                            () -> a.o.floatValue(),
                                            () -> a.reward,
                                            () -> a.rewardSum

                                            )
                                            ,
                                            history),
                                    col(
                                            new Vis.EmotionPlot(history, a),
                                            new ReflectionSurface<>(a),
                                            Vis.beliefCharts(history,
                                                    Lists.newArrayList(a.in, a.out)
                                                    , a.nar)
                                    )
                            )
                            , 900, 900);

                }).start();
                                a.runCycles(runtime);

            }

            public static Grid conceptPlot(NAR nar, Iterable<FloatSupplier> concepts, int plotHistory) {

                //TODO make a lambda Grid constructor
                Grid grid = new Grid(VERTICAL);
                List<Plot2D> plots = $.newArrayList();
                for (FloatSupplier t : concepts) {
                    Plot2D p = new Plot2D(plotHistory, Plot2D.Line /*BarWave*/);
                    p.add(t.toString(), () -> t.asFloat(), 0f, 1f);
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

        public static class Line1DTrainer {

            public static final int trainingRounds = 20;
            private float lastReward;
            int consecutiveCorrect = 0;
            int lag = 0;
            //int perfect = 0;
            int step = 0;


            final LinkedHashSet<Task>
                    current = new LinkedHashSet();

            private final Line1DSimplest a;

            //how long the correct state must be held before it advances to next step
            int completionThreshold;

            float worsenThreshold;

            public Line1DTrainer(Line1DSimplest a) {
                this.a = a;
                this.lastReward = a.reward;

                NAR n = a.nar;
                a.speed.setValue(0.02f);
                a.target(0.5f); //start

                float speed = a.speed.floatValue();
                this.worsenThreshold = speed / 2f;
                this.completionThreshold = n.dur() * 32;
                float rewardThresh = 0.75f; //reward to be considered correct in this frame

                n.onTask(x -> {
                    if (step > trainingRounds && x.isGoal() && !x.isInput()

                        //&& x.term().equals(a.out.term())
                            ) {
                        current.add(x);
                    }
                });

                a.onFrame((z) -> {


                    //System.out.println(a.reward);
                    if (a.reward > rewardThresh)
                        consecutiveCorrect++;
                    else
                        consecutiveCorrect = 0; //start over

                    if (consecutiveCorrect > completionThreshold) {
                        //int lagCorrected = lag - perfect;
                        System.out.println(lag);

                        float next = Util.round(n.random().nextFloat(), speed);
                        //perfect = (int) Math.floor((next - a.target()) / speed);
                        a.target(next);

                        step++;
                        consecutiveCorrect = 0;
                        lag = 0;

                        if (step < trainingRounds) {
                            //completionThreshold += n.dur(); //increase completion threshold
                        } else {
                            if (a.curiosity.floatValue() > 0)
                                System.err.println("TRAINING FINISHED - DISABLING CURIOSITY");
                            a.curiosity.setValue(0f); //disable curiosity
                        }
                    } else {

                        if (lag > 1) { //skip the step after a new target has been selected which can make it seem worse

                            float worsening = lastReward - a.reward;
                            if (step > trainingRounds && worsening > worsenThreshold) {
                                //print tasks suspected of faulty logic
                                current.forEach(x -> {
                                    System.err.println(worsening + "\t" + x.proof());
                                });
                            }
                        }

                        lag++;

                    }

                    lastReward = a.reward;

                    current.clear();
                });
            }

        }
    }


}


//    private static class InteractiveFirer extends FireConcepts.DirectConceptBagFocus {
//
//        private Premise premise;
//
//        public InteractiveFirer(NAR n) {
//            super(n, ((Default) n).newConceptBag(1024), ((Default) n).newPremiseBuilder());
//        }
//
//        final Set<Task> derived = new HashSet(1024);
//
//        @Override
//        protected synchronized void cycle() {
//
//            new PremiseMatrix(1, 1, new MutableIntRange(1,1)).accept(nar);
//
//            if (!derived.isEmpty()) {
//                System.out.println(premise);
//
//                List<Task> l = new FasterList(derived);
//                l.sort((a, b)->{
//                   int x = Float.compare(b.budget().pri(), a.pri());
//                   if (x == 0)
//                       return 1;
//                   else
//                       return x;
//                });
//
//                derived.clear();
//
//                for (Task x : l) {
//                    System.out.println("\t" + x);
//                }
//                try {
//                    System.in.read();
//                } catch (IOException e) {
//
//                }
//            }
//        }
//
//
//
//        @Override
//        public void accept(DerivedTask derivedTask) {
//            //nar.input(derivedTask);
//            premise = derivedTask.premise;
//            derived.add(derivedTask);
//        }
//    }
