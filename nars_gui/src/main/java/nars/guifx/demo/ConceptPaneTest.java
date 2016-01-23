package nars.guifx.demo;

import nars.NAR;
import nars.concept.Concept;
import nars.guifx.NARfx;
import nars.nar.Default;
import nars.util.Texts;



/**
 * Created by me on 1/21/16.
 */
public class ConceptPaneTest {

    public static final int beliefCapacity = 32;
    static float waveFreq = 0.12715f;
    static float conf = 0.5f;
    static float predictionProbability = 0.5f; //how often to ask for a prediction
    static float pastProbability = 0.5f; //how often to ask for a prediction
    static final float fps = 30f;
    static boolean enableEternals = false;
    static boolean enableTemporals = true;

    public static void main(String[] args) {
        NAR nar = new Default();
        //nar.input("y:x.");
        //nar.input("y:x. %0%");
        nar.memory.conceptBeliefsMax.set(beliefCapacity);
        nar.run(2);
        nar.memory.duration.set(10);
        nar.onEachFrame(n-> {

            //random eternals
            if (enableEternals) {
                //float ef = nar.memory.random.nextFloat();
                //float ec = nar.memory.random.nextFloat() * 0.5f;
                float ef = 0.5f * ((float) Math.sin(nar.time() * waveFreq) + 1f);
                float ec = 0.5f * (float)Math.random(); //0.1f;
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


        Concept c = nar.concept("y:x");


        NARfx.run((a,s)->{
            NARfx.newWindow(nar, c);


            nar.loop(fps);
        });
    }
}
