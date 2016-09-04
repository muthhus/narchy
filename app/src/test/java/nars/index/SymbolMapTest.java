package nars.index;

import nars.concept.AtomConcept;
import nars.concept.Concept;
import nars.nar.Terminal;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atomic;
import org.junit.Test;

import java.util.function.Function;

import static nars.$.$;
import static org.junit.Assert.*;

/**
 * Created by me on 2/25/16.
 */
public class SymbolMapTest {


    @Test
    public void testAtomInsertion() {

        TermTree tree = new TermTree();

        Function<Term, Concept> cb = (t)->new AtomConcept((Atomic)t, null, null);

        tree.computeIfAbsent("concept", cb);
        tree.computeIfAbsent("term", cb);
        tree.computeIfAbsent("termutator", cb);
        tree.print(System.out);

        assertNotNull(tree.get("term"));
        assertNull(tree.get("xerm"));
        assertNull(tree.get("te")); //partial

        assertNotNull(tree.computeIfAbsent("term", cb));
        assertEquals(3, tree.size());

        assertNotNull(tree.computeIfAbsent("termunator", cb));

        tree.print(System.out);

        assertEquals(4, tree.size());


//        String stringWithUnicode = "unicode\u00easomething";
//        assertNull( tree.resolveOrAdd(stringWithUnicode)); //unicode not supported yet

    }


    @Test
    public void testCompoundInsertion() {

        Terminal nar = new Terminal();
        TreeIndex index = new TreeIndex(nar.index.conceptBuilder());


        index.get($("x"), true);
//        index.putIfAbsent($("(x)"), cb);
//        index.putIfAbsent($("(xx)"), cb);
//        index.putIfAbsent($("(xxx)"), cb);
//        index.putIfAbsent($("(x,y)"), cb);
//        index.putIfAbsent($("(x,z)"), cb);
//        index.putIfAbsent($("(x-->z)"), cb);
//        index.putIfAbsent($("(x<->z)"), cb);
//        index.putIfAbsent($("(x && z)"), cb);
//        index.putIfAbsent($("(/, x, _)"), cb);
//        index.putIfAbsent($("(/, _, x)"), cb);

        assertEquals(11, index.size());

        index.print(System.out);

//        String stringWithUnicode = "unicode\u00easomething";
//        assertNull( tree.resolveOrAdd(stringWithUnicode)); //unicode not supported yet

    }
}