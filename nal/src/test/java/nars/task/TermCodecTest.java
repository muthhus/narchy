package nars.task;

import nars.$;
import nars.NAR;
import nars.Symbols;
import nars.nar.Default;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.truth.DefaultTruth;
import nars.truth.Truth;
import nars.util.TermCodec;
import org.junit.Test;
import org.nustaq.serialization.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by me on 3/28/16.
 */
public class TermCodecTest {




    final static FSTConfiguration conf = TermCodec.the;


    void assertEqualSerialize(Object orig) {
        byte barray[] = conf.asByteArray(orig);
        System.out.println(orig + "\n\tserialized: " + barray.length + " bytes");

        Object copy = conf.asObject(barray);
        //System.out.println("\tbytes: " + Arrays.toString(barray));
        System.out.println("\tcopy: " + copy);

        assertTrue(copy != orig);
        assertEquals(copy, orig);
        assertEquals(copy.getClass(), orig.getClass());
    }

    //    /* https://github.com/RuedigerMoeller/fast-serialization/wiki/Serialization*/
    @Test
    public void testTermSerialization() {
        final NAR nar = new Default();
        assertEqualSerialize(nar.term("<a-->b>").term() /* term, not the concept */);
    }

    @Test
    public void testTaskSerialization() {
        final NAR nar = new Default();
        assertEqualSerialize(nar.inputTask("<a-->b>."));
        assertEqualSerialize(nar.inputTask("<a-->(b==>c)>!"));
    }

    @Test public void testNARTaskDump() throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        NAR a = new Default()
                        .input("a:b.", "b:c.", "c:d!")
                        .run(100)
                        .output(baos);

        byte[] x = baos.toByteArray();
        System.out.println("NAR tasks serialized: " + x.length + " bytes");

        NAR b = new Default()
                        .input(new ByteArrayInputStream(x))
                        .step()
                        .forEachConceptTask(true,true,true,true,System.out::println)
                        //.forEachConcept(System.out::println)
                        ;

    }

}
