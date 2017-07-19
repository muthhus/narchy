package jcog.math;

import org.junit.Test;

import java.util.Arrays;
import java.util.Random;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;

public class ByteShufflerTest {

    @Test
    public void testByteShuffler() {
        ByteShuffler b = new ByteShuffler(16);
        Random rng = new Random();
        for (int i = 2; i < 5; i ++)
            testPermutes(b, rng, i);
    }

    void testPermutes(ByteShuffler b, Random rng, int len) {
        int permutations = (int) org.apache.commons.math3.util.CombinatoricsUtils.factorial(len);
        int iterates = permutations * 6 /* to be sure */;
        TreeSet<String> combos = new TreeSet<>();
        for (int i = 0; i < iterates; i++) {
            byte[] order = b.shuffle(rng, len, true);
            assertEquals(len, order.length );
            combos.add( Arrays.toString(order) );
        }
        //System.out.println(combos);
        assertEquals(permutations, combos.size());
    }

}