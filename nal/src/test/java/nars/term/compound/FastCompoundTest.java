package nars.term.compound;

import nars.Narsese;
import nars.term.Compound;

import static nars.$.$;
import static org.junit.Assert.assertEquals;

class FastCompoundTest {

    public static void main(String[] args) throws Narsese.NarseseException {
        Compound c = $("(&&,(MedicalCode-->MedicalIntangible),(MedicalIntangible-->#1),(SuperficialAnatomy-->#1),label(MedicalCode,MedicalCode),label(MedicalIntangible,MedicalIntangible),label(SuperficialAnatomy,SuperficialAnatomy))");
        System.out.println(c);
        FastCompound f = FastCompound.get( c );
        f.print();
        assertEquals(c.op(), f.op());
        assertEquals(c.subs(), f.subs());
        int s = f.subterms().subs();
        assertEquals(c.subterms().subs(), s);
        for (int i = 0; i < s; i++)
            assertEquals(c.subterms().sub(i), f.subterms().sub(i));
        System.out.println( f.toString() );
    }

}