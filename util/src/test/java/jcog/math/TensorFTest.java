package jcog.math;

import org.junit.Test;

import static org.junit.Assert.*;

public class TensorFTest {

    @Test
    public void testVector() {
        TensorF t = new TensorF(2);
        t.set(0.1f, 0);
        t.set(0.2f, 1);
        assertEquals("[2]<0.1000 0.2000>", t.toString());
    }

    @Test public void testMatrix() {
        TensorF t = new TensorF(2, 2);
        t.set(0.5f, 0, 0);
        t.set(0.25f, 1, 0);
        t.set(0.5f, 1, 1);
        assertEquals(0.25f, t.get(1, 0), 0.005f);
        assertEquals("[2, 2]<0.5000 0.2500 0.0000 0.5000>", t.toString());
    }

    @Test public void testVectorComposition() {
        TensorF t = new TensorF(4);
        //TODO
    }
}