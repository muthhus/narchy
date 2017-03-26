/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package nars.nar;


import com.google.common.collect.Iterables;
import nars.Narsese;
import nars.concept.Concept;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * TODO test this for each different kind of attention/bag etc
 */
public class AttentionTest {
    
    @Test public void testSampleNextConcept() throws Narsese.NarseseException {
        
        int numConcepts = 32;
        Default n = new Default();
        for (int i = 0; i < numConcepts; i++)
            n.believe("<x" + i + " <-> x" + (i + 1) + '>');
        
        //n.runWhileInputting(100);
        n.run(16);
        
        int c = n.focus.active.size();
        assertTrue(c > 16);

        //n.trace(System.out);

        Set<Concept> uniqueconcepts = new HashSet();
        
        for (int i = 0; i < numConcepts; i++) {
            Concept s = n.focus.active.sample().get();
            uniqueconcepts.add(s);
        }

        assertTrue(uniqueconcepts.size() > 1);

        int c2 = Iterables.size(n.focus.active);
        assertEquals("does not affect # of concepts", c, c2);
    }
    
}
