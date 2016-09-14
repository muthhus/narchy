package nars.rdfowl;

import nars.NAR;
import nars.Param;
import nars.experiment.tetris.Tetris;
import nars.index.TreeIndex;
import nars.nar.Default;
import nars.nar.exe.Executioner;
import nars.nar.util.DefaultConceptBuilder;
import nars.time.FrameClock;
import nars.util.data.random.XorShift128PlusRandom;
import org.junit.Ignore;
import org.junit.Test;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Random;

import static junit.framework.TestCase.assertTrue;

/**
 * Created by me on 9/13/16.
 */
public class NQuadsRDFTest {

    @Test
    public void test1() throws Exception {
        NAR n = new Default();
        //n.stdout();
        NQuadsRDF.input(n, "<http://example.org/#spiderman> <http://xmlns.com/foaf/0.1/name> \"Человек-паук\"@ru .");
        n.run(1);
        assertTrue(n.index.size() > 2);
    }

    @Ignore
    @Test
    public void testSchema1() throws Exception {
        Param.DEBUG_ANSWERS = true;

        Random rng = new XorShift128PlusRandom(1);
        //Multi nar = new Multi(3,512,
        Executioner e = Tetris.exe;
        Default n = new Default(1024,
                72, 2, 2, rng,
                //new CaffeineIndex(new DefaultConceptBuilder(rng), DEFAULT_INDEX_WEIGHT, false, e),
                new TreeIndex.L1TreeIndex(new DefaultConceptBuilder(rng), 32768, 3),
                new FrameClock(), e
        );

        File output = new File("/tmp/onto.nal");
        PrintStream pout = new PrintStream(new BufferedOutputStream(new FileOutputStream(output), 512*1024));

        n.input(
                NQuadsRDF.stream(n, new File(
                        //"/tmp/all-layers.nq"
                        "/home/me/Downloads/nquad"
                )).map(t -> {
                    pout.println(t.term().toString() + t.punc());
                    //t.budget(0, 0.5f);
                    return t;
                } )
        );

        pout.close();

//        n.forEachActiveConcept(c -> {
//            c.print();
//        });

        n.run(1);
        n.core.concepts.clear();
        n.log();
        n.input("$0.9$ (Bacteria <-> Pharmacy)?");


        //Param.DEBUG = true;

        n.run(128);

//        n.index.forEach(c -> {
//            System.out.println(c);
//        });
    }
}