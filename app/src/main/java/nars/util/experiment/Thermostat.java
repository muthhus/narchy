package nars.util.experiment;

import com.gs.collections.api.tuple.Twin;
import com.gs.collections.impl.tuple.Tuples;
import nars.NAR;
import nars.nar.Default;
import nars.util.Agent;
import nars.util.DQN;
import nars.util.NAgent;
import nars.util.Optimize;
import nars.util.data.Util;
import org.apache.commons.lang3.mutable.MutableFloat;

import static java.lang.System.out;

/**
 * Created by me on 5/4/16.
 */
public class Thermostat implements Environment {


    public float targetPeriod = 15;
    public final float speed = 0.17f;
    boolean print = false;
    private MutableFloat yHidden;
    private MutableFloat yEst;
    boolean enableAbsolute = false; //additional inputs

    @Override public Twin<Integer> start() {

        //Global.DEBUG = true;


        yEst = new MutableFloat(0.5f); //NAR estimate of Y
        yHidden = new MutableFloat(0.5f); //actual best Y used by loss function



        yEst.setValue(0.5f);


        return Tuples.twin(2 + (enableAbsolute ? 2 : 0), 3);
    }

    @Override
    public float cycle(int t, int aa, float[] ins, Agent a) {

        if (print) {

            int cols = 50;
            int colActual = Math.round(cols * yHidden.floatValue());
            int colEst = Math.round(cols * yEst.floatValue());
            for (int i = 0; i <= cols; i++) {

                char c;
                if (i == colActual)
                    c = 'X';
                else if (i == colEst)
                    c = '|';
                else
                    c = '.';

                out.print(c);
            }

            out.print(' ');
            out.print(a.summary());
            out.println();
        }


        float diff = yHidden.floatValue() - yEst.floatValue();
        ins[0] = Util.clamp(diff);
        ins[1] = Util.clamp(-diff);

        if (enableAbsolute) {
            ins[2] = Util.clamp(yHidden.floatValue());
            ins[3] = Util.clamp(yEst.floatValue());
        }

        float dist =  Math.abs(yHidden.floatValue() - yEst.floatValue());

        float reward = 1f-dist;
        //float reward = dist < speed ? (0.5f/(1f+dist)) : -dist;
        //float reward = -dist + 0.1f;
        //float reward = 1f / (1+dist*dist);

        float de;
        switch (aa) {
            case 1:
                de = 1f * speed;
                break;
            case 2:
                de = -1f * speed;
                break;
//                case 3:
//                    de = 1f * speed/4f;
//                    break;
//                case 4:
//                    de = -1f * speed/4f;
//                    break;
            case 0:
            default:
                de = 0f; //nothing
                break;
        }

        yEst.setValue( Util.clamp(yEst.floatValue() + de) );

        yHidden.setValue( Util.clamp(function(t)) );

        return reward;

    }

    public float function(int t) {
        float a = 0.95f;
        return 0.5f +
                0.5f * (a * (float)Math.sin(t / (targetPeriod))) +
                0.05f * (a * (float)Math.cos(t / (targetPeriod/3f))-1)
                ;
        //return 0.5f + 0.5f * (float)Math.tan(t / (targetPeriod)) + (float)Math.random()*0.1f;
    }

    public static void main(String[] args) {

        int cycles = 1500;

        float b = new Thermostat().run(
                new DQN(),
                cycles
        );
        System.out.println("DQN baseline = " + b);

        Optimize.Result r = new Optimize<NAR>(() -> new Default())

                .call("beliefConf", 0.1f, 0.95f, 0.1f, "beliefConfidence(#x)")
                .call("goalConf", 0.1f, 0.95f, 0.1f, "goalConfidence(#x)")


                .call("conceptsPerCyc", 2, 3, 1f, "core.conceptsFiredPerCycle.setValue(#i)")
                .call("termLinksPerConcept", 1, 3, 1f, "premiser.termlinksFiredPerFiredConcept.setValue(#i)")

                .call("cycPerFrame", 4, 24, 1f, "cyclesPerFrame.setValue(#i)")

                .call("conceptRem", 1f, 6f, 0.25f, "conceptRemembering.setValue(#x)")
                .call("taskRem",    1f, 8f, 0.25f, "taskLinkRemembering.setValue(#x)")
                .call("termRem",    1f, 8f, 0.25f, "termLinkRemembering.setValue(#x)")

                //((DefaultConceptBuilder)new Default(512, 1, 1, 3).index.conceptBuilder()).termLinkBagSize

                .call("conceptAct", 0.1f, 0.8f, 0.05f,  "conceptActivation.setValue(#x)")

                .run(3500, (x) ->
                    new Thermostat().run(new NAgent(x), cycles)
                );

        System.out.println();
        r.print();


//        //for (int cycPerFrame : new int[] { 1, 2, 3, 4, 5, 6, 7, 8}) {
//        for (float cRem : new float[] { 1, 2, 3, 4, 5, 6, 7, 8}) {
//            System.out.println("concept remembering: " + cRem );
//            //System.out.println("cycperFrame: " + cycPerFrame );
//


//            //n.cyclesPerFrame.setValue(cycPerFrame);
//            //n.conceptRemembering.setValue(cRem);
//            //n.conceptActivation.setValue(0.5);
//            //n.cyclesPerFrame.set(2);
//            //n.shortTermMemoryHistory.set(3);
//            //n.logSummaryGT(System.out, 0.55f);


//            n.conceptRemembering.setValue(cRem);
//
//            NAgent a = //new NAgentDebug(n);
//                    new NAgent(n);
//
//            new Thermostat().run(
//                    //new DQN(),
//                    a,
//                    cycles
//            );
//        }

        /*printTasks(n, true);
        printTasks(n, false);*/
    }


}
