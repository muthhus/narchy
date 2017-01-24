package nars.term.transform;

import com.google.common.collect.Lists;
import jcog.data.random.XorShift128PlusRandom;
import nars.Narsese;
import nars.Op;
import nars.Param;
import nars.derive.meta.match.Ellipsis;
import nars.term.Term;
import nars.term.mutate.Choose1;
import nars.term.mutate.Choose2;
import nars.term.mutate.CommutivePermutations;
import nars.term.mutate.Termutator;
import nars.term.subst.Unify;
import nars.term.var.Variable;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import static nars.$.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by me on 12/22/15.
 */
public class TermutatorTest {

    final Unify f = new Unify(terms, Op.VAR_PATTERN, new XorShift128PlusRandom(1), Param.UnificationStackMax, Param.UnificationTermutesMax) {
        @Override
        public boolean onMatch() {
            return true;
        }
    };

    @Test
    public void testChoose1_2() {

        assertTermutatorProducesUniqueResults(
                new Choose1(e1, p2,
                        p("a", "b").toSet()), 2);

    }

    @Test
    public void testChoose1_3() {

        assertTermutatorProducesUniqueResults(
                new Choose1(e1, p2,
                        p("a", "b", "c").toSet()), 3);
    }

    @Test
    public void testChoose1_4() {

        assertTermutatorProducesUniqueResults(
                new Choose1(e1, p2,
                        p("a", "b", "c", "d").toSet()), 4);
    }


    static final Term e0;
    static {
        //HACK
        Term ee0;
        try {
            ee0 = $("%A..+");
        } catch (Narsese.NarseseException e) {
            ee0 = null;
            e.printStackTrace();
            System.exit(1);
        }
        e0 = ee0;
    }
    static final @NotNull Ellipsis e1 = Ellipsis.EllipsisPrototype.make(1,1);


    static final Variable p2= v(Op.VAR_PATTERN, 2);
    static final Collection<Term> p2p3 = Lists.newArrayList( p2, v(Op.VAR_PATTERN, 3) );

    @Test public void testChoose2_2() {



        assertTermutatorProducesUniqueResults(
                new Choose2(f, e1,
                        p2p3,
                        p("a", "b").toSet()), 2);
    }

    @Test public void testChoose2_3() {

        assertTermutatorProducesUniqueResults(
                new Choose2(f, e1, p2p3,
                        p("a", "b", "c").toSet()), 6);
    }
    @Test public void testChoose2_4() {

        Set<String> series = new HashSet();
        for (int i = 0; i < 4; i++) {
            series.add(
                    assertTermutatorProducesUniqueResults(
                            new Choose2(f, e1,
                                    p2p3,
                                    p("a", "b", "c", "d").toSet()), 12)
            );
        }

        assertTrue(series.size() > 1); //shuffling works
    }



    @Test public void testComm2() throws Narsese.NarseseException {
        assertTermutatorProducesUniqueResults(
                new CommutivePermutations(f, $("{%A,%B}"),
                        $("{x,y}")), 2);
    }
    @Test public void testComm3() throws Narsese.NarseseException {
        assertTermutatorProducesUniqueResults(
                new CommutivePermutations(f, $("{%A,%B,%C}"),
                        $("{x,y,z}")), 6);
    }
    @Test public void testComm4() throws Narsese.NarseseException {
        assertTermutatorProducesUniqueResults(
                new CommutivePermutations(f, $("{%A,%B,%C,%D}"),
                        $("{w,x,y,z}")), 24);
    }

    String assertTermutatorProducesUniqueResults(@NotNull Termutator t, int num) {

        assertEquals(num, t.getEstimatedPermutations());

        Set<String> s = new LinkedHashSet(); //record the order
        final int[] actual = {0};
        //int blocked = 0;
        final int[] duplicates = {0};

        t.run(f, new Termutator[] { t, new Termutator("evaluate") {

            @Override
            public boolean run(@NotNull Unify f, Termutator[] chain, int current) {
                if (s.add( f.xy.toString() )) {
                    actual[0]++;
                } else {
                    duplicates[0]++;
                }

                return true;
            }

            @Override
            public int getEstimatedPermutations() {
                return 0;
            }
        }}, 0);


        String res = s.toString();
        System.out.println(res);

        assertEquals(num, s.size());
        assertEquals(num, actual[0]);
        assertEquals(0, duplicates[0]);

        return res;
    }

}