package nars.term.compound;

import nars.Narsese;
import nars.term.Compound;
import org.junit.Test;

import static nars.$.$;
import static org.junit.Assert.assertEquals;

public class FastCompoundTest {


    @Test
    public void test1() throws Narsese.NarseseException {
        assertEquivalent("(&&,(MedicalCode-->MedicalIntangible),(MedicalIntangible-->#1),(SuperficialAnatomy-->#1),label(MedicalCode,MedicalCode),label(MedicalIntangible,MedicalIntangible),label(SuperficialAnatomy,SuperficialAnatomy))");
    }
        @Test
    public void test2() throws Narsese.NarseseException {
        assertEquivalent("(((x)))");
        assertEquivalent("((x))");
        assertEquivalent("(((P-->S)))");
        assertEquivalent("(P-->S)");
        assertEquivalent("((P-->S))");
        assertEquivalent("((P-->S),x)");

        assertEquivalent("(((P-->S)),x)");
        assertEquivalent("task(\"?\")");
        assertEquivalent("((P-->S),(S-->P),task(x))");
        assertEquivalent("(((Conversion-->Belief)),(P-->S))");
        assertEquivalent("(((Conversion-->Belief),(Belief-->Punctuation)),(P-->S))");
        assertEquivalent("((P-->S),((Conversion-->Belief),(Belief-->Punctuation)))");
    }
    @Test
    public void test2b() throws Narsese.NarseseException {
        assertEquivalent("(((P-->S),(S-->P),task(\"?\")),((P-->S),((Conversion-->Belief),(Belief-->Punctuation))))");
    }

    static void assertEquivalent(String c) throws Narsese.NarseseException {
        assertEquivalent($(c));
    }

    static void assertEquivalent(Compound c) {
        FastCompound f = FastCompound.get( c );
        assertEquals(c.op(), f.op());
        assertEquals(c.subs(), f.subs());
        int s = f.subterms().subs();
        assertEquals(c.subterms().subs(), s);
        for (int i = 0; i < s; i++)
            assertEquals(c.subterms().sub(i), f.subterms().sub(i));

        assertEquals(c.structure(), f.structure());
        assertEquals(c.complexity(), f.complexity());
        assertEquals(c.volume(), f.volume());
        assertEquals(c.toString(), f.toString());

//        f.print();
    }

}