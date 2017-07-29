package nars.logic;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.Param;
import nars.exe.BufferedExecutioner;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;

import static nars.$.$;

/**
 * Created by me on 4/17/17.
 */
public class TestProloglike {

//    static {
//        Param.TRACE = true;
//    }

    /** http://www.doc.gold.ac.uk/~mas02gw/prolog_tutorial/prologpages/rules.html */
    @Test public void testProloglike1() throws Narsese.NarseseException {
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
        Param.ANSWER_REPORTING = true;
        n.believe(
            "((red($x) && car($x))==>fun($x))",
            "((blue($x) && bike($x))==>fun($x))",
            "car(vw_beatle)", "car(ford_escort)", "bike(harley_davidson)", "red(vw_beatle)", "red(ford_escort)", "blue(harley_davidson)"
        );
        n.run(1);
        n.concept($("fun($x)")).print();
        n.clear();
        n.log();
        n.input("$0.99 fun(?x)?");
        n.run(100);

    }

    @Test
    public void testRiddle1() throws IOException, Narsese.NarseseException {
        //Param.DEBUG = true;
        NAR n = //new NARS().exe(new BufferedExecutioner(256, 256, 0.25f)).get();
                NARS.tmp();

        n.termVolumeMax.setValue(1024);
        n.log();
        URL resource = TestProloglike.class.getResource("einsteinsRiddle.nal");
        n.inputNarsese(
            resource.openStream()
        );
        n.run(128);



    }


}
