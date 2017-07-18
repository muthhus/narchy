package nars.index.term.tree;

import com.google.common.collect.Sets;
import nars.Narsese;
import nars.concept.AtomConcept;
import nars.concept.Concept;
import nars.nar.Terminal;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atom;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static nars.$.$;
import static org.junit.Assert.*;

/**
 * Created by me on 10/14/16.
 */
public class TermTreeTest {

    @Test
    public void testAtomInsertion() throws Narsese.NarseseException {

        TermTree tree = new TermTree();

        Function<Term, Concept> cb = (t)->new AtomConcept((Atom)t, null, null);

        tree.computeIfAbsent(TermKey.term($("concept")), cb);
        tree.computeIfAbsent(TermKey.term($("term")), cb);
        tree.computeIfAbsent(TermKey.term($("termutator")), cb);
        //tree.print(System.out);

        assertNotNull(tree.get(TermKey.term($("term"))));
        assertNull(tree.get(TermKey.term($("xerm"))));
        assertNull(tree.get(TermKey.term($("te")))); //partial

        assertNotNull(tree.computeIfAbsent(TermKey.term($("term")), cb));
        assertEquals(3, tree.size());

        assertNotNull(tree.computeIfAbsent(TermKey.term($("termunator")), cb));

        tree.prettyPrint(System.out);

        assertEquals(4, tree.size());


//        String stringWithUnicode = "unicode\u00easomething";
//        assertNull( tree.resolveOrAdd(stringWithUnicode)); //unicode not supported yet

    }


    @Test
    public void testCompoundInsertion() throws Narsese.NarseseException {

        Terminal nar = new Terminal();
        TreeTermIndex index = new TreeTermIndex(1000);
        index.start(nar);

        String[] terms = {
                "x",
                "(x)", "(xx)", "(xxx)",
                "(x,y)", "(x,z)",
                "(x --> z)", "(x <-> z)",
                "(x&&z)"
        };
        HashSet<Term> input = new HashSet();
        for (String s : terms) {
            @NotNull Term ts = $(s);
            input.add(ts);
            index.get(ts, true);
        }

        assertEquals(terms.length, index.size());


        Set<Termed> stored = StreamSupport.stream(index.concepts.spliterator(), false).collect(Collectors.toSet());

        assertEquals(Sets.symmetricDifference(input, stored) + " = difference", input, stored);

        index.concepts.prettyPrint();
        index.print(System.out);

//        String stringWithUnicode = "unicode\u00easomething";
//        assertNull( tree.resolveOrAdd(stringWithUnicode)); //unicode not supported yet

    }

}