package nars.term;

import nars.nar.Terminal;
import nars.term.variable.CommonVariable;
import nars.term.variable.GenericNormalizedVariable;
import nars.term.variable.GenericVariable;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;


/**
 * Created by me on 9/9/15.
 */
public class CommonVariableTest {

    static final Terminal p = new Terminal();

    static final GenericVariable p1 = (GenericVariable) p.term("%1");
    static final GenericVariable p2 = (GenericVariable) p.term("%2");
    static final GenericVariable p12 = (GenericVariable) p.term("%12");

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
                (GenericVariable)p.term("%1"),
                (GenericVariable)p.term("%2"));
        GenericNormalizedVariable cb = make(
                (GenericVariable)p.term("%2"),
                (GenericVariable)p.term("%1"));

        assertEquals(ca, cb);
        Assert.assertTrue(ca == cb);
    }
}