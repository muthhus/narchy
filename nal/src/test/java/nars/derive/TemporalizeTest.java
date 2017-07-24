package nars.derive;

import nars.$;
import nars.Narsese;
import org.chocosolver.solver.variables.Variable;
import org.junit.Test;

import static org.junit.Assert.*;

public class TemporalizeTest {

    @Test
    public void testEventize() throws Narsese.NarseseException {
        Temporalize t = new Temporalize();
        t.eventize($.$("(a &&+5 b)"));
        for (Variable v : t.getVars()) {
            System.out.println(v);
        }
    }
}