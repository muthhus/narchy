package nars.term;


import nars.NAR;
import nars.Param;
import nars.nar.Default;
import nars.util.analyze.EventCount;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NALLevelTest {



    @Test
    public void testLevel1vs8() {
        Param.DEBUG = true;

        NAR nDefault = new Default();
        assertEquals(Param.DEFAULT_NAL_LEVEL, nDefault.nal());

        NAR n1 = new Default();
        n1.nal(1);
        EventCount n1Count = new EventCount(n1);
        assertEquals(1, n1.nal());

        NAR n8 = new Default();
        n8.nal(8);
        EventCount n8Count = new EventCount(n8);

        String productSentence = "<(a,b) --> c>.\n<c <-> a>?\n";

        try {
            n1.input(productSentence);
            n1.run(5);
            assertTrue(false); //should not reach here
        } catch (NAR.InvalidTaskException e) {
            //this is normal
        }

        assertEquals(0, n1.time());


        n8.input(productSentence);
        n8.run(5);


        assertEquals("NAL1 will NOT process sentence containing a Product", 0, n1Count.numTaskProcesses());
        assertTrue("NAL8 will process sentence containing a Product", n8Count.numTaskProcesses() >= 1);




    }
}
