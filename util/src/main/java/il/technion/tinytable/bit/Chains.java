package il.technion.tinytable.bit;


import org.eclipse.collections.api.list.primitive.LongList;
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;

import java.util.Collection;


public class Chains {

    private static boolean isFingerprint(long item) {
        return ((item & 1L) == 1L);
    }

    public static long setFingerprint(long item) {
        return item | 1L;
    }

    private static int findFingerprint(long[] chain, long fingerprint) {
        if (chain == null || chain.length < 1)
            return -1;

        for (int i = 0; i < chain.length; i++) {
            if (isFingerprint(chain[i])) {
                if (chain[i] == fingerprint)
                    return i;
            }
        }
        return -1;
    }

    public static long size(long[] chain, long fingerprint, int itemSize) {
        int idx = findFingerprint(chain, fingerprint);
        if (idx < 0)
            return 0;

        if (idx >= chain.length - 1)
            return 1;
        long counter = 0L;
        int offset = 0;
        for (int i = idx + 1; i < chain.length; i++) {
            if (isFingerprint(chain[i]))
                break;
            //assemble the counter
            long newPart = ((chain[i] >>> 1) << (offset * itemSize));
            offset++;
            counter |= newPart;
        }
        return 1 + counter;
    }

    //
    public static LongList set(long[] chain, long fingerprint, int itemSize, long newCounter) {
        int idx = findFingerprint(chain, fingerprint);
        if (idx < 0)
            throw new RuntimeException("cannot find fingerprint");

        LongArrayList newChain = new LongArrayList(chain.length);

        // the first occurance is the fingerprint itself.
        // all items that are not
        int i;
        for (i = 0; i <= idx; i++) {
            newChain.add(chain[i]);
        }


        // remove old counter.
        for (; i < chain.length; i++) {
            if (isFingerprint(chain[i])) {
                break;
            }

        }

        long value = newCounter - 1;
        newChain = packNewCounterToChain(itemSize, newChain, value);


        // pack back all other
        for (; i < chain.length; i++) {
            newChain.add(chain[i]);
        }
        return newChain;
    }

    private static LongArrayList packNewCounterToChain(int itemSize,
                                                         LongArrayList newChain, long value) {
        // pack back the value into the chain.
        int ctrBitSize = itemSize - 1;

        long mask = (1L << (ctrBitSize)) - 1L;
        //System.out.println()
        assert (Long.bitCount(mask) == ctrBitSize);

        while (value != 0) {
            // mark the value as counter and remove un needed bits. If the LSB bit is zero it is a counter.
            long item = ((value & mask));
            item = item << 1;

            value = value >>> ctrBitSize;

            // add it to chain.
            newChain.add(item);
            // shift the remaining value to remove stored bits.
        }
        return newChain;
    }

    public static long[] toArray(Collection<Long> items) {
        int i = 0;
        long[] $ = new long[items.size()];
        for (Long long1 : items) {
            $[i++] = long1;
        }
        return $;
    }


}

