package nars.term;


import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.Param;
import nars.test.analyze.EventCount;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NALLevelTest {



    @Test
    public void testLevel1vs8() throws Narsese.NarseseException {
        Param.DEBUG = true;

        NAR nDefault = NARS.shell();
        assertEquals(8, nDefault.nal());

        NAR n1 = NARS.shell();
        n1.nal(1);
        EventCount n1Count = new EventCount(n1);
        assertEquals(1, n1.nal());

        NAR n8 = NARS.shell();
        n8.nal(8);
        EventCount n8Count = new EventCount(n8);

        String productSentence = "<(a,b) --> c>.\n<c <-> a>?\n";

        assertEquals(0, n1.time());


        n8.input(productSentence);
        n8.run(5);


        assertEquals("NAL1 will NOT process sentence containing a Product", 0, n1Count.numTaskProcesses());
        assertTrue("NAL8 will process sentence containing a Product", n8Count.numTaskProcesses() >= 1);




    }
}
