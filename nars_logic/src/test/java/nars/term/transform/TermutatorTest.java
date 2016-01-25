package nars.term.transform;

import nars.Op;
import nars.term.Term;
import nars.term.transform.subst.FindSubst;
import nars.term.transform.subst.choice.Choose1;
import nars.term.transform.subst.choice.Choose2;
import nars.term.transform.subst.choice.CommutivePermutations;
import nars.term.transform.subst.choice.Termutator;
import nars.util.data.random.XorShift128PlusRandom;
import org.junit.Test;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import static nars.$.$;
import static nars.$.p;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sun.management.snmp.jvminstr.JvmThreadInstanceEntryImpl.ThreadStateMap.Byte0.blocked;

/**
 * Created by me on 12/22/15.
 */
public class TermutatorTest {

    final FindSubst f = new FindSubst(Op.VAR_PATTERN, new XorShift128PlusRandom(1)) {
        @Override
        public boolean onMatch() {
            return true;
        }
    };

    @Test
    public void testChoose1_2() {

        assertTermutatorProducesUniqueResults(
                new Choose1($("%A..+"), $("%X"),
                        p("a", "b").toSet()), 2);

    }
    @Test
    public void testChoose1_3() {

        assertTermutatorProducesUniqueResults(
                new Choose1($("%A..+"), $("%X"),
                        p("a", "b", "c").toSet()), 3);
    }
    @Test
    public void testChoose1_4() {

        assertTermutatorProducesUniqueResults(
                new Choose1($("%A..+"), $("%X"),
                        p("a", "b", "c", "d").toSet()), 4);
    }


    @Test public void testChoose2_2() {

        assertTermutatorProducesUniqueResults(
                new Choose2(f, $("%A..+"),
                        new Term[] { $("%X"), $("%Y") },
                        p("a", "b").toSet()), 2);
    }

    @Test public void testChoose2_3() {

        assertTermutatorProducesUniqueResults(
                new Choose2(f, $("%A..+"),
                        new Term[] { $("%X"), $("%Y") },
                        p("a", "b", "c").toSet()), 6);
    }
    @Test public void testChoose2_4() {

        Set<String> series = new HashSet();
        for (int i = 0; i < 4; i++) {
            series.add(
                assertTermutatorProducesUniqueResults(
                    new Choose2(f, $("%A..+"),
                            new Term[]{$("%X"), $("%Y")},
                            p("a", "b", "c", "d").toSet()), 12)
            );
        }

        assertTrue(series.size() > 1); //shuffling works
    }



    @Test public void testComm2() {
        assertTermutatorProducesUniqueResults(
                new CommutivePermutations($("{%A,%B}"),
                        $("{x,y}")), 2);
    }
    @Test public void testComm3() {
        assertTermutatorProducesUniqueResults(
                new CommutivePermutations($("{%A,%B,%C}"),
                        $("{x,y,z}")), 6);
    }
    @Test public void testComm4() {
        assertTermutatorProducesUniqueResults(
                new CommutivePermutations($("{%A,%B,%C,%D}"),
                        $("{w,x,y,z}")), 24);
    }

    String assertTermutatorProducesUniqueResults(Termutator t, int num) {

        assertEquals(num, t.getEstimatedPermutations());

        Set<String> s = new LinkedHashSet(); //record the order
        final int[] actual = {0};
        //int blocked = 0;
        final int[] duplicates = {0};
        int i = 0;


        t.run(f, new Termutator[] { t, new Termutator("evaluate") {

            @Override
            public void run(FindSubst f, Termutator[] chain, int current) {
                if (s.add( f.xy.toString() )) {
                    actual[0]++;
                } else {
                    duplicates[0]++;
                }

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
        assertEquals(0, blocked);
        assertEquals(0, duplicates[0]);

        return res;
    }

}