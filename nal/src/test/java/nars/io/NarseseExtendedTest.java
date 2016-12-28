package nars.io;

import nars.$;
import nars.Narsese;
import nars.Op;
import nars.Task;
import nars.nar.Terminal;
import nars.term.Compound;
import nars.term.Term;
import nars.time.Tense;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static nars.io.NarseseTest.task;
import static nars.io.NarseseTest.term;
import static nars.time.Tense.*;
import static org.junit.Assert.*;

/**
 * Proposed syntax extensions, not implemented yet
 */
public class NarseseExtendedTest {


    @Test public void testRuleComonent0() {
        assertNotNull($.$("((P ==> S), (S ==> P))"));
        assertNotNull($.$("((P ==> S), (S ==> P), neqCom(S,P), time(dtCombine))"));
    }

    @Test public void testRuleComonent1() {
        String s = "((P ==> S), (S ==> P), neqCom(S,P), time(dtCombine), notImplEqui(P), notEqui(S), task(\"?\"))";
        assertNotNull($.$(s));
    }

    void eternal(Task t) {
        assertNotNull(t);
        tensed(t, true, Tense.Eternal);
    }
    void tensed(@NotNull Task t, @NotNull Tense w) {
        tensed(t, false, w);
    }
    void tensed(@NotNull Task t, boolean eternal, @NotNull Tense w) {
        assertEquals(eternal, isEternal(t.occurrence()));
        if (!eternal) {
            switch (w) {
                case Past: assertTrue(t.occurrence() < 0); break;
                case Future: assertTrue(t.occurrence() > 0); break;
                case Present: assertEquals(0, t.occurrence()); break;
                case Eternal: assertEquals(ETERNAL, t.occurrence()); break;
            }
        }
    }

    @Test public void testOriginalTruth() {
        //singular form, normal, to test that it still works
        eternal(task("(a & b). %1.0;0.9%"));

        //normal, to test that it still works
        tensed(task("(a & b). :|: %1.0;0.9%"), Present);
    }



    /** compact representation combining truth and tense */
    @Test public void testTruthTense() {


        tensed(task("(a & b). %1.0|0.7%"), Present);

        tensed(task("(a & b). %1.0/0.7%"), Future);
        tensed(task("(a & b). %1.0\\0.7%"), Past);
        eternal(task("(a & b). %1.0;0.7%"));

        /*tensed(task("(a & b). %1.0|"), Present);
        tensed(task("(a & b). %1.0/"), Future);
        tensed(task("(a & b). %1.0\\"), Past);*/
        eternal(task("(a & b). %1.0;0.9%"));


    }

    @Test public void testQuestionTenseOneCharacter() {
        //TODO one character tense for questions/quests since they dont have truth values
    }

    @Test
    public void testColonReverseInheritance() {
        Compound t = term("namespace:named");
        assertEquals(t.op(), Op.INH);
        assertEquals("named", t.term(0).toString());
        assertEquals("namespace", t.term(1).toString());



        Compound u = term("<a:b --> c:d>");
        assertEquals("((b-->a)-->(d-->c))", u.toString());

        Task ut = task("<a:b --> c:d>.");
        assertNotNull(ut);
        assertEquals(ut.term(), u);

    }
//    @Test
//    public void testBacktickReverseInstance() {
//        Inheritance t = term("namespace`named");
//        assertEquals(t.op(), Op.INHERITANCE);
//        assertEquals("namespace", t.getPredicate().toString());
//        assertEquals("{named}", t.getSubject().toString());
//    }


    static void eqTerm(@NotNull String shorter, String expected) {
        Narsese p = Narsese.the();

        Term a = p.term(shorter);
        assertNotNull(a);
        assertEquals(expected, a.toString());

        eqTask(shorter, expected);
    }

    static final Terminal t = new Terminal(8);

    static void eqTask(String x, String b) {
        Task a = t.task(x + '.');
        assertNotNull(a);
        assertEquals(b, a.term().toString());
    }

    @Test
    public void testNamespaceTerms2() {
        eqTerm("a:b", "(b-->a)");
        eqTerm("a : b", "(b-->a)");
    }

    @Test public void testNamespaceTermsNonAtomicSubject() {
        eqTerm("c:{a,b}", "({a,b}-->c)");
    }
    @Test public void testNamespaceTermsNonAtomicPredicate() {
        eqTerm("<a-->b>:c", "(c-->(a-->b))");
        eqTerm("{a,b}:c", "(c-->{a,b})");
        eqTerm("(a,b):c", "(c-->(a,b))");
    }

    @Test public void testNamespaceTermsChain() {

        eqTerm("d:{a,b}:c", "((c-->{a,b})-->d)");


        eqTerm("c:{a,b}", "({a,b}-->c)");
        eqTerm("a:b:c",   "((c-->b)-->a)");
        eqTerm("a :b :c",   "((c-->b)-->a)");
    }

    @Test
    public void testNamespaceLikeJSON() {
        Narsese p = Narsese.the();
        Term a = p.term("{ a:x, b:{x,y} }");
        assertNotNull(a);
        assertEquals(p.term("{<{x,y}-->b>, <x-->a>}"), a);

    }

    @Test
    public void testNegation2() throws Narsese.NarseseException {


        for (String s : new String[]{"--(negated-->a)!", "-- (negated-->a)!"}) {
            Task t = task(s);

            //System.out.println(t);
            /*
            (--,(negated))! %1.00;0.90% {?: 1}
            (--,(negated))! %1.00;0.90% {?: 2}
            */

            Term tt = t.term();
            assertEquals(Op.INH, tt.op());
            assertTrue("(negated-->a)".equals(tt.toString()));
            assertTrue(t.punc() == Op.GOAL);
        }
    }

    @Test
    public void testNegation3() {
        //without comma
        assertEquals( "(--,(x))", term("--(x)").toString() );
        assertEquals( "(--,(x))", term("-- (x)").toString() );

        assertEquals( "(--,(x&&y))", term("-- (x && y)").toString() );

        assertEquals( term("(goto(z) <=>+5 --(x))"),
                term("(goto(z) <=>+5 (--,(x)))")
        );

        assertEquals( term("(goto(z) <=>+5 --x:y)"),
                      term("(goto(z) <=>+5 (--,x:y))")
        );

        Compound nab = term("--(a & b)");
        assertTrue(nab.op() == Op.NEG);

        assertTrue(nab.term(0).op() == Op.SECTe);

//        try {
//            task("(-- negated illegal_extra_term)!");
//            assertTrue(false);
//        } catch (Exception e) {
//        }
    }

//    @Test public void testOptionalCommas() {
//        Term pABC1 = $.$("(a b c)");
//        Term pABC2 = $.$("(a,b,c)");
//        assertEquals(pABC1, pABC2);
//
//        Term pABC11 = $.$("(a      b c)");
//        assertEquals(pABC1, pABC11);
//    }


    @Test public void testTripleQuote() {
        assertEquals( "(\"\"\"triplequoted\"\"\")", term("(\"\"\"triplequoted\"\"\")").toString() );
        assertEquals( "(\"\"\"triple\"quoted\"\"\")", term("(\"\"\"triple\"quoted\"\"\")").toString() );
    }

}

