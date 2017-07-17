package nars.term.atom;

import nars.NAR;
import nars.Narsese;
import nars.NARS;
import org.junit.Ignore;
import org.junit.Test;

public class IntAtomTest {

    @Ignore
    @Test
    public void testVariableIntroduction() throws Narsese.NarseseException {
        NAR n = new NARS().get();
        n.log();
        n.input(" ((3,x) ==>+1 (4,y)).");
        // ((3,x) ==>+1 (add(3,1),y)).

        n.run(10);
    }

}