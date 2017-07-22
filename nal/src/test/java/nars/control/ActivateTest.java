package nars.control;

import jcog.pri.PLink;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.concept.Concept;
import nars.term.Termed;
import nars.term.atom.Atom;
import org.eclipse.collections.api.tuple.primitive.ObjectIntPair;
import org.eclipse.collections.impl.bag.mutable.HashBag;
import org.junit.Test;

import java.util.Arrays;

import static nars.$.$;
import static org.junit.Assert.assertEquals;

public class ActivateTest {

    @Test
    public void testConceptFireLinkSelection() throws Narsese.NarseseException {
        NAR nar = new NARS().get();
        nar.input("$0.01 a:b."); //low priority so it doesnt affect links
        nar.run(1);
        Concept c = nar.conceptualize("a:b");
        for (int n = 0; n < 5; n++) {
            c.termlinks().put(new PLink<>($(n + ":a"), 0.2f * n));
        }

        HashBag<String> s = new HashBag();
        Activate cf = new Activate(c, 1f) {
            @Override
            protected void premise(Premise p, NAR nar) {
                //System.out.println("tasklink=" + tasklink + " termlink=" + termlink);
                if (p.termLink.get() instanceof Atom)
                    return ; //ignore
                String tls = p.termLink.get().toString();
                s.addOccurrences(/*tasklink.get() + " " +*/ tls, 1);
            }
        };

        for (int i = 0; i < 500; i++)
            cf.run(nar);

        s.forEachWithOccurrences((x,o)->{
            System.out.println(o + "\t" + x);
        });

        System.out.println();

        c.print();

        System.out.println();

        ObjectIntPair<String> top = s.topOccurrences(1).get(0);
        ObjectIntPair<String> bottom = s.bottomOccurrences(1).get(0);
        assertEquals("(a-->1)", bottom.getOne());
        assertEquals("(a-->4)", top.getOne());

    }

        @Test
    public void testDerivedBudgets() throws Narsese.NarseseException {

        NAR n= new NARS().get();

        //TODO System.err.println("TextOutput.out impl in progress");
        //n.stdout();


        n.input("$0.1$ <a --> b>.");
        n.input("$0.1$ <b --> a>.");
        n.run(15);


        n.forEachConceptActive(System.out::println);
    }

    @Test public void testTemplates() throws Narsese.NarseseException {

        //layer 1:
        testTemplates("open:door",
                "[door, open]");

        //layer 2:
        testTemplates("open(John,door)",
                "[(John,door), open, door, John]");

        //layer 3:
        testTemplates("(open(John,door) ==> #x)",
                "[(John,door), open, #1, open(John,door), door, John]");

        //dont descend past layer 3:
        testTemplates("(open(John,portal:interdimensional) ==> #x)",
                "[open(John,(interdimensional-->portal)), #1, (John,(interdimensional-->portal)), (interdimensional-->portal), open, John]");
    }

    static void testTemplates(String term, String expect) throws Narsese.NarseseException {
        NAR n = NARS.tmp(1);
        n.believe(term + ".");
        Activate a = new Activate(n.conceptualize($(term)), 0.5f);
        Termed[] t = a.templates(a.id, n);
        assertEquals(expect, Arrays.toString(t));
    }
}