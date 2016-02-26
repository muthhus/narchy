package nars.term;

import nars.concept.DefaultConceptBuilder;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by me on 2/25/16.
 */
public class SymbolMapTest {

    @Test
    public void testAtomInsertion() {

        SymbolMap tree = new SymbolMap(new DefaultConceptBuilder());

        //int start = SymbolMap.getLastSerial();

        tree.resolveOrAdd("concept");
        tree.resolveOrAdd("term");
        tree.resolveOrAdd("termutator");
        tree.print(System.out);

        assertNotNull(tree.resolve("term"));
        assertNull(tree.resolve("xerm"));
        assertNull(tree.resolve("te")); //partial

        assertNotNull(tree.resolveOrAdd("term"));
        assertEquals(3, tree.size());

        assertNotNull(tree.resolveOrAdd("termunator"));

        tree.print(System.out);

        assertEquals(4, tree.size());


//        String stringWithUnicode = "unicode\u00easomething";
//        assertNull( tree.resolveOrAdd(stringWithUnicode)); //unicode not supported yet

    }


}