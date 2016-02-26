package nars.term;

import com.googlecode.concurrenttrees.common.PrettyPrinter;
import com.googlecode.concurrenttrees.radix.node.util.PrettyPrintable;
import nars.concept.DefaultConceptBuilder;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by me on 2/25/16.
 */
public class AtomsTest {

    @Test
    public void testAtomInsertion() {

        Atoms tree = new Atoms(new DefaultConceptBuilder());

        int start = tree.getLastSerial();

        tree.resolveOrAdd("concept");
        tree.resolveOrAdd("term");
        tree.resolveOrAdd("termutator");
        tree.print(System.out);

        assertNotNull(tree.resolve("term"));
        assertNull(tree.resolve("xerm"));
        assertNull(tree.resolve("te")); //partial

        assertNotNull(tree.resolveOrAdd("term"));

        assertNotNull(tree.resolveOrAdd("termunator"));

        tree.print(System.out);

        assertEquals(4, tree.getLastSerial() - start);


//        String stringWithUnicode = "unicode\u00easomething";
//        assertNull( tree.resolveOrAdd(stringWithUnicode)); //unicode not supported yet

    }


}