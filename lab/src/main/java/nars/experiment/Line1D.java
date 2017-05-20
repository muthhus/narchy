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
                        && !(x instanceof ActionConcept.CuriosityTask)
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
                        if (a.curiosityProb.floatValue() > 0)
                            System.err.println("TRAINING FINISHED - DISABLING CURIOSITY");
                        a.curiosityProb.setValue(0f); //disable curiosity
                        a.curiosityConf.setValue(0f);
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
        n.time.dur(4);
        n.termVolumeMax.setValue(24);


        Line1DSimplest a = new Line1DSimplest(n);
        //Line1DTrainer trainer = new Line1DTrainer(a);

        //new RLBooster(a, new HaiQAgent());

//ImplicationBooster.implAccelerator(a);
        a.onFrame((z) -> {
            a.target(
                    (float) (0.5f * (Math.sin(n.time() / 500f) + 1f))
                    //Util.sqr((float) (0.5f * (Math.sin(n.time()/90f) + 1f)))
                    //(0.5f * (Math.sin(n.time()/90f) + 1f)) > 0.5f ? 1f : 0f
            );

        });


        NAgentX.chart(a);

        a.runCycles(5000000);


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
