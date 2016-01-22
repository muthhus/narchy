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

    static float waveFreq = 0.715f;
    static float conf = 0.75f;

    public static void main(String[] args) {
        NAR nar = new Default();
        //nar.input("y:x.");
        //nar.input("y:x. %0%");
        nar.memory.conceptBeliefsMax.set(96);
        nar.run(2);
        nar.onEachFrame(n-> {
            CharSequence y = Texts.n2(0.5f * ((float) Math.sin(nar.time() * waveFreq) + 1f));
            nar.input("y:x. :|: %" + y + ";" + conf + "%");

            //nar.input("y:x? :/:");
        });


        Concept c = nar.concept("y:x");


        NARfx.run((a,s)->{
            NARfx.newWindow(nar, c);

            nar.loop(10f);
        });
    }
}
