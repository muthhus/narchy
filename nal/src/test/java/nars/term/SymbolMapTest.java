package nars.term;

import nars.concept.AtomConcept;
import nars.concept.Concept;
import nars.term.atom.Atomic;
import org.junit.Test;

import java.util.function.Function;

import static org.junit.Assert.*;

/**
 * Created by me on 2/25/16.
 */
public class SymbolMapTest {

    @Test
    public void testAtomInsertion() {

        RadixTreeSymbolMap tree = new RadixTreeSymbolMap();

        //int start = SymbolMap.getLastSerial();

        Function<Term, Concept> cb = (t)->new AtomConcept((Atomic)t, null, null);

        tree.resolveOrAdd("concept", cb);
        tree.resolveOrAdd("term", cb);
        tree.resolveOrAdd("termutator", cb);
        tree.print(System.out);

        assertNotNull(tree.resolve("term"));
        assertNull(tree.resolve("xerm"));
        assertNull(tree.resolve("te")); //partial

        assertNotNull(tree.resolveOrAdd("term", cb));
        assertEquals(3, tree.size());

        assertNotNull(tree.resolveOrAdd("termunator", cb));

        tree.print(System.out);

        assertEquals(4, tree.size());


//        String stringWithUnicode = "unicode\u00easomething";
//        assertNull( tree.resolveOrAdd(stringWithUnicode)); //unicode not supported yet

    }


}