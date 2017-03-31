package nars.op;

import nars.nar.Default;
import nars.time.Tense;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by me on 3/30/17.
 */
public class DepIndepVarIntroductionTest {

    @Test
    public void testIntroInProduct() {
        Default d = new Default();
        d.log();
        d.believe("(fz-->(d00,c00,b01,a00))", Tense.Present, 1f, 0.9f);
        d.believe("(fz-->(d01,c00,b01,a00))", Tense.Present, 1f, 0.9f);
        d.run(100);
        //should introduce multiple variables
    }
}