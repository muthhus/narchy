package nars.logic;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.Param;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;

import static nars.time.Tense.ETERNAL;

/**
 * Created by me on 4/17/17.
 */
public class TestProloglike {

//    static {
//        Param.TRACE = true;
//    }

    /** http://www.doc.gold.ac.uk/~mas02gw/prolog_tutorial/prologpages/rules.html */
    @Test public void testProloglike1() throws Narsese.NarseseException {
        Param.DEBUG = true;
        NAR n = NARS.tmp();
        /*
        fun(X) :- red(X), car(X).
        fun(X) :- blue(X), bike(X).
        car(vw_beatle).
        car(ford_escort).
        bike(harley_davidson).
        red(vw_beatle).
        red(ford_escort).
        blue(harley_davidson).
        */

        n.truthResolution.setValue(0.1f);
        n.believe(
            "((red($x) && car($x))==>fun($x))",
            "((blue($x) && bike($x))==>fun($x))",
            "(car($x) <=> (--,bike($x)))",
            "(red($x) <=> (--,blue($x)))",
            "car(vw_beatle)", "car(ford_escort)", "bike(harley_davidson)", "red(vw_beatle)", "blue(ford_escort)", "blue(harley_davidson)"
        );
        //n.log();
        n.DEFAULT_QUESTION_PRIORITY = 0.99f;
        n.question("fun(?x)", ETERNAL, (q,a)->{
            //System.out.println(a.term() + " " + a.truth());
            System.out.println(a.proof());
        });
        n.run(1000);



    }

    @Test
    public void testRiddle1() throws IOException, Narsese.NarseseException {
        //Param.DEBUG = true;
        NAR n = NARS.tmp();

        n.termVolumeMax.setValue(1024);
        n.log();
        URL resource = TestProloglike.class.getResource("einsteinsRiddle.nal");
        n.inputNarsese(
            resource.openStream()
        );
        n.run(128);



    }


}
