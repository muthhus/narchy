package jcog.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class ArrayPoolTest {

    @Test
    public void testBytePool() {
        ArrayPool<byte[]> p = ArrayPool.bytes();
        byte[] b1 = p.getMin(1);
        assertEquals(1, b1.length);
        byte[] b2 = p.getMin(2);
        assertEquals(2, b2.length);
        p.release(b1);
        byte[] b1again = p.getMin(1);
        assertSame(b1, b1again);

    }
}