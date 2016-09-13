package nars.rdfowl;

import nars.NAR;
import nars.experiment.tetris.Tetris;
import nars.index.CaffeineIndex;
import nars.index.TreeIndex;
import nars.nar.Default;
import nars.nar.exe.Executioner;
import nars.nar.util.DefaultConceptBuilder;
import nars.time.FrameClock;
import nars.util.data.random.XorShift128PlusRandom;
import org.junit.Test;

import java.io.File;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * Created by me on 9/13/16.
 */
public class NQuadsRDFTest {

    @Test
    public void testSchema1() throws Exception {
        Random rng = new XorShift128PlusRandom(1);
        //Multi nar = new Multi(3,512,
        Executioner e = Tetris.exe;
        Default n = new Default(1024,
                72, 2, 2, rng,
                //new CaffeineIndex(new DefaultConceptBuilder(rng), DEFAULT_INDEX_WEIGHT, false, e),
                new TreeIndex.L1TreeIndex(new DefaultConceptBuilder(rng), 32768, 3),
                new FrameClock(), e
        );
        NQuadsRDF.input(n, new File("/tmp/all-layers.nq"));

        n.log();

        n.run(5);

//        n.index.forEach(c -> {
//            System.out.println(c);
//        });
    }
}