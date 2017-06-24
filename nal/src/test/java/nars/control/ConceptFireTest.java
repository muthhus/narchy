package nars.control;

import nars.NAR;
import nars.Narsese;
import nars.concept.Concept;
import nars.nar.NARBuilder;
import org.junit.Test;

public class ConceptFireTest {

    @Test
    public void testConceptFireLinkSelection() throws Narsese.NarseseException {
        NAR d = new NARBuilder().get();
        d.believe("a:b");
        d.run(1);
        Concept c = d.concept("a:b");
        c.print();

        ConceptFire cf = new ConceptFire(c, 1f);

    }

}