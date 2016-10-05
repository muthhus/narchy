package nars.term.transform;

import nars.$;
import nars.Op;
import nars.Param;
import nars.term.Term;
import nars.term.subst.Unify;
import nars.term.subst.Subst;
import nars.util.data.random.XORShiftRandom;
import nars.util.data.random.XorShift128PlusRandom;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


@Deprecated
public class UnifyTest {

    @Test
    public void testFindSubst1() {
        testFindSubst($.$("<a-->b>"), $.$("<?C-->b>"), true);
        testFindSubst($.$("(--,(a))"), $.$("<?C-->(b)>"), false);
    }


    @NotNull
    public Subst testFindSubst(@NotNull Term a, @NotNull Term b, boolean matches) {

        AtomicBoolean matched = new AtomicBoolean(false);

        Unify f = new Unify($.terms, Op.VAR_QUERY, new XorShift128PlusRandom(1), Param.UnificationStackMax, Param.UnificationTermutesMax) {

            @Override
            public boolean onMatch() {

                assertTrue(matches);

                matched.set(true);

                //identifier: punctuation, mapA, mapB
                assertEquals("{?1=a}", xy.toString());

                //output
                assertEquals(
                        "(a-->b) (?1-->b) -?>",
                        a + " " + b + " -?>"  /*+ " remaining power"*/);

                return true;
            }
        };

        f.unifyAll(b, a);

        assertEquals(matched.get(), matches);

        return f;
    }
}