package nars.experiment;

import jcog.Util;
import nars.NAR;
import nars.NAgentX;
import nars.Param;
import nars.Task;
import nars.concept.ActionConcept;
import nars.nar.Default;
import nars.test.agent.Line1DSimplest;

import java.util.LinkedHashSet;

/**
 * Created by me on 3/15/17.
 */
public class Line1D {

    public static class Line1DTrainer {

        public static final int trainingRounds = 20;
        private float lastReward;
        float rewardSum = 0;
        int lag = 0;
        int perfect = 0;
        int step = 0;


        final LinkedHashSet<Task>
                current = new LinkedHashSet();

        private final Line1DSimplest a;
        float completionThreshold;
        float worsenThreshold;

        public Line1DTrainer(Line1DSimplest a) {
            this.a = a;
            this.lastReward = a.reward;

            NAR n = a.nar;
            a.speed.setValue(0.05f);
            a.target(0.5f); //start

            float speed = a.speed.floatValue();
            this.worsenThreshold = speed * 2;
            this.completionThreshold = n.dur();

            n.onTask(x -> {
                if (step > trainingRounds && x.isGoal() && !x.isInput()
                        && !(x instanceof ActionConcept.CuriosityTask)
                        && x.term().equals(a.out.term())
                ) {
                    current.add(x);
                }
            });

            a.onFrame((z) -> {



                //System.out.println(a.reward);
                rewardSum +=
                        //a.reward
                        Math.abs(a.reward);

                if (rewardSum > completionThreshold) {
                    int lagCorrected = lag - perfect;
                    System.out.println(lagCorrected);

                    float next = Util.round(n.random().nextFloat(), speed);
                    perfect = (int) Math.floor((next - a.target()) / speed);
                    a.target(next);

                    step++;
                    rewardSum = 0;
                    lag = 0;

                    if (step < trainingRounds) {
                        completionThreshold += n.dur(); //increase completion threshold
                    } else {
                        if (a.curiosityProb.floatValue() > 0)
                            System.err.println("TRAINING FINISHED - DISABLING CURIOSITY");
                        a.curiosityProb.setValue(0f); //disable curiosity
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


    public Line1D() {

    }


    public static void main(String[] args) {
        Param.DEBUG = true;


        NAR n = new Default();
        //n.time.dur(4);


        Line1DSimplest a = new Line1DSimplest(n);
        Line1DTrainer trainer = new Line1DTrainer(a);

        //new RLAccel(a, new HaiQAgent());
        //ImplicationBooster.implAccelerator(a);
//        a.onFrame((z) -> {
//            a.target(
//                    0.5f * (Math.sin(n.time() / 200f) + 1f)
//                    //Util.sqr((float) (0.5f * (Math.sin(n.time()/90f) + 1f)))
//                    //(0.5f * (Math.sin(n.time()/90f) + 1f)) > 0.5f ? 1f : 0f
//            );

//        });


        NAgentX.chart(a);

        a.runCycles(50000);


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
