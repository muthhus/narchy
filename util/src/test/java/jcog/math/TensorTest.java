package jcog.math;

import jcog.tensor.ArrayTensor;
import jcog.tensor.TensorChain;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class TensorTest {

    @Test
    public void testVector() {
        ArrayTensor t = new ArrayTensor(2);
        t.set(0.1f, 0);
        t.set(0.2f, 1);
        assertEquals(0, t.index(0));
//        assertEquals(0, t.coord(0, new int[1])[0]);
//        assertEquals(1, t.coord(1, new int[1])[0]);
        assertEquals("[2]<0.1000 0.2000>", t.toString());
    }

    @Test public void testMatrix() {
        ArrayTensor t = new ArrayTensor(2, 2);
        t.set(0.5f, 0, 0);
        t.set(0.25f, 1, 0);
        t.set(0.5f, 1, 1);

        assertEquals(0, t.index(0, 0));
        assertEquals(1, t.index(1, 0));
        assertEquals(2, t.index(0, 1));
        assertEquals(3, t.index(1, 1));

        final String[] s = {""};
        t.forEach((i,v)-> s[0] +=v+ " ");
        assertEquals("[0.5 0.25 0.0 0.5 ]", Arrays.toString(s));

//        assertEquals(0, t.coord(0, new int[2])[0]);
//        assertEquals(0, t.coord(0, new int[2])[1]);
//
//        assertEquals("",Arrays.toString(t.coord(1, new int[2])));
//        assertEquals(1, t.coord(1, new int[2])[0]);
//        assertEquals(0, t.coord(1, new int[2])[1]);
//
//        assertEquals(1, t.coord(2, new int[2])[0]);
//        assertEquals(0, t.coord(2, new int[2])[1]);
//
//        assertEquals(1, t.coord(3, new int[2])[0]);
//        assertEquals(1, t.coord(3, new int[2])[1]);

        assertEquals(0.25f, t.get(1, 0), 0.005f);
        assertEquals("[2, 2]<0.5000 0.2500 0.0000 0.5000>", t.toString());
    }

    @Test public void test1DTensorChain() {
        ArrayTensor a = new ArrayTensor(4);
        a.set(1, 2);
        ArrayTensor b = new ArrayTensor(2);
        b.set(2, 0);
        TensorChain ab = new TensorChain(a, b);
        assertEquals(1, ab.shape().length);
        assertEquals(6, ab.shape()[0]);

        final String[] s = {""}; ab.forEach((i,v)-> s[0] +=v+ " ");
        assertEquals("[0.0 0.0 1.0 0.0 2.0 0.0 ]", Arrays.toString(s));

    }
}