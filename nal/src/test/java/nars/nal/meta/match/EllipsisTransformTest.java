package nars.nal.meta.match;

import nars.$;
import nars.Op;
import nars.term.variable.AbstractVariable;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by me on 3/23/16.
 */
public class EllipsisTransformTest {

    @Test
    public void testInequality() {
        AbstractVariable v1 = $.v(Op.VAR_PATTERN, 1);
        EllipsisTransform a = new EllipsisTransform(v1, Op.Imdex, $.v(Op.VAR_PATTERN, 2));
        EllipsisTransform b = new EllipsisTransform(v1, $.v(Op.VAR_PATTERN, 2), Op.Imdex);
        assertNotEquals(a.toString(), b.toString());
        assertNotEquals(a, b);
        assertNotEquals(0, a.compareTo(b));
        assertEquals(b.compareTo(a), -a.compareTo(b));
        assertNotEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, v1);

        assertEquals(a, a);
        assertEquals(0, a.compareTo(a));
    }
}