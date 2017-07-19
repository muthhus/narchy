package nars.control;

import jcog.pri.PLink;
import jcog.pri.PriReference;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.Task;
import nars.concept.Concept;
import nars.control.premise.Derivation;
import nars.task.DerivedTask;
import nars.term.Term;
import nars.term.atom.Atom;
import org.eclipse.collections.api.tuple.primitive.ObjectIntPair;
import org.eclipse.collections.impl.bag.mutable.HashBag;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.util.function.Consumer;

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
            c.termlinks().put(new PLink<Term>($(n + ":a"), 0.2f * n));
        }

        HashBag<String> s = new HashBag();
        Activate cf = new Activate(c, 1f) {
            @Override
            protected int premise(Derivation d, @Nullable PriReference<Task> tasklink, @Nullable PriReference<Term> termlink, Consumer<DerivedTask> x, int ttlPerPremise) {
                //System.out.println("tasklink=" + tasklink + " termlink=" + termlink);
                if (termlink.get() instanceof Atom)
                    return 0 ; //ignore
                s.addOccurrences(/*tasklink.get() + " " +*/ termlink.get().toString(), 1);
                return super.premise(d, tasklink, termlink, x, ttlPerPremise);
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

}