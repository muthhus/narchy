package nars.guifx.demo;

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
	public static final int beliefCapacity = 64;
	static float waveFreq = 0.1715f;

    static float confMax = 0.9f;
    static float confMin = 0.2f;

	static float predictionProbability = 0.1f; // how often to ask for a
												// prediction
	static float pastProbability = 0.1f; // how often to ask for a prediction
	static final float fps = 45f;
	static boolean enableEternals;
	static boolean enableTemporals = true;
	static int stmInduction;

	public static void main(String[] args) {
        Default nar = new Default();
        //nar.input("y:x.");
        //nar.input("y:x. %0%");


        //nar.shortTermMemoryHistory.set(stmInduction);
        nar.conceptWarm.beliefsMaxEteOrTemp.set(beliefCapacity);
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
                    nar.input("y:x. :|: %" + y + ";" + ((float)(Math.random()*(confMax-confMin))+ confMin) + "%");
                }

            }
        });




        NARfx.run((a,s)->{
            NARfx.newWindow(nar, c);


            nar.loop(fps);
        });
    }
}
