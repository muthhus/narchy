package nars.task;

import nars.NAR;
import nars.nar.Default;
import nars.nar.Terminal;
import nars.term.Termed;
import nars.util.IO;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import static java.lang.System.out;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Term serialization
 */
public class TermIOTest {

    final NAR nar = new Terminal();

    void assertEqualSerialize(@NotNull Object orig) {
        final IO.DefaultCodec codec = new IO.DefaultCodec(nar.index);

        byte barray[] = codec.asByteArray(orig);
        out.println(orig + "\n\tserialized: " + barray.length + " bytes " + Arrays.toString(barray));


        Object copy = codec.asObject(barray);
        //if (copy instanceof Task) {
            //((MutableTask)copy).invalidate();
            //((Task)copy).normalize(nar);
            //out.println("\t\t" +((Task)orig).explanation());
            //out.println("\t\t" +((Task)copy).explanation());
        //}
        //System.out.println("\tbytes: " + Arrays.toString(barray));
        out.println("\tcopy: " + copy);

        //assertTrue(copy != orig);
        assertEquals(copy, orig);
        assertEquals(copy.hashCode(), orig.hashCode());
        //assertEquals(copy.getClass(), orig.getClass());
    }


    //    /* https://github.com/RuedigerMoeller/fast-serialization/wiki/Serialization*/
    @Test
    public void testTermSerialization() {

        assertEqualSerialize(nar.term("<a-->b>").term() /* term, not the concept */);
        assertEqualSerialize(nar.term("<aa-->b>").term() /* term, not the concept */);
        assertEqualSerialize(nar.term("<aa--><b<->c>>").term() /* term, not the concept */);
        assertEqualSerialize(nar.term("(a &&+1 b)").term() /* term, not the concept */);
        assertEqualSerialize(nar.term("(/, x, _, y)").term() /* term, not the concept */);
        assertEqualSerialize(nar.term("exe(a,b)").term() /* term, not the concept */);
    }
    @Test
    public void testTermSerialization2() {
        assertTermEqualSerialize("<a-->(b==>c)>");
        assertTermEqualSerialize("(#a --> b)");
    }

    void assertTermEqualSerialize(@NotNull String s) {
        Termed t = nar.term(s);
        assertTrue(t.isNormalized());
        assertTrue(t.term().isNormalized());
        assertEqualSerialize(t.term() /* term, not the concept */);
    }

    @Test
    public void testTaskSerialization() {
        assertEqualSerialize(nar.inputTask("<a-->b>."));
        assertEqualSerialize(nar.inputTask("<a-->(b==>c)>!"));
        assertEqualSerialize(nar.inputTask("<a-->(b==>c)>?"));
        assertEqualSerialize(nar.inputTask("$0.1;0.2;0.4$ (b-->c)! %1.0;0.8%"));
    }

    @Test public void testTaskSerialization2() {
        assertEqualSerialize(nar.inputTask("$0.3;0.2;0.1$ <a-->(b==>c)>! %1.0;0.8%"));
    }

    @Test public void testNARTaskDump() throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        NAR a = new Default()
                        .input("a:b.", "b:c.", "c:d!")
                        .run(32)
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
        a.forEachConceptTask(t->ab.add(t.toString()), true,true,true,true);
        b.forEachConceptTask(t->bb.add(t.toString()), true,true,true,true);
        assertEquals(ab, bb);
    }

}
