package nars.term;

import nars.Op;
import nars.term.var.AbstractVariable;
import nars.term.var.CommonVariable;
import nars.term.var.UnnormalizedVariable;
import nars.term.var.Variable;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Created by me on 9/9/15.
 */
public class CommonVariableTest {



    static final UnnormalizedVariable p1 = new UnnormalizedVariable(Op.VAR_PATTERN, "%1");
    static final UnnormalizedVariable p2 = new UnnormalizedVariable(Op.VAR_PATTERN, "%2");
    static final UnnormalizedVariable p3 = new UnnormalizedVariable(Op.VAR_PATTERN, "%3");
    static final UnnormalizedVariable p12 = new UnnormalizedVariable(Op.VAR_PATTERN, "%12");

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


    public static @NotNull Variable common(@NotNull UnnormalizedVariable v1, @NotNull UnnormalizedVariable v2) {
        return CommonVariable.common(
                (AbstractVariable)v1.normalize(1),
                (AbstractVariable)v2.normalize(2)
        );
    }

    @Disabled
    @Test
    public void CommonVariableOfCommonVariable() {
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
        assertTrue(c12 != c12_reverse);

        Variable c123 = CommonVariable.common((AbstractVariable)c12, (AbstractVariable)p3.normalize(3));
        assertEquals("%770%3 class nars.term.var.UnnormalizedVariable", (c123 + " " + c123.getClass()));

        //duplicate: already included
        Variable c122 = CommonVariable.common((AbstractVariable)c12, (AbstractVariable)p2.normalize(2));
        assertEquals("%770 class nars.term.var.CommonVariable", (c122 + " " + c122.getClass()));


    }
}