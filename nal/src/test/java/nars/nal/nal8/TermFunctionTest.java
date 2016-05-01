package nars.nal.nal8;

import nars.nar.Default;
import org.junit.Test;

/**
 * Created by me on 5/1/16.
 */
public class TermFunctionTest {

    @Test
    public void testAdd1() {
        Default d = new Default();
        d.log();
        d.input("add(1,2,#x)!");
        d.run(16);
        d.input("add(3,4,#x)!");
        d.run(16);
    }

    @Test
    public void testAdd1Temporal() {
        Default d = new Default();
        d.log();
        d.input("add(1,2,#x)! :|:");
        d.run(16);
        d.input("add(3,4,#x)! :|:");
        d.run(16);
    }
}
