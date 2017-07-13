package nars.term;

import nars.$;
import nars.Narsese;
import nars.term.atom.Atomic;
import org.junit.Test;

import static nars.$.$;
import static org.junit.Assert.*;

/**
 * Created by me on 8/28/15.
 */
public class VariableTest {


    static final Narsese p = Narsese.the();

    @Test
    public void testPatternVarVolume() throws Narsese.NarseseException {

        assertEquals(0, p.term("$x").complexity());
        assertEquals(1, p.term("$x").volume());

        assertEquals(0, p.term("%x").complexity());
        assertEquals(1, p.term("%x").volume());

        assertEquals(p.term("<x --> y>").volume(),
                p.term("<%x --> %y>").volume());

    }

    @Test public void testNumVars() throws Narsese.NarseseException {
        assertEquals(1, p.term("$x").vars());
        assertEquals(1, p.term("#x").vars());
        assertEquals(1, p.term("?x").vars());
        assertEquals(0, p.term("%x").vars());

        //the pattern variable is not counted toward # vars
        assertEquals(1, $("<$x <-> %y>").vars());
    }

//    @Test
//    public void testIndpVarNorm() {
//        assertEquals(2, $.$("<$x <-> $y>").vars());
//
//        testIndpVarNorm("$x", "$y", "($1,$2)");
//        testIndpVarNorm("$x", "$x", "($1,$1)");
//        testIndpVarNorm("$x", "#x", "($1,#2)");
//        testIndpVarNorm("#x", "#x", "(#1,#1)");
//    }
//
//    @Test
//    public void testIndpVarNormCompound() {
//        //testIndpVarNorm("<$x <-> $y>", "<$x <-> $y>", "(<$1 <-> $2>, <$3 <-> $4>)");
//
//        testIndpVarNorm("$x", "$x", "($1,$1)");
//        testIndpVarNorm("#x", "#x", "(#1,#1)");
//        testIndpVarNorm("<#x<->#y>", "<#x<->#y>", "((#1<->#2),(#1<->#2))");
//        testIndpVarNorm("<$x<->$y>", "<$x<->$y>", "(($1<->$2),($1<->$2))");
//    }
//    public void testIndpVarNorm(String vara, String varb, String expect) {
//
//
//        Term a = $.$(vara);
//        Term b = $.$(varb);
//        //System.out.println(a + " " + b + " "  + Product.make(a, b).normalized().toString());
//
//        assertEquals(
//            expect,
//            t.concept($.p(a, b)).toString()
//        );
//    }

    @Test public void testBooleanReductionViaHasPatternVar() throws Narsese.NarseseException {
        Compound d = $("<a <-> <$1 --> b>>");
        assertEquals(0,  d.varPattern() );

        Compound c = $("<a <-> <%1 --> b>>");
        assertEquals(1,  c.varPattern() );

        Compound e = $("<%2 <-> <%1 --> b>>");
        assertEquals(2,  e.varPattern() );

    }

    /** tests term sort order consistency */
    @Test public void testVariableSubtermSortAffect0() {

        assertEquals(-1, $.varIndep(1).compareTo($.varIndep(2)));


        Compound k1 = $.inh($.varIndep(1), Atomic.the("key")); //raw("($1 --> key)");
        Compound k2 = $.inh($.varIndep(2), Atomic.the("key")); //raw("($2 --> key)");
        Compound l1 = $.inh($.varIndep(1), Atomic.the("lock")); //raw("($1 --> lock)");
        Compound l2 = $.inh($.varIndep(2), Atomic.the("lock")); //raw("($2 --> lock)");
        assertEquals(-1, k1.compareTo(k2));
        assertEquals(+1, k2.compareTo(k1));
        assertEquals(-1, l1.compareTo(l2));
        assertEquals(+1, l2.compareTo(l1));

        assertEquals(l1.compareTo(k1), -k1.compareTo(l1));
        assertEquals(l2.compareTo(k2), -k2.compareTo(l2));

        //HERE IS THE EXCEPTION: non-variable terms need to be compared first, because normalization implicitly involves the subservience of variables to non-variables
        assertEquals(-1, k1.compareTo(l2));
        assertEquals(-1, k2.compareTo(l1));
        assertEquals(+1, l2.compareTo(k1));
        assertEquals(+1, l1.compareTo(k2));


        testVariableSorting("(($1-->key)&&($2-->lock))", "(($1-->lock)&&($2-->key))");

    }

    /** tests term sort order consistency */
    @Test public void testVariableSubtermSortAffectNonComm() {

        testVariableSorting("(($1-->key),($2-->lock))", "(($2-->key),($1-->lock))");

    }

    static void testVariableSorting(String a, String b) {
        Compound A = raw(a);
        Compound B = raw(b);

        //not equal...
        assertNotEquals(A, B);

        //but normalization should make them equal:
        Term NA = $.terms.normalize(A);
        Term NB = $.terms.normalize(B);
        System.out.println(A + "\t" + B);
        System.out.println(NA + "\t" + NB);
        assertEquals(NA, NB);
    }

    private static Compound raw(String a) {
        try {
            return (Compound) $.terms.termRaw(a);
        } catch (Narsese.NarseseException e) {
            assertTrue(false);
            return null;
        }
   }

    /** tests term sort order consistency */
    @Test public void testVariableSubtermSortAffect1() {

        testVariableSorting("((($1-->key)&&($2-->lock))==>open($1,$2))", "((($1-->lock)&&($2-->key))==>open($2,$1))");

    }
//    @Test public void testEqualityOfVariablesAndTheirConceptInstances() {
//        @NotNull VarPattern vp0 = $.varPattern(0);
//        VariableConcept vc0 = new VariableConcept(vp0);
//        assertEquals(vp0, vc0);
//        assertEquals(vc0, vp0); //reverse
//        assertEquals(vp0, vc0.term());
//    }


}
