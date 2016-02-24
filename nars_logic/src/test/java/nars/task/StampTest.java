package nars.task;

import com.google.common.collect.Lists;
import com.gs.collections.impl.list.mutable.primitive.LongArrayList;
import nars.truth.Stamp;
import org.junit.Test;

import java.util.Arrays;

import static junit.framework.TestCase.assertTrue;
import static nars.truth.Stamp.toSetArray;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 *
 * @author me
 */


public class StampTest {

    static long[] a(long... x) {
        return x;
    }

    @Test
    public void testOverlap() {


        assertTrue(Stamp.overlapping(a(1, 2), a(2)));
        assertTrue(Stamp.overlapping(a(1), a(1, 2)));
        assertFalse(Stamp.overlapping(a(1), a(2)));
        assertFalse(Stamp.overlapping(a(2), a(1)));
        assertFalse(Stamp.overlapping(a(1, 2), a(3, 4)));
        assertTrue(Stamp.overlapping(a(1, 2), a(2, 3)));
        assertTrue(Stamp.overlapping(a(2, 3), a(1, 2)));
        assertFalse(Stamp.overlapping(a(2, 3), a(1)));

        assertFalse(Stamp.overlapping(a(1), a(2, 3, 4, 5, 6)));
        assertFalse(Stamp.overlapping(a(2, 3, 4, 5, 6), a(1)));


    }

    @Test public void testStampZip() {

        assertArrayEquals(
            new long[] { 1, 2, 3 },
            Stamp.zip(new long[] { 1, 2}, new long[] { 3, 4}, 3)
        );
        assertArrayEquals(
            new long[] { 1, 2, 3, 4 },
            Stamp.zip(new long[] { 1, 2}, new long[] { 3, 4}, 4)
        );
        assertArrayEquals(
            new long[] { 1, 2, 3, 4 },
            Stamp.zip(new long[] { 1 }, new long[] { 2, 3, 4}, 4)
        );
        assertArrayEquals(
            new long[] { 1, 2, 3, 4 },
            Stamp.zip(new long[] { 1,2,3 }, new long[] { 4 }, 4)
        );
        assertArrayEquals(
            new long[] { 0, 1, 2, 4 },
            Stamp.zip(new long[] { 0, 1,2,3 }, new long[] { 4 }, 4)
        );

        //no duplicates
        assertArrayEquals(
            new long[] { 0, 1, 2, 3 },
            Stamp.zip(new long[] { 0, 1,2 }, new long[] { 2, 3, 4 }, 4)
        );
    }

    @Test
    public void testStampToSetArray() {
        assertTrue(toSetArray(new long[] { 1, 2, 3 }).length == 3);
        assertTrue(toSetArray(new long[] { 1, 1, 3 }).length == 2);
        assertTrue(toSetArray(new long[] { 1 }).length == 1);
        assertTrue(toSetArray(new long[] {  }).length == 0);
        assertTrue(
                Arrays.hashCode(toSetArray(new long[] { 3,2,1 }))
                ==
                Arrays.hashCode(toSetArray(new long[] { 2,3,1 }))
        );
        assertTrue(
                Arrays.hashCode(toSetArray(new long[] { 1,2,3 }))
                !=
                Arrays.hashCode(toSetArray(new long[] { 1,1,3 }))
        );
    }

    @Test
    public void testStampPreservesOldestEvidence() {
        assertArrayEquals(
                new long[] { 1, 3 },
                Stamp.zip(new long[] { 1, 2}, new long[] { 3, 4}, 2)
        );

        assertArrayEquals(
                new long[] { 1, 2, 3, 4 },
                Stamp.zip(new long[] { 1, 2, 8, 12}, new long[] { 3, 4, 7, 13}, 4)
        );


        long[] a = new long[] { 1, 2, 10, 11 };
        long[] b = new long[] { 3, 5, 7, 22 };
        assertEquals(
                new LongArrayList(new long[] { 1, 2, 3, 5}),
                new LongArrayList(Stamp.zip(a, b, 4)));
    }

}
