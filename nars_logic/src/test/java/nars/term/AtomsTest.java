package nars.term;

import com.googlecode.concurrenttrees.common.PrettyPrinter;
import com.googlecode.concurrenttrees.radix.node.util.PrettyPrintable;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by me on 2/25/16.
 */
public class AtomsTest {

    @Test
    public void testAtomInsertion() {

        Atoms tree = new Atoms();

        tree.resolveOrAdd("concept");
        tree.resolveOrAdd("term");
        tree.resolveOrAdd("termutator");

        assertEquals(2, tree.resolveOrAdd("term"));
        assertEquals(-1, tree.resolve("xerm"));

        String stringWithUnicode = "unicode\u00easomething";
        assertEquals(4, tree.resolveOrAdd(stringWithUnicode));

        tree.print();
    }


}