package nars.control;

import nars.Narsese;
import nars.concept.Concept;
import nars.nar.Default;
import org.junit.Test;

import static org.junit.Assert.*;

public class ConceptFireTest {

    @Test
    public void testConceptFireLinkSelection() throws Narsese.NarseseException {
        Default d = new Default();
        d.believe("a:b");
        d.run(1);
        Concept c = d.concept("a:b");
        c.print();

        ConceptFire cf = new ConceptFire(c, 1f);

    }

}