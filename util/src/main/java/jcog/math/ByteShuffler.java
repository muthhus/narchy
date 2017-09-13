package jcog.math;

import java.util.Arrays;
import java.util.Random;

public class ByteShuffler {

    public final byte[] order;

    public ByteShuffler(int capacity) {
        this.order = new byte[capacity];
    }

    public byte[] shuffle(Random rng, int len, boolean copy) {
        assert(len >= 2 && len < 127);
        for (byte i = 0; i < ((byte)len); i++)
            order[i] = i; //caution: there could be numbers remaining beyond the set range here

        long rndInt = 0; //generate one 64 bit random long every 8 cycles
        int generate = 0;
        for (int i=0; i < len; i++) {
            if ((generate++ & 8) == 0)
                rndInt = rng.nextLong();
            else
                rndInt >>= 8;
            int j = ((int)(rndInt & 0xff)) % len;
            if (i!=j) {
                byte x = order[i];
                order[i] = order[j];
                order[j] = x;
            }
        }

        return copy ? Arrays.copyOfRange(order, 0, len) : order;
    }

    public void shuffle(Random rng, Object[] a, int from, int to) {
        int len = to - from;
        assert(len >= 2 && len < 127);

        long rndInt = 0; //generate one 64 bit random long every 8 cycles
        int generate = 0;
        for (int i=0; i < len; i++) {
            if ((generate++ & 8) == 0)
                rndInt = rng.nextLong();
            else
                rndInt >>= 8;
            int j = ((int)(rndInt & 0xff)) % len;
            if (i!=j) {
                Object x = a[i];
                a[i] = a[j];
                a[j] = x;
            }
        }
    }

}
