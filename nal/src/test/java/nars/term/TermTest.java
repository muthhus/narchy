/*
 * Copyright (C) 2014 me
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package nars.term;

import nars.*;
import nars.concept.Concept;
import nars.nar.Default;
import nars.nar.Terminal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.util.TreeSet;
import java.util.function.Supplier;

import static java.lang.Long.toBinaryString;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static nars.$.$;
import static nars.$.inh;
import static nars.Op.*;
import static nars.term.Term.False;
import static org.junit.Assert.*;

/**
 * @author me
 */
public class TermTest {
    static {
        Param.DEBUG = true;
    }


    @Nullable
    public static Term imageInt(Term... x) {
        return $.compound(IMGi, x);
    }

    @Nullable
    public static Term imageExt(Term... x) {
        return $.compound(IMGe, x);
    }


    protected void assertEquivalentTerm(@NotNull String term1String, @NotNull String term2String) {
        try {

            NAR n = new Terminal(16);

            Termed term1 = n.term(term1String);
            Termed term2 = n.term(term2String);

            assertNotEquals(term1String, term2String);

            assertEquivalentTerm(term1.term(), term2.term());

        } catch (Exception e) {
            assertFalse(e.toString(), true);
        }
    }

    public void assertEquivalentTerm(Term term1, Term term2) {
        assertTrue(term1 instanceof Compound);
        assertTrue(term2 instanceof Compound);

        assertEquals(term1, term2);
        assertEquals(term2, term1);
        assertEquals(term1.hashCode(), term2.hashCode());
        assertEquals(((Compound) term1).dt(), ((Compound) term2).dt());
        assertEquals(0, term1.compareTo(term2));
        assertEquals(0, term2.compareTo(term1));
        assertEquals(0, term1.compareTo(term1));
        assertEquals(0, term2.compareTo(term2));
    }

    @Test
    public void testCommutativeCompoundTerm() throws Exception {

        assertEquivalentTerm("(&&,a,b)", "(&&,b,a)");
        assertEquivalentTerm("(&&,(||,(b),(c)),(a))", "(&&,(a),(||,(b),(c)))");
        assertEquivalentTerm("(&&,(||,(c),(b)),(a))", "(&&,(a),(||,(b),(c)))");
        assertEquivalentTerm("(&&,(||,(c),(b)),(a))", "(&&,(a),(||,(c),(b)))");

        assertEquivalentTerm("(&,a,b)", "(&,b,a)");
        assertEquivalentTerm("{a,c,b}", "{b,a,c}");
        assertEquivalentTerm("{a,c,b}", "{b,c,a}");
        assertEquivalentTerm("[a,c,b]", "[b,a,c]");

        assertEquivalentTerm("<{Birdie}<->{Tweety}>", "<{Tweety}<->{Birdie}>");
        assertEquivalentTerm($("<{Birdie}<->{Tweety}>"),
                        $("<{Tweety}<->{Birdie}>"));
        assertEquivalentTerm(
                $.sim($("{Birdie}"),$("{Tweety}")),
                $.sim($("{Tweety}"),$("{Birdie}"))
        );

//        //test ordering after derivation
//        assertEquals("<{Birdie}<->{Tweety}>",
//            (((Compound)$("<{Birdie}<->{Tweety}>")).term(
//                new Term[] { $("{Tweety}"), $("{Birdie}") }).toString())
//        );
    }

    @Test
    public void testCommutativivity()  {
        assertFalse($.sete($.the("x")).isCommutative());
        assertTrue($.sete($.the("x"), $.the("y")).isCommutative());
    }

    @Test
    public void testTermSort() throws Exception {

        NAR n = new Terminal(16);

        Term a = n.term("a").term();
        Term b = n.term("b").term();
        Term c = n.term("c").term();

        assertEquals(3, Terms.toSortedSetArray(a, b, c).length);
        assertEquals(2, Terms.toSortedSetArray(a, b, b).length);
        assertEquals(1, Terms.toSortedSetArray(a, a).length);
        assertEquals(1, Terms.toSortedSetArray(a).length);
        assertEquals("correct natural ordering", a, Terms.toSortedSetArray(a, b)[0]);
    }

    @Test
    public void testConjunction1Term() throws Narsese.NarseseException {
        NAR n = new Terminal(16);

        assertEquals("a", n.term("(&&,a)").toString());
        assertEquals("x(a)", n.term("(&&,x(a))").toString());
        assertEquals("a", n.term("(&&,a, a)").toString());
        //assertEquals("a", n.term("(&&+0,a)").toString());
        //assertEquals("a", n.term("(&&+3,a)").toString());
    }

    @Test
    public void testConjunctionTreeSet() throws Narsese.NarseseException {
        NAR n = new Terminal(16);

        //these 2 representations are equal, after natural ordering
        String term1String = "<#1 --> (&,boy,(/,taller_than,{Tom},_))>";
        Term term1 = n.term(term1String).term();
        String term1Alternate = "<#1 --> (&,(/,taller_than,{Tom},_),boy)>";
        Term term1a = n.term(term1Alternate).term();


        // <#1 --> (|,boy,(/,taller_than,{Tom},_))>
        Term term2 = n.term("<#1 --> (|,boy,(/,taller_than,{Tom},_))>").term();

        assertEquals(term1a.toString(), term1.toString());
        assertTrue(term1.complexity() > 1);
        assertTrue(term1.complexity() == term2.complexity());

        assertTrue(term1.op() == INH);


        //System.out.println("t1: " + term1 + ", complexity=" + term1.getComplexity());
        //System.out.println("t2: " + term2 + ", complexity=" + term2.getComplexity());


        boolean t1e2 = term1.equals(term2);
        int t1c2 = term1.compareTo(term2);
        int t2c1 = term2.compareTo(term1);

        assertTrue(!t1e2);
        assertTrue("term1 and term2 inequal, so t1.compareTo(t2) should not = 0", t1c2 != 0);
        assertTrue("term1 and term2 inequal, so t2.compareTo(t1) should not = 0", t2c1 != 0);

        /*
        System.out.println("t1 equals t2 " + t1e2);
        System.out.println("t1 compareTo t2 " + t1c2);
        System.out.println("t2 compareTo t1 " + t2c1);
        */

        TreeSet<Term> set = new TreeSet<>();
        boolean added1 = set.add(term1);
        boolean added2 = set.add(term2);
        assertTrue("term 1 added to set", added1);
        assertTrue("term 2 added to set", added2);

        assertTrue(set.size() == 2);

    }

    @Test
    public void testUnconceptualizedTermInstancing() throws Narsese.NarseseException {
        NAR n = new Terminal(7);

        String term1String = "<a --> b>";
        Term term1 = n.term(term1String).term();
        Term term2 = n.term(term1String).term();

        assertTrue(term1.equals(term2));
        assertTrue(term1.hashCode() == term2.hashCode());

        Compound cterm1 = ((Compound) term1);
        Compound cterm2 = ((Compound) term2);

        //test subterms
        assertTrue(cterm1.term(0).equals(cterm2.term(0))); //'a'

    }



//    @Test
//    public void testEscaping() {
//        bidiEscape("c d", "x$# x", "\\\"sdkf sdfjk", "_ _");
//
////        NAR n = new Terminal().build();
////        n.addInput("<a --> \"b c\">.");
////        n.step(1);
////        n.finish(1);
////
////        Term t = new Term("\\\"b_c\\\"");
////        System.out.println(t);
////        System.out.println(n.memory.getConcepts());
////        System.out.println(n.memory.conceptProcessor.getConcepts());
////
////
////        assertTrue(n.memory.concept(new Term("a"))!=null);
////        assertTrue(n.memory.concept(t)!=null);
//
//    }

//    protected void bidiEscape(String... tests) {
//        for (String s : tests) {
//            s = '"' + s + '"';
//            String escaped = Texts.escape(s).toString();
//            String unescaped = Texts.unescape(escaped).toString();
//            //System.out.println(s + " " + escaped + " " + unescaped);
//            assertEquals(s, unescaped);
//        }
//    }

    @Test
    public void invalidTermIndep() {

        String t = "($1-->({place4}~$1))";

        NAR n = new Terminal(8);


        try {
            Task x = n.inputTask(t + '.');
            assertFalse(t + " is invalid compound term", true);
        } catch (Throwable tt) {
            assertTrue(true);
        }

        Term subj = null, pred = null;
        try {
            subj = $.varIndep(1);
            pred = n.term("(~,{place4},$1)").term();

            assertTrue(true);

        } catch (Throwable ex) {
            ex.printStackTrace();
            assertTrue(false);
        }




//        } catch (Throwable ex) {
//            assertTrue(ex.toString(), false);
//        }
    }


    @Test
    public void testParseOperationInFunctionalForm() {

        NAR n = new Terminal(8);

//        assertFalse(Op.isOperation(n.term("(a,b)")));
//        assertFalse(Op.isOperation(n.term("^wonder")));

        try {
            Term x = n.term("^wonder(a,b)").term();
            assertEquals(INH, x.op());
            assertTrue(Op.isOperation(x));
            assertEquals("^wonder(a,b)", x.toString());

        } catch (Narsese.NarseseException ex) {
            ex.printStackTrace();
            assertTrue(false);
        }


    }



//    public void nullCachedName(String term) {
//        NAR n = new Terminal();
//        n.input(term + ".");
//        n.run(1);
//        assertNull("term name string was internally generated although it need not have been", ((Compound) n.concept(term).getTerm()).nameCached());
//    }
//
//    @Test public void avoidsNameConstructionUnlessOutputInheritance() {
//        nullCachedName("<a --> b>");
//    }
//
//    @Test public void avoidsNameConstructionUnlessOutputNegationAtomic() {
//        nullCachedName("(--, a)");
//    }
//    @Test public void avoidsNameConstructionUnlessOutputNegationCompound() {
//        nullCachedName("(--, <a-->b> )");
//    }
//
//    @Test public void avoidsNameConstructionUnlessOutputSetInt1() {
//        nullCachedName("[x]");
//    }
//    @Test public void avoidsNameConstructionUnlessOutputSetExt1() {
//        nullCachedName("{x}");
//    }

    @Test public void testPatternVar() {
        assertTrue($("%x").op() == Op.VAR_PATTERN);
    }

    @Test
    public void termEqualityWithQueryVariables() {
        NAR n = new Terminal(8);
        String a = "<?1-->bird>";
        assertEquals(n.term(a), n.term(a));
        String b = "<bird-->?1>";
        assertEquals(n.term(b), n.term(b));
    }

    protected void testTermEqualityNonNormalizing(@NotNull String s) {
        testTermEquality(s, false);
    }
    protected void testTermEquality(@NotNull String s) {
        testTermEquality(s, true);
    }
    protected void testTermEquality(@NotNull String s, boolean normalize) {

        NAR n = new Terminal(16);

        Term a = n.term(s).term();

        NAR n2 = new Default();
        Term b = n.term(s).term();

        //assertTrue(a != b);

        if (a instanceof Compound) {
            assertEquals(((Compound)a).subterms(), ((Compound)b).subterms());
        }
        assertEquals(a.hashCode(), b.hashCode());
        assertEquals(a.toString(), b.toString());

        assertEquals(a, b);

        assertEquals(a.compareTo(a), a.compareTo(b));
        assertEquals(0, b.compareTo(a));

        if (normalize) {
            Concept n2a = n2.concept(a, true);
            assertNotNull(a + " should conceptualize", n2a);
            assertNotNull(b);
            assertEquals(n2a.toString(), b.toString());
            assertEquals(n2a.hashCode(), b.hashCode());
            assertEquals(n2a, b);
        }

    }

    @Test
    public void termEqualityOfVariables1() {
        testTermEqualityNonNormalizing("#1");
    }

    @Test
    public void termEqualityOfVariables2() {
        testTermEqualityNonNormalizing("$1");
    }

    @Test
    public void termEqualityOfVariables3() {
        testTermEqualityNonNormalizing("?1");
    }

    @Test
    public void termEqualityOfVariables4() {
        testTermEqualityNonNormalizing("%1");
    }


    @Test
    public void termEqualityWithVariables1() {
        testTermEqualityNonNormalizing("<#2 --> lock>");
    }

    @Test
    public void termEqualityWithVariables2() {
        testTermEquality("<<#2 --> lock> --> x>");
    }

    @Test
    public void termEqualityWithVariables3() {
        testTermEquality("(&&, x, <#2 --> lock>)");
        testTermEquality("(&&, x, <#1 --> lock>)");
    }

    @Test
    public void termEqualityWithVariables4() {
        testTermEquality("(&&, <<$1 --> key> ==> <#2 --> (/, open, $1, _)>>, <#2 --> lock>)");
    }

    @Test
    public void termEqualityWithMixedVariables() {

        NAR n = new Terminal(16);

        String s = "(&&, <<$1 --> key> ==> <#2 --> (/, open, $1, _)>>, <#2 --> lock>)";
        Termed a = n.term(s);

        NAR n2 = new Default();
        Termed b = n2.term(s);

        //assertTrue(a != b);
        assertEquals(a, b);

        //todo: method results ignored ?
        b.equals(n2.concept(a));

//        assertEquals("re-normalizing doesn't affect: " + n2.concept(a), b,
//                n2.concept(a));

    }


    @Test
    public void statementHash() {
        //this is a case where a faulty hash function produced a collision
        statementHash("i4", "i2");
        statementHash("{i4}", "{i2}");
        statementHash("<{i4} --> r>", "<{i2} --> r>");


        statementHash("<<{i4} --> r> ==> A(7)>", "<<{i2} --> r> ==> A(8)>");

        statementHash("<<{i4} --> r> ==> A(7)>", "<<{i2} --> r> ==> A(7)>");

    }

    @Test
    public void statementHash2() {
        statementHash("<<{i4} --> r> ==> A(7)>", "<<{i2} --> r> ==> A(9)>");
    }

    @Test
    public void statementHash3() {

        //this is a case where a faulty hash function produced a collision
        statementHash("<<{i0} --> r> ==> A(8)>", "<<{i1} --> r> ==> A(7)>");

        //this is a case where a faulty hash function produced a collision
        statementHash("<<{i10} --> r> ==> A(1)>", "<<{i11} --> r> ==> A(0)>");
    }

    public void statementHash(@NotNull String a, @NotNull String b) {


        Term ta = $(a);
        Term tb = $(b);

        assertNotEquals(ta, tb);
        assertNotEquals(ta + " vs. " + tb,
                ta.hashCode(), tb.hashCode());


    }

    @Test
    public void testTermComplexityMass() {
        NAR n = new Terminal(8);

        testTermComplexityMass(n, "x", 1, 1);

        testTermComplexityMass(n, "#x", 0, 1, 0, 1, 0);
        testTermComplexityMass(n, "$x", 0, 1, 1, 0, 0);
        testTermComplexityMass(n, "?x", 0, 1, 0, 0, 1);

        testTermComplexityMass(n, "<a --> b>", 3, 3);
        testTermComplexityMass(n, "<#a --> b>", 2, 3, 0, 1, 0);

        testTermComplexityMass(n, "<a --> (c & d)>", 5, 5);
        testTermComplexityMass(n, "<$a --> (c & #d)>", 3, 5, 1, 1, 0);
    }

    private void testTermComplexityMass(@NotNull NAR n, @NotNull String x, int complexity, int mass) {
        testTermComplexityMass(n, x, complexity, mass, 0, 0, 0);
    }

    private void testTermComplexityMass(@NotNull NAR n, @NotNull String x, int complexity, int mass, int varIndep, int varDep, int varQuery) {
        Term t = n.term(x).term();

        assertNotNull(t);
        assertEquals(complexity, t.complexity());
        assertEquals(mass, t.volume());

        assertEquals(varDep, t.varDep());
        assertEquals(varDep != 0, t.hasVarDep());

        assertEquals(varIndep, t.varIndep());
        assertEquals(varIndep != 0, t.hasVarIndep());

        assertEquals(varQuery, t.varQuery());
        assertEquals(varQuery != 0, t.hasVarQuery());

        assertEquals(varDep + varIndep + varQuery, t.vars());
        assertEquals((varDep + varIndep + varQuery) != 0, t.vars() > 0);
    }

    @NotNull
    public <C extends Compound> C testStructure(@NotNull String term, String bits) {
        NAR n = new Terminal(16);
        C a = (C) n.term(term).term();
        assertEquals(bits, toBinaryString(a.structure()));
        assertEquals(term, a.toString());
        return a;
    }

//    @Ignore
//    @Test
//    public void testSubtermsVector() {
//
//        NAR n = new Terminal();
//
//        Term a3 = n.term("c");
//
//        Compound a = testStructure("<c </> <a --> b>>", "1000000000000000000001000001");
//        Compound a0 = testStructure("<<a --> b> </> c>", "1000000000000000000001000001");
//
//        Compound a1 = testStructure("<c <|> <a --> b>>", "10000000000000000000001000001");
//        Compound a2 = testStructure("<c <=> <a --> b>>", "100000000000000000001000001");
//
//        Compound b = testStructure("<?1 </> <$2 --> #3>>", "1000000000000000000001001110");
//        Compound b2 = testStructure("<<$1 --> #2> </> ?3>", "1000000000000000000001001110");
//
//
//        assertTrue(a.impossibleStructureMatch(b.structure()));
//        assertFalse(a.impossibleStructureMatch(a3.structure()));
//
//
//        assertEquals("no additional structure code in upper bits",
//                a.structure(), a.structure());
//        assertEquals("no additional structure code in upper bits",
//                b.structure(), b.structure());
//
//
//    }

    @Test
    public void testImageConstruction() {
        Term e1 = imageExt($("X"), $("Y"), $("_"));
        Term e2 = imageExt($("X"), $("Y"), Op.Imdex);
        assertEquals(e1, e2);

        Term f1 = imageInt($("X"), $("Y"), $("_"));
        Term f2 = imageInt($("X"), $("Y"), Op.Imdex);
        assertEquals(f1, f2);

        assertNotEquals(e1, f1);
        assertEquals(((Compound) e1).subterms(), ((Compound) f1).subterms());
    }

    @Test
    public void testImageConstruction2() {
        assertTrue($("(/,_,X,Y)").op().image);
        assertFalse($("(X,Y)").op().image);

        assertValidTermValidConceptInvalidTaskContent(()->imageExt($("X"), $("Y")));
        assertValidTermValidConceptInvalidTaskContent(()->imageInt($("X"), $("Y")));

        assertEquals("(/,X,_)", $("(/,X,_)").toString());
        assertEquals("(/,X,_)", imageExt($("X"), $("_")).toString());

        assertEquals("(/,X,Y,_)", imageExt($("X"), $("Y"), $("_")).toString());
        assertEquals("(/,X,_,Y)", imageExt($("X"), $("_"), $("Y")).toString());
        assertEquals("(/,_,X,Y)", imageExt($("_"), $("X"), $("Y")).toString());

        assertEquals("(\\,X,Y,_)", imageInt($("X"), $("Y"), $("_")).toString());
        assertEquals("(\\,X,_,Y)", imageInt($("X"), $("_"), $("Y")).toString());
        assertEquals("(\\,_,X,Y)", imageInt($("_"), $("X"), $("Y")).toString());

    }

    public static void assertValid(Term o) {
        assertNotNull(o);
    }

    public static void assertValidTermValidConceptInvalidTaskContent(@NotNull Supplier<Term> o) {
        try {
            Term x = o.get();
            assertNotNull(x);

            Terminal t = new Terminal(8);
            t.believe(x);

            assertTrue(x + " should not have been allowed as a task content", false);


        } catch (Exception e) {
            //correct if happens here
        }
    }

    @Test
    public void testImageInhConstruction() {
        Compound p = $.p("a", "b", "c");
        assertEquals("(a-->(/,_,b,c))", $.imge(0, p).toString());
        assertEquals("(a-->(/,_,b,c))", $.image(0, p.terms()).toString());
        assertEquals("(b-->(/,a,_,c))", $.imge(1, p).toString());
        assertEquals("(c-->(/,a,b,_))", $.imge(2, p).toString());

        assertEquals("((\\,_,b,c)-->a)", $.imgi(0, p).toString());
        assertEquals("((\\,_,b,c)-->a)", $.imgi(0, p.terms()).toString());
        assertEquals("((\\,a,_,c)-->b)", $.imgi(1, p).toString());
        assertEquals("((\\,a,b,_)-->c)", $.imgi(2, p).toString());

    }

    @Test public void testStatemntString() {
        assertTrue(inh("a", "b").op().statement);
        Term aInhB = $("<a-->b>");
        assertTrue(aInhB instanceof Compound);
        assertEquals("(a-->b)",
                     aInhB.toString());
    }

    @Test
    public void testImageConstructionExt() {




        assertEquals(
            "(A-->(/,%1,_))", $("<A --> (/, %1, _)>").toString()
        );
        assertEquals(
            "(A-->(/,_,%1))", $("<A --> (/, _, %1)>").toString()
        );
//        assertEquals(
//                "(/,_,%X)", $("(/, _, %X)").toString()
//        );

        assertEquals(
                imageExt($("X"), $("_"), $("Y")), $("(/, X, _, Y)")
        );
        assertEquals(
                imageExt($("_"), $("X"), $("Y")), $("(/, _, X, Y)")
        );
        assertEquals(
                imageExt($("X"), $("Y"), $("_")), $("(/, X, Y, _)")
        );
    }
    @Test
    public void testImageConstructionInt() {
        assertEquals(
                imageInt($("X"), $("_"), $("Y")), $("(\\, X, _, Y)")
        );
        assertEquals(
                imageInt($("_"), $("X"), $("Y")), $("(\\, _, X, Y)")
        );
        assertEquals(
                imageInt($("X"), $("Y"), $("_")), $("(\\, X, Y, _)")
        );
    }

    @Test
    public void testImageOrdering1() {
        testImageOrdering('/');
    }

    @Test
    public void testImageOrdering2() {
        testImageOrdering('\\');
    }

    void testImageOrdering(char v) {
        NAR n = new Terminal(16);

        Termed<Compound> aa = n.term("(" + v + ",x, y, _)");
        Compound a = aa.term();
        Termed<Compound> bb = n.term("(" + v + ",x, _, y)");
        Compound b = bb.term();
        Termed<Compound> cc = n.term("(" + v + ",_, x, y)");
        Compound c = cc.term();
        assertNotEquals(a.dt(), b.dt());
        assertNotEquals(b.dt(), c.dt());

        assertNotEquals(a, b);
        assertNotEquals(b, c);
        assertNotEquals(a, c);

        assertNotEquals(a.hashCode(), b.hashCode());
        assertNotEquals(b.hashCode(), c.hashCode());
        assertNotEquals(a.hashCode(), c.hashCode());

        assertEquals(+1, a.compareTo(b));
        assertEquals(-1, b.compareTo(a));

        assertEquals(+1, a.compareTo(c));
        assertEquals(-1, c.compareTo(a));

        assertEquals(+1, b.compareTo(c));
        assertEquals(-1, c.compareTo(b));


    }

    @Test
    public void testImageStructuralVector() {

        String i1 = "(/,x,y,_)";
        String i2 = "(/,x,_,y)";
        Compound a = testStructure(i1, "1000000000001");
        Compound b = testStructure(i2, "1000000000001");

        /*assertNotEquals("additional structure code in upper bits",
                a.structure2(), b.structure2());*/
        assertNotEquals(a.dt(), b.dt());
        assertNotEquals("structure code influenced contentHash",
                b.hashCode(), a.hashCode());

        NAR n = new Terminal(8);
        Termed<Compound> x3 = n.term('<' + i1 + " --> z>");
        Termed<Compound> x4 = n.term('<' + i1 + " --> z>");

        assertFalse("i2 is a possible subterm of x3, structurally, even if the upper bits differ",
                x3.term().impossibleSubTermOrEquality(n.term(i2).term()));
        assertFalse(
                x4.term().impossibleSubTermOrEquality(n.term(i1).term()));


    }


    @Test
    public void testSubTermStructure() {
        NAR n = new Terminal(16);

        assertTrue(
                n.term("<a --> b>").term().impossibleSubterm(
                        n.term("<a-->b>").term()
                )
        );
        assertTrue(
                !Op.hasAll(n.term("<a --> b>").term().structure(), n.term("<a-->#b>").term().structure())
        );

    }

    @Test
    public void testCommutativeWithVariableEquality() {
        NAR n = new Terminal(16);

        Termed a = n.term("<(&&, <#1 --> M>, <#2 --> M>) ==> <#2 --> nonsense>>");
        Termed b = n.term("<(&&, <#2 --> M>, <#1 --> M>) ==> <#2 --> nonsense>>");
        assertEquals(a, b);

        Termed c = n.term("<(&&, <#1 --> M>, <#2 --> M>) ==> <#1 --> nonsense>>");
        assertNotEquals(a, c);

        Termed<Compound> x = n.term("(&&, <#1 --> M>, <#2 --> M>)");
        Term xa = x.term().term(0);
        Term xb = x.term().term(1);
        int o1 = xa.compareTo(xb);
        int o2 = xb.compareTo(xa);
        assertEquals(o1, -o2);
        assertNotEquals(0, o1);
        assertNotEquals(xa, xb);
    }

    @Test
    public void testHash1() {
        testUniqueHash("<A --> B>", "<A <-> B>");
        testUniqueHash("<A --> B>", "<A ==> B>");
        testUniqueHash("A", "B");
        testUniqueHash("%1", "%2");
        testUniqueHash("%A", "A");
        testUniqueHash("$1", "A");
        testUniqueHash("$1", "#1");
    }

    public void testUniqueHash(@NotNull String a, @NotNull String b) {
        NAR n = new Terminal(16);

        int h1 = n.term(a).hashCode();
        int h2 = n.term(b).hashCode();
        assertNotEquals(h1, h2);
    }

    @Test public void testSetOpFlags() {
        assertTrue( $("{x}").op().isSet() );
        assertTrue( $("[y]}").op().isSet() );
        assertFalse( $("x").op().isSet() );
        assertFalse( $("a:b").op().isSet() );
    }

    @Test public void testEmptyProductEquality()  {
        assertEquals( $("()"),$("()") );
        assertEquals( $("()"),Terms.ZeroProduct);
    }


    public static void assertInvalid(@NotNull Supplier<Term> o) {
        try {
            Term recv = o.get();
            if (recv!=False) //False also signals invalid reduction
                assertTrue(recv.toString() + " was not null", false);
        } catch (InvalidTermException e) {
            //correct if happens here
        } catch (Narsese.NarseseException e) {
            //correct if happens here
        }
    }

}
