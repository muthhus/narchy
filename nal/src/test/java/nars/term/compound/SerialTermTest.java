package nars.term.compound;

import nars.Narsese;
import nars.term.Compound;
import nars.term.Term;
import org.junit.Test;

import static nars.$.$;
import static org.junit.Assert.*;

/**
 * Created by me on 2/19/17.
 */
public class SerialTermTest {

    @Test
    public void test1() throws Narsese.NarseseException {

        //testSerialize("x");
        //testSerialize("#y");
        //testSerialize("\"sdhfdkjsf\"");
        testSerialize("a:b");
        testSerialize("(a ==>+1 b)");
        testSerialize("(x --> (/, a, _, c))");
        testSerialize("(&&,(MedicalCode-->MedicalIntangible),(MedicalIntangible-->#1),(SuperficialAnatomy-->#1),label(MedicalCode,MedicalCode),label(MedicalIntangible,MedicalIntangible),label(SuperficialAnatomy,SuperficialAnatomy))");

    }

    public static void testSerialize(String x) throws Narsese.NarseseException {
        testSerialize($(x));
    }

    public static void testSerialize(Compound x) {
        SerialCompound y = new SerialCompound( x );

        System.out.println(x + " encoded to " + y.length() + " bytes");

        assertTrue(y.length() > 1);

        Compound z = y.build();

        assertNotSame(x, z);
        assertEquals(x, z);
        assertEquals(x.size(), z.size());
        assertEquals(x.volume(), z.volume());

        assertEquals(x.toString(), z.toString());
    }
}