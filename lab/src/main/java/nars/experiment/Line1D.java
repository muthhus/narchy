package nars.experiment;

import com.google.common.collect.Lists;
import jcog.Optimize;
import jcog.Util;
import jcog.math.FloatSupplier;
import jcog.net.MeshOptimize;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.gui.Vis;
import nars.nar.NARS;
import nars.op.stm.STMTemporalLinkage;
import nars.test.agent.Line1DSimplest;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.intelligentjava.machinelearning.decisiontree.RealDecisionTree;
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


    static class Line1DExperiment implements FloatFunction<NAR> {
        float tHz = 0.003f; //in time units
        float yResolution = 0.04f; //in 0..1.0
        float periods = 532;

        final int runtime = Math.round(periods /tHz);

        @Override
        public float floatValueOf(NAR n) {

            n.truthResolution.setValue(0.05f);

            Line1DSimplest a = new Line1DSimplest(n) {

//                final FloatAveraged rewardAveraged = new FloatAveraged(()->super.act(), 10);

            };

            a.init();

            onStart(a);



            a.speed.setValue(yResolution);

            a.happy.resolution.setValue(0.01f);
            a.out.resolution.setValue(yResolution);
            a.in.resolution.setValue(yResolution);
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
                        Util.round((float)   ( 0.5f + 0.5f * Math.sin(a.nar.time() * tHz * 2 * PI) ), yResolution)
                        //(float) ( Math.sin(a.nar.time() * tHz * 2 * PI) )
                        //Util.sqr((float) (0.5f * (Math.sin(n.time()/90f) + 1f)))
                        //(0.5f * (Math.sin(n.time()/90f) + 1f)) > 0.5f ? 1f : 0f
                );

                //Util.pause(1);
            });


            a.runCycles(runtime);

            return a.rewardSum / runtime;

        }

        protected void onStart(Line1DSimplest a) {

        }
    }


    public static class Line1DOptimize {

        public static void main(String[] args) {

            Param.ANSWER_REPORTING = false;

            int maxIterations = 1024;
            int repeats = 2;

            Optimize<NAR> o = new MeshOptimize<NAR>("d1", () -> {

                NAR n = new NARS().get();
                n.random().setSeed(System.nanoTime());

                n.time.dur(1);
                n.termVolumeMax.set(32);

                return n;
            })/*.tweak("beliefConf", 0.1f, 0.9f, 0.1f, (y, x) -> {
                x.beliefConfidence(y);
            })*/.tweak("goalConf", 0.1f, 0.9f, 0.1f, (y, x) -> {
                x.goalConfidence(y);
            }).tweak("belfPri", 0.1f, 1.0f, 0.1f, (y, x) -> {
                x.DEFAULT_BELIEF_PRIORITY = (y);
            }).tweak("goalPri", 0.1f, 1.0f, 0.1f, (y, x) -> {
                x.DEFAULT_GOAL_PRIORITY = (y);
            }).tweak("questionPri", 0.1f, 1.0f, 0.1f, (y, x) -> {
                x.DEFAULT_QUESTION_PRIORITY = (y);
                x.DEFAULT_QUEST_PRIORITY = (y);
            })

                    /*.tweak("termVolMax", 10, 25, 1, (y, x) -> {
                x.termVolumeMax.setValue(y);
//            }).tweak("exeRate", 0.1f, 0.9f, 0.1f, (y, x) -> {
//                ((TaskExecutor) x.exe).exePerCycleMax.setValue(y);
            })*//*.tweak("activation", 0.1f, 1f, 0.1f, (y, x) -> {
                x.in.streams.values().forEach(s -> s.setValue(y));
            })*//*.tweak("stmSize", 1, 2, 1, (y, x) -> {
                ((Default) x).stmLinkage.capacity.setValue(y);
            })*//*.tweak("confMin", 0.05f, 0.5f, 0.01f, (y, x) -> {
                x.confMin.setValue(y);
            })*//*.tweak("truthResolution", 0.01f, 0.04f, 0.01f, (y, x) -> {
                x.truthResolution.setValue(y);
            })*/
            ;

            Optimize.Result r = o.run(64, maxIterations, repeats, new Line1DExperiment());

            r.print();

            RealDecisionTree t = r.predict(3, o.tweaks.size());
            t.print();

//            float predictedScore = t.get(0, 1, 0, 1, Float.NaN);
//            System.out.println(predictedScore);

            //t.root().recurse().forEach(x -> System.out.println(x));

//            System.out.println(t.leaves().collect(toList()));


        }

        public static class Line1DVis {


            public static void main(String[] args) {

//        a.nar.onTask(t -> {
//            if (!t.isInput() && t instanceof DerivedTask && t.isGoal()) {
//                System.err.println(t.proof());
//            }
//        });
//                InstrumentedExecutor exe =
//                        new InstrumentedExecutor(
//                        new TaskExecutor(256, 0.5f)
//                );
//
                NAR n = new NARS().get();

                //new STMTemporalLinkage(n, 2, false);
                n.time.dur(1);
                n.termVolumeMax.set(22);
                n.beliefConfidence(0.9f);
                n.goalConfidence(0.5f);
                n.onCycle((nn)->{
                    System.out.println(nn.emotion.summary());
                });

                new Line1DExperiment() {
                    @Override
                    protected void onStart(Line1DSimplest a) {
                        new Thread(() -> {
                            //NAgentX.chart(a);
                            int history = 800;
                            window(
                                    row(
                                            conceptPlot(a.nar, Lists.newArrayList(
                                                    () -> (float) a.in.sensor.getAsDouble(),
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
                                                            Lists.newArrayList(a.sensors.get(0).sensor.freq(), a.out)
                                                            , a.nar)
                                            )
                                    )
                                    , 900, 900);

                        }).start();

                    }
                }.floatValueOf(n);


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

        public static class Line1DTrainer {

            public static final int trainingRounds = 20;
            private float lastReward;
            int consecutiveCorrect;
            int lag;
            //int perfect = 0;
            int step;


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
