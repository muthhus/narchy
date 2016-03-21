package nars.term;

import nars.Global;
import nars.NAR;
import nars.concept.AtomConcept;
import nars.concept.DefaultConceptBuilder;
import nars.nar.Default;
import nars.nar.Terminal;
import nars.task.Task;
import nars.term.atom.Atomic;
import nars.term.container.TermContainer;
import nars.term.container.TermVector;
import nars.term.index.MapIndex2;
import nars.time.FrameClock;
import nars.util.data.random.XorShift128PlusRandom;
import org.jetbrains.annotations.NotNull;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

import static nars.$.$;
import static nars.$.operator;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;


public class TermIndexTest {

    @Test
    public void testTaskTermSharing1() {

        NAR t = new Default();

        String term = "<a --> b>.";

        Task t1 = t.inputTask(term);
        Task t2 = t.inputTask(term);
        t.step();

        testShared(t.concept(t1), t.concept(t2));

    }

    void testIndex(TermIndex i) {
        testTermSharing(i);
        //testSequenceNotShared(i);
    }

//    @Test public void testTermSharing1() {
//        testIndex(new MapIndex(new HashMap(), new HashMap()));
//    }
//    @Test public void testTermSharing2() {
//        testIndex(new MapIndex(new UnifriedMap(), new UnifriedMap()));
//    }
//    @Test public void testTermSharing3() {
//        testIndex(new MapIndex(new SoftValueHashMap(), new SoftValueHashMap()));
//    }


    @Test public void testTermSharing5a() {
        testIndex(new MapIndex2(new HashMap<>(),
                new DefaultConceptBuilder(
                    new XorShift128PlusRandom(2), 32, 32
                )));
    }
    @Test public void testTermSharing5b() {
        testIndex(
                new MapIndex2(Global.newHashMap(),
                    new DefaultConceptBuilder(
                        new XorShift128PlusRandom(2), 32, 32
                    ))
        );
        //testIndex(new MapIndex2(newHashMap(), conceptBuilder));
    }

//    @Test public void testTermSharing4() {
//        testIndex(new MapIndex(new WeakHashMap(), new WeakHashMap()));
//    }
//    @Test public void testTermSharingGuava() {
//        testIndex(new GuavaIndex());
//    }


    void testTermSharing(TermIndex tt) {

        testShared(tt, "<<x-->w> --> <y-->z>>");
        testShared(tt, "<a --> b>");
        testShared(tt, "(c, d)");
        testShared(tt, "<e <=> f>");
        testShared(tt, "g");

        //tt.print(System.out);
        //System.out.println();

    }


//    public void testSequenceNotShared(TermIndex i) {
//        NAR n = new Terminal(i);
//
//        Termed a = n.term("(&/, 1, /2)");
//        assertNull(n.memory.index.getTermIfPresent(a));
//
//        Termed b = n.term("(&/, 1, /3)");
//        assertNull(n.memory.index.getTermIfPresent(b));
//
//        assertFalse(((Sequence)a).equals2((Sequence) b.term()));
//
//    }

    private void testNotShared(NAR n, String s) {
        Termed t1 = n.term(s); //create by parsing
        Termed t2 = n.term(s); //create by parsing again
        assertEquals(t1, t2);
        assertTrue(t1 != t2);
    }

    private void testShared(TermIndex i, String s) {

        int t0 = i.size();
        int s0 = i.subtermsCount();

        Term a = i.the(s); //create by parsing

        int t1 = i.size();
        int s1 = i.subtermsCount();

        //some terms and subterms were added
        if (a instanceof Compound) {
            assertTrue(t0 < t1);
            assertTrue(s1 + " subterms indexed for " + t0 + " terms", s0 < s1);
        }

        Term a2 = i.the(s); //create by parsing again
        testShared(a, a2);

        assertEquals(i.size(), t1 /* unchanged */);
        assertEquals(i.subtermsCount(), s1 /* unchanged */);

        //i.print(System.out); System.out.println();

        //create by composition
        Compound b = i.the('(' + s + ')');
        testShared(a.term(), b.term(0));

        assertEquals(i.size(), t1 + 1 /* one more for the product container */);

        //i.print(System.out); System.out.println();

        assertEquals(i.subtermsCount(), s1 + 1 /* unchanged */);

        //create by transformation (substitution)
        //testShared(a, n.term(..).substMap(..
    }

    static void testShared(Termed t1, Termed t2) {
        //t.memory.terms.forEachTerm(System.out::println);

        assertEquals(t1.term(), t2.term());
        if (t1 != t2)
            System.err.println("share failed: " + t1 + ' ' + t1.getClass() + ' ' + t2 + ' ' + t2.getClass());

        assertTrue(t1 == t2);
        assertEquals(t1, t2);

        if (t1 instanceof Compound) {
            //test all subterms are shared
            for (int i = 0; i < t1.term().size(); i++)
                testShared(((Compound) t1).term(i), ((Compound) t2).term(i));
        }
    }

    @Ignore
    @Test
    public void testRuleTermsAddedToMemoryTermIndex() {
        NAR d = new Default(100, 1, 1, 1);
        Set<Term> t = new TreeSet();
        d.index.forEach(x -> t.add(x.term()));

        assertTrue(t.size() > 100); //approximate

        //t.forEach(System.out::println);

    }



    @Test public void testSubtermIntern() {
        MapIndex2 i = (MapIndex2)(new Default().index());

        Term at = $("a");
        TermContainer a = TermVector.the(at, $("b"), $("cd"));
        TermContainer b = i.theSubterms(a);
        assertEquals(a, b);

        i.print(System.out);

        System.out.println(a.term(0));
        System.out.println(a.term(0));
        System.out.println(i.data);
        a.forEach(bb->System.out.println(bb + " " + bb.getClass()));

        Term B = b.term(0);
        assertTrue(at!= B);
        assertTrue(B instanceof AtomConcept);
        assertTrue(B.term() == B);
        assertEquals(at.toString(), B.toString());


    }

    @Test public void testCommonPrefix1() {
        testCommonPrefix(true);
    }
    @Test public void testCommonPrefix2() {
        testCommonPrefix(false);
    }

    public static void testCommonPrefix(boolean direction) {
        MapIndex2 i = (MapIndex2)(new Default().index());
        Atomic sui = operator("substituteIfUnifies");
        Atomic su = operator("substitute");

        if (direction) {
            i.the(sui);
            i.the(su);
        } else { //reverse
            i.the(su);
            i.the(sui);
        }


        System.out.println(i);
        i.print(System.out);

        //assertEquals(20 + 2, i.atoms.size());
        assertEquals(sui, i.get(sui));
        assertEquals(su, i.get(su));
        assertNotEquals(sui, i.get(su));

    }
}