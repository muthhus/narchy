package nars.term;

import nars.$;
import nars.Narsese;
import nars.concept.VariableConcept;
import nars.nal.meta.match.VarPattern;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by me on 8/28/15.
 */
public class VariableTest {


    static final Narsese p = Narsese.the();

    @Test
    public void testPatternVarVolume() {

        assertEquals(0, p.term("$x").complexity());
        assertEquals(1, p.term("$x").volume());

        assertEquals(0, p.term("%x").complexity());
        assertEquals(1, p.term("%x").volume());

        assertEquals(p.term("<x --> y>").volume(),
                p.term("<%x --> %y>").volume());

    }

    @Test public void testNumVars() {
        assertEquals(1, p.term("$x").vars());
        assertEquals(1, p.term("#x").vars());
        assertEquals(1, p.term("?x").vars());
        assertEquals(0, p.term("%x").vars());

        //the pattern variable is not counted toward # vars
        assertEquals(1, $.$("<$x <-> %y>").vars());
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

    @Test public void testBooleanReductionViaHasPatternVar() {
        Compound d = $.$("<a <-> <$1 --> b>>");
        assertEquals(0,  d.varPattern() );

        Compound c = $.$("<a <-> <%1 --> b>>");
        assertEquals(1,  c.varPattern() );

        Compound e = $.$("<%2 <-> <%1 --> b>>");
        assertEquals(2,  e.varPattern() );

    }

    @Test public void testEqualityOfVariablesAndTheirConceptInstances() {
        @NotNull VarPattern vp0 = $.varPattern(0);
        VariableConcept vc0 = new VariableConcept(vp0);
        assertEquals(vp0, vc0);
        assertEquals(vc0, vp0); //reverse
        assertEquals(vp0, vc0.term());
    }


}
