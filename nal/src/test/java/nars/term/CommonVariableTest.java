package nars.term;

import nars.Op;
import nars.nar.Terminal;
import nars.term.var.CommonVariable;
import nars.term.var.GenericVariable;
import nars.term.var.Variable;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;


/**
 * Created by me on 9/9/15.
 */
public class CommonVariableTest {



    static final GenericVariable p1 = new GenericVariable(Op.VAR_PATTERN, "1");
    static final GenericVariable p2 = new GenericVariable(Op.VAR_PATTERN, "2");
    static final GenericVariable p3 = new GenericVariable(Op.VAR_PATTERN, "3");
    static final GenericVariable p12 = new GenericVariable(Op.VAR_PATTERN, "12");

    @Test
    public void commonVariableTest1() {
        assertEquals("%1%2",
                common(
                        p1,
                        p2).toString(),

        //reverse order
                common(
                        p2,
                        p1).toString());
    }

    @Test
    public void commonVariableTest2() {
        //different lengths

        assertEquals("%12%2",
                common(
                        p12,
                        p2).toString(),
        //different lengths
                common(
                        p2,
                        p12).toString());

    }


    public static @NotNull Variable common(@NotNull GenericVariable v1, @NotNull GenericVariable v2) {
        return CommonVariable.common(v1.normalize(1), v2.normalize(2));
    }

    @Test
    public void commonVariableInstancing() {
        //different lengths

        Variable c12 = common(
                p1,
                p2);
        Variable c12_reverse = common(
                p2,
                p1);

        assertEquals(c12, c12_reverse);
        assertEquals(0, c12.compareTo(c12_reverse));
        assertEquals(0, c12_reverse.compareTo(c12));
        Assert.assertTrue(c12 != c12_reverse);

        Variable c123 = CommonVariable.common(c12, p3.normalize(3));
        assertEquals("%770%3 class nars.term.var.GenericVariable", (c123 + " " + c123.getClass()));

        //duplicate: already included
        Variable c122 = CommonVariable.common(c12, p2.normalize(2));
        assertEquals("%770 class nars.term.var.CommonVariable", (c122 + " " + c122.getClass()));


    }
}