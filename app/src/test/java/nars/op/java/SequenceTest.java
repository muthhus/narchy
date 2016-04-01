package nars.op.java;

import nars.$;
import nars.Global;
import nars.NAR;
import nars.nal.Tense;
import nars.nar.Default;
import nars.term.Termed;

/**
 * Created by me on 4/1/16.
 */
public class SequenceTest {

    public static void main(String[] args) {
        Global.DEBUG = true;

        NAR n = new Default(1000, 4, 2, 2);
        n.shortTermMemoryHistory.set(4);

        n.log();

        int repeats = 7;
        int w = 0;
        char c = 0;
        int words = 2;
        for (int i = 0; i < repeats; i++) {
            if (c!=0)
                n.believe((Termed) $.$("hear(" + c +")"), Tense.Present, 0f, 0.9f).step();

            w = (w+1) % words;

            c = (char)('a' + w);
            n.believe((Termed) $.$("hear(" + c +")"), Tense.Present, 1f, 0.9f).run(10);

        }

        n.input("hear(?x)? :/:");
        for (int i = 0; i < 1000; i++) {
            n.step();
            System.out.println( "a=" + n.concept( (Termed)$.$("hear(a)") ).beliefs().truth(n.time(), n.duration()));
            System.out.println( "b=" + n.concept( (Termed)$.$("hear(b)") ).beliefs().truth(n.time(), n.duration()));
        }
    }
}
