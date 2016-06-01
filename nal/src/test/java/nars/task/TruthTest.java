package nars.task;

import nars.$;
import nars.Global;
import nars.truth.DefaultTruth;
import nars.truth.Truth;
import org.junit.Test;

import static nars.$.t;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;


public class TruthTest {

    @Test
    public void testFreqEquality() {
        Truth a = t(1.0f, 0.9f);
        Truth aCopy = t(1.0f, 0.9f);
        assertEquals(a, aCopy);

        Truth aEqualWithinThresh = t(1.0f- Global.TRUTH_EPSILON / 2.0f, 0.9f);
        assertEquals(a, aEqualWithinThresh);
        assertEquals(a.hashCode(), aEqualWithinThresh.hashCode());

        Truth aNotWithinThresh = t(1.0f - Global.TRUTH_EPSILON * 1.0f, 0.9f);
        assertNotEquals(a, aNotWithinThresh);
        assertNotEquals(a.hashCode(), aNotWithinThresh.hashCode());

    }

    @Test
    public void testConfEquality() {
        Truth a = t(1.0f, 0.5f);

        Truth aEqualWithinThresh = t(1.0f, 0.5f- Global.TRUTH_EPSILON / 2.0f);
        assertEquals(a, aEqualWithinThresh);
        assertEquals(a.hashCode(), aEqualWithinThresh.hashCode());

        Truth aNotWithinThresh = t(1.0f, 0.5f - Global.TRUTH_EPSILON * 1.0f);
        assertNotEquals(a, aNotWithinThresh);
        assertNotEquals(a.hashCode(), aNotWithinThresh.hashCode());
    }


//    @Test public void testEpsilon() {
//        float e = 0.1f;
//
//        Truth a = BasicTruth.get(1.0f, 0.9f, e);
//        assertEquals(a.getEpsilon(), e, 0.0001);
//
//        Truth aCopy = BasicTruth.get(1.0f, 0.9f, e);
//        assertEquals(a, aCopy);
//
//        Truth aEqualWithinThresh = BasicTruth.get(1.0f - a.getEpsilon() / 2, 0.9f, e);
//        assertEquals(a, aEqualWithinThresh);
//
//        Truth aNotWithinThresh = BasicTruth.get(1.0f - a.getEpsilon(), 0.9f, e);
//        assertNotEquals(a, aNotWithinThresh);
//    }

    @Test public void testTruthHash() {
        assertEquals( t(0.5f, 0.5f).hashCode(), t(0.5f, 0.5f).hashCode() );
        assertNotEquals( t(1.0f, 0.5f).hashCode(), t(0.5f, 0.5f).hashCode() );
        assertNotEquals( t(0.51f, 0.5f).hashCode(), t(0.5f, 0.5f).hashCode() );
        assertEquals( t(0.504f, 0.5f).hashCode(), t(0.5f, 0.5f).hashCode() );
        assertNotEquals( t(0.506f, 0.5f).hashCode(), t(0.5f, 0.5f).hashCode() );


        assertEquals( t(0, 0.01f).hashCode(), t(0, 0.01f).hashCode() );
        assertEquals( t(0.004f, 0.01f).hashCode(), t(0, 0.01f).hashCode() );
        assertNotEquals( t(0.006f, 0.01f).hashCode(), t(0, 0.01f).hashCode() );

    }

    @Test public void testInterpolate() {
        {
            Truth a = t(0.75f, 0.5f);
            Truth b = t(0.5f, 0.25f);
            assertEquals(t(0.67f, 0.41f), a.interpolate(b));
        }

        {
            Truth a = t(0.75f, 0.25f);
            Truth b = t(0.5f, 0.5f);
            assertEquals(t(0.58f, 0.41f), a.interpolate(b));
        }

        {
            Truth a = t(0.55f, 0.25f);
            Truth b = t(0.5f, 0.5f);
            assertEquals(t(0.52f, 0.48f), a.interpolate(b));
        }
        Truth a = t(0.95f, 0.5f);
        Truth b = t(0.5f, 0.01f);
        assertEquals(t(0.94f, 0.28f), a.interpolate(b));
    }

    @Test
    public void testExpectation() {
        assertEquals(0.75f, t(1f, 0.5f).expectation(), 0.01f);
        assertEquals(0.95f, t(1f, 0.9f).expectation(), 0.01f);
        assertEquals(0.05f, t(0f, 0.9f).expectation(), 0.01f);
    }

    @Test public void testTruthRevision() {
        Truth d = Revision.revision(t(1f, 0.1f), t(1f, 0.1f));
        assertEquals(1f, d.freq(), 0.01f);
        assertEquals(0.18f, d.conf(), 0.01f);

        Truth a = Revision.revision(t(1f, 0.3f), t(1f, 0.3f));
        assertEquals(1f, a.freq(), 0.01f);
        assertEquals(0.46f, a.conf(), 0.01f);

        Truth b = Revision.revision(t(0f, 0.3f), t(1f, 0.3f));
        assertEquals(0.5f, b.freq(), 0.01f);
        assertEquals(0.46f, b.conf(), 0.01f);

        Truth c = Revision.revision(t(1f, 0.9f), t(1f, 0.9f));
        assertEquals(1f, c.freq(), 0.01f);
        assertEquals(0.95f, c.conf(), 0.01f);
    }
}
