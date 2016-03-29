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

	// static {
	// Global.HORIZON = 2f;
	// }
	public static final int beliefCapacity = 80;
	static float waveFreq = 0.08715f;
	static float conf = 0.9f;
	static float predictionProbability = 0.1f; // how often to ask for a
												// prediction
	static float pastProbability = 0.1f; // how often to ask for a prediction
	static final float fps = 20f;
	static boolean enableEternals = false;
	static boolean enableTemporals = true;
	static int duration = 2;
	static int stmInduction = 0;

	public static void main(String[] args) {
        NAR nar = new Default();
        //nar.input("y:x.");
        //nar.input("y:x. %0%");
        nar.duration.set(duration);

        nar.shortTermMemoryHistory.set(stmInduction);
        nar.conceptBeliefsMax.set(beliefCapacity);
        nar.run(2);

        Concept c = nar.concept("y:x");

        nar.onFrame(n-> {

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

                if (nar.random.nextFloat() < predictionProbability)
                    nar.input("y:x? :/:");
                else if (nar.random.nextFloat() < pastProbability)
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
