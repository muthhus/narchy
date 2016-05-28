package nars.term.transform;

import nars.$;
import nars.Op;
import nars.term.Term;
import nars.term.Terms;
import nars.term.subst.FindSubst;
import nars.term.subst.Subst;
import nars.util.data.random.XORShiftRandom;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


@Deprecated
public class FindSubstTest {

    @Test
    public void testFindSubst1() {
        testFindSubst($.$("<a-->b>"), $.$("<?C-->b>"), true);
        testFindSubst($.$("(--,a)"), $.$("<?C-->b>"), false);
    }


    @NotNull
    public Subst testFindSubst(@NotNull Term a, @NotNull Term b, boolean matches) {

        AtomicBoolean matched = new AtomicBoolean(false);

        FindSubst f = new FindSubst(Terms.terms, Op.VAR_QUERY, new XORShiftRandom()) {

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

        f.matchAll(b, a);

        assertEquals(matched.get(), matches);

        return f;
    }
}