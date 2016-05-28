package nars.concept;

import nars.$;
import nars.NAR;
import nars.nar.Default;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by me on 5/28/16.
 */
public class ProxyCompoundConceptTest {

    @Test
    public void testProxy1() {
        NAR n = new Default();

        String t = "(a --> b)";
        String p = "(c)";

        n.input(t + ".");
        CompoundConcept ab = (CompoundConcept) n.concept(t);

        ProxyCompoundConcept c = new ProxyCompoundConcept($.$(p), ab);
        n.on(c);

        @Nullable Concept P = n.concept(p);
        assertNotNull(P);
        assertEquals(2, P.termlinks().size());


    }

}