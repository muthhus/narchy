package nars.term;

import nars.$;
import nars.term.var.AbstractVariable;
import nars.term.var.CommonVariable;
import nars.term.var.Variable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Created by me on 9/9/15.
 */
public class CommonVariableTest {


    static final Variable p1 = $.varDep(1);
    static final Variable p2 = $.varDep(2);
    static final Variable p3 = $.varDep(3);


    @Test
    public void commonVariableTest1() {
        //same forward and reverse
        Variable p1p2 = CommonVariable.common(p1, p2);
        Variable p2p1 = CommonVariable.common(p2, p1);
        Variable p1p3 = CommonVariable.common(p1, p3);
        Variable p1p1 = CommonVariable.common(p1, p1);

        System.out.println(p1p2);
        System.out.println(p2p1);
        System.out.println(p1p3);
        System.out.println(p1p1);

        assertEquals(p1p2, p2p1);

        assertTrue(!p1p2.equals(p1p3));
        assertNotEquals(p1p2, p1p3);

        assertNotEquals(p1, p1p1);
    }


    @Disabled
    @Test
    public void CommonVariableOfCommonVariable() {
        //different lengths

        Variable c12 = CommonVariable.common(p1, p2);
        Variable c12_reverse = CommonVariable.common(p2, p1);

        assertEquals(c12, c12_reverse);
        assertEquals(0, c12.compareTo(c12_reverse));
        assertEquals(0, c12_reverse.compareTo(c12));
        assertNotSame(c12, c12_reverse);

        Variable c123 = CommonVariable.common((AbstractVariable) c12, (AbstractVariable) p3.normalize(3));
        assertEquals("%770%3 class nars.term.var.UnnormalizedVariable", (c123 + " " + c123.getClass()));

        //duplicate: already included
        Variable c122 = CommonVariable.common((AbstractVariable) c12, (AbstractVariable) p2.normalize(2));
        assertEquals("%770 class nars.term.var.CommonVariable", (c122 + " " + c122.getClass()));


    }
}