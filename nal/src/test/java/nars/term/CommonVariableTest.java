package nars.term;

import nars.Op;
import nars.nar.Terminal;
import nars.term.variable.CommonVariable;
import nars.term.variable.GenericNormalizedVariable;
import nars.term.variable.GenericVariable;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;


/**
 * Created by me on 9/9/15.
 */
public class CommonVariableTest {

    static final Terminal p = new Terminal(32);

    static final GenericVariable p1 = new GenericVariable(Op.VAR_PATTERN, "1");
    static final GenericVariable p2 = new GenericVariable(Op.VAR_PATTERN, "2");
    static final GenericVariable p12 = new GenericVariable(Op.VAR_PATTERN, "12");

    @Test
    public void commonVariableTest1() {
        assertEquals("%1%2",
                make(
                        p1,
                        p2).toString(),

        //reverse order
                make(
                        p2,
                        p1).toString());
    }

    @Test
    public void commonVariableTest2() {
        //different lengths

        assertEquals("%12%2",
                make(
                        p12,
                        p2).toString(),
        //different lengths
                make(
                        p2,
                        p12).toString());

    }


    public static @NotNull GenericNormalizedVariable make(@NotNull GenericVariable v1, @NotNull GenericVariable v2) {
        return CommonVariable.make(v1.normalize(1), v2.normalize(2));
    }

    @Test
    public void commonVariableInstancing() {
        //different lengths

        GenericNormalizedVariable ca = make(
                p1,
                p2);
        GenericNormalizedVariable cb = make(
                p2,
                p1);

        assertEquals(ca, cb);
        assertEquals(0, ca.compareTo(cb));
        assertEquals(0, cb.compareTo(ca));
        Assert.assertTrue(ca != cb);
    }
}