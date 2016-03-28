package nars.task;

import nars.NAR;
import nars.nar.Default;
import nars.util.TermCodec;
import org.junit.Test;
import org.nustaq.serialization.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Set;
import java.util.TreeSet;

import static java.lang.System.out;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by me on 3/28/16.
 */
public class TermCodecTest {




    final static FSTConfiguration conf = TermCodec.the;


    void assertEqualSerialize(Object orig) {
        byte barray[] = conf.asByteArray(orig);
        out.println(orig + "\n\tserialized: " + barray.length + " bytes");

        Object copy = conf.asObject(barray);
        //if (copy instanceof Task) {
            //((MutableTask)copy).invalidate();
            //((Task)copy).normalize(nar);
            //out.println("\t\t" +((Task)orig).explanation());
            //out.println("\t\t" +((Task)copy).explanation());
        //}
        //System.out.println("\tbytes: " + Arrays.toString(barray));
        out.println("\tcopy: " + copy);

        assertTrue(copy != orig);
        assertEquals(copy, orig);
        assertEquals(copy.hashCode(), orig.hashCode());
        assertEquals(copy.getClass(), orig.getClass());
    }

    //    /* https://github.com/RuedigerMoeller/fast-serialization/wiki/Serialization*/
    @Test
    public void testTermSerialization() {
        final NAR nar = new Default();
        assertEqualSerialize(nar.term("<a-->b>").term() /* term, not the concept */);
        assertEqualSerialize(nar.term("<aa-->b>").term() /* term, not the concept */);
        assertEqualSerialize(nar.term("<aa--><b<->c>>").term() /* term, not the concept */);
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
        out.println("NAR tasks serialized: " + x.length + " bytes");

        NAR b = new Default()
                        .input(new ByteArrayInputStream(x))
                        .step()
                        //.forEachConceptTask(true,true,true,true, out::println)
                        //.forEachConcept(System.out::println)
                        ;

        //dump all tasks to a set of sorted strings and compare their equality:
        Set<String> ab = new TreeSet();
        Set<String> bb = new TreeSet();
        a.forEachConceptTask(true,true,true,true, t->ab.add(t.toString()));
        b.forEachConceptTask(true,true,true,true, t->bb.add(t.toString()));
        assertEquals(ab, bb);
    }

}
