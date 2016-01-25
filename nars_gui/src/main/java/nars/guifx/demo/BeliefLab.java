package nars.guifx.demo;

import nars.NAR;
import nars.concept.Concept;
import nars.guifx.NARfx;
import nars.nar.Default;
import nars.util.Texts;



/**
 * Created by me on 1/21/16.
 */
public class BeliefLab {

    public static final int beliefCapacity = 192;
    static float waveFreq = 0.152715f;
    static float conf = 0.8f;
    static float predictionProbability = 0.3f; //how often to ask for a prediction
    static float pastProbability = 0.3f; //how often to ask for a prediction
    static final float fps = 30f;
    static boolean enableEternals = true;
    static boolean enableTemporals = false;
    static int duration = 3;
    static int stmInduction = 0;


    public static void main(String[] args) {
        NAR nar = new Default();
        //nar.input("y:x.");
        //nar.input("y:x. %0%");
        nar.memory.duration.set(duration);

        nar.memory.shortTermMemoryHistory.set(stmInduction);
        nar.memory.conceptBeliefsMax.set(beliefCapacity);
        nar.run(2);

        Concept c = nar.concept("y:x");

        nar.onEachFrame(n-> {

            //random eternals
            if (enableEternals) {
                //float ef = nar.memory.random.nextFloat();
                //float ec = nar.memory.random.nextFloat() * 0.5f;

                float ef = 0.5f * ((float) Math.sin(nar.time() * waveFreq) + 1f);
                //float ef = Math.random() < 0.5 ? 1f : 0f; //biphasic

                float ec = 0.4f * (float)Math.random(); //0.1f;
                nar.input("y:x. %" + Texts.n2(ef) + ";" + Texts.n2(ec) + "%");
            }

            //random temporals
            if (enableTemporals) {

                if (nar.memory.random.nextFloat() < predictionProbability)
                    nar.input("y:x? :/:");
                else if (nar.memory.random.nextFloat() < pastProbability)
                    nar.input("y:x? :\\:");
                else {
                    CharSequence y = Texts.n2(0.5f * ((float) Math.sin(nar.time() * waveFreq) + 1f));
                    nar.input("y:x. :|: %" + y + ";" + conf + "%");
                }

            }
        });




        NARfx.run((a,s)->{
            NARfx.newWindow(nar, c);


            nar.loop(fps);
        });
    }
}
