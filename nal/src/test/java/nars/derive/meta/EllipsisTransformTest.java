package nars.derive.meta;

import nars.$;
import nars.Narsese;
import nars.Op;
import nars.derive.meta.match.Ellipsis;
import nars.derive.meta.match.EllipsisTransform;
import nars.derive.rule.PremiseRule;
import nars.index.term.PatternTermIndex;
import nars.index.term.TermIndex;
import nars.term.Compound;
import nars.term.Term;
import nars.term.var.AbstractVariable;
import org.junit.Test;

import static nars.Op.Imdex;
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


    @Test public void testEllipsisTransform() throws Narsese.NarseseException {
        String s = "%A..%B=_..+";
        Ellipsis.EllipsisTransformPrototype unnormalized = $.$(s);
        assertEquals("%A..%B=_..+", unnormalized.toString());

        assertNotNull(unnormalized);
        assertEquals($.$("%B"), unnormalized.from);
        assertEquals(Imdex, unnormalized.to);

        TermIndex i = new PatternTermIndex();

        Term u = i.transform(
                $.p(unnormalized), new PremiseRule.PremiseRuleVariableNormalization());
        Object tt = ((Compound)u).sub(0);
        assertEquals(Ellipsis.EllipsisTransformPrototype.class, tt.getClass());
        assertEquals("(%1747846151..%2=_..+)", u.toString());

        Ellipsis.EllipsisTransformPrototype ttt = (Ellipsis.EllipsisTransformPrototype) tt;
        assertEquals($.$("%2").toString(), ttt.from.toString());
        assertEquals(Imdex, ttt.to);
    }
}