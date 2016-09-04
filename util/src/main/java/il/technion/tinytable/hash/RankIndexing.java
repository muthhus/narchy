package il.technion.tinytable.hash;


import org.eclipse.collections.api.list.primitive.IntList;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;

public class RankIndexing {

    /**
     * The basic rank operation, composed of a bitwise and and a bit count instruction.
     *
     * @param Istar
     * @param idx
     * @return
     */
    public static int rank(long Istar, int idx) {
        return Long.bitCount(Istar & ((1L << idx) - 1));
    }


    /**
     * Counts the number of items stored in this index with bitwise manipulations.
     *
     * @param I0
     * @param IStar
     * @return
     */
    public static int getNrItems(long I0, long IStar) {
        int baseOffset = Long.bitCount(I0);

        int offset = 0;
        while (true) {
			int nextOffset = Long.bitCount(IStar & ((1L << (baseOffset + offset)) - 1));
            if (nextOffset == offset || nextOffset == 0)
                break;
            offset = nextOffset;
			int i = 0;
			i++;

        }
        return baseOffset + offset;

    }

    /**
     * return true if the chainID bit is set in I0.
     *
     * @param I0
     * @param chainId
     * @return
     */
    public static boolean chainExist(long I0, int chainId) {
        long mask = (1L << chainId);
        return (I0 & mask) != 0;
    }


    /**
     * Calculates the offset of each chain in the array relative to the last chain.
     *
     * @param I0
     * @param IStar
     * @param offsets  - an array to write the result in
     * @param bucketId
     */
    public static void getItemsPerLevelUpTo64New(long[] I0, long[] IStar, final byte[] offsets, int bucketId) {
        offsets[0] = (byte) Long.bitCount(I0[bucketId]);


        byte offset = 0;
        int i = 1;
        while (true) {
            long tempMask = (1L << (offsets[0] + offset)) - 1;
            byte nextOffset = (byte) Long.bitCount(IStar[bucketId] & tempMask);

            //Long.bitCount(IStar&((1l<<(offsets[0]+offset)-1));

//					rank(IStar,offsets[0]+offset);
            byte x = offsets[i++] = (byte) (nextOffset - offset);
            if (x <= 0)
                break;
//			if(nextOffset == offset)
//				break;
            offset = nextOffset;
        }

    }

    public static int[] getItemsPerLevelUpTo64(long I0, long IStar) {
        int[] temp = new int[65];
        temp[0] = Long.bitCount(I0);


        int offset = 0;
        int i = 1;
        while (true) {
            int nextOffset = rank(IStar, temp[0] + offset);
            temp[i++] = nextOffset - offset;
            if ((nextOffset - offset) <= 0)
                break;
            if (nextOffset == offset)
                break;
            offset = nextOffset;
        }
        return temp;


    }


    /**
     * Calculate the index of the next chain according to the index.
     *
     * @param chainNumber
     * @param index
     * @param offset      - the offset of the bit in the index that tells us where the chain is
     * @return
     */
    public static int getNextChainIndex(int chainNumber, long index, int offset) {
        long modified = index >> offset;
        long mask = 1L << chainNumber;
        return ((modified) & (mask)) != 0L ? Long.bitCount(modified & (mask - 1)) : -1;
    }


    /**
     * Get the index of an item to add.
     *
     * @param fpaux
     * @param I0
     * @param IStar
     * @param offsets - outputs the offset of each item
     * @param chain   - outputs the indices of all items in the chain here.
     * @return the item's index in the bucket
     */
    public static int addItem(final FingerPrint fpaux, long[] I0, long[] IStar,
                              byte[] offsets, byte[] chain) {
        long mask = 1L << fpaux.chainId;

        int bucketId = fpaux.bucketId;

        if ((mask & I0[bucketId]) == 0L) {
            I0[bucketId] |= mask;
            int offset = Long.bitCount(I0[bucketId] & ((1L << fpaux.chainId) - 1));
            //rank(I0[fpaux.bucketId],fpaux.chainId);
            IStar[bucketId] = extendZero(IStar[bucketId], offset);
            return offset;
        }
        int lastChainLocation = getChainAndUpdateOffsets(fpaux, I0, IStar, offsets, chain) - 1;// YARON added -1
        //getChainWithoutUpdatingOffsets(fpaux,I0, IStar,offsets,chain);
        IStar[bucketId] |= (1L << chain[lastChainLocation]);
        offsets[lastChainLocation + 1]++;
        //getChainAndUpdateOffsets(fpaux,I0,IStar,offsets,chain);
        getChainWithoutUpdatingOffsets(fpaux, I0, IStar, offsets, chain);
        IStar[bucketId] = extendZero(IStar[bucketId], chain[lastChainLocation + 1]);

        return chain[lastChainLocation + 1];
    }

    public static int RemoveItem(int chainNumber, long[] I0, long[] IStar, int bucketId, byte[] offsets,
                                 byte[] chain, int maxChainSize) {
        if (maxChainSize == 0) {
            I0[bucketId] &= ~(1L << chainNumber);
            IStar[bucketId] = shrinkOffset(IStar[bucketId], chain[0]);
            return chain[maxChainSize];

        } else {
            IStar[bucketId] = IStar[bucketId] & (~(1L << chain[maxChainSize - 1]));
            IStar[bucketId] = shrinkOffset(IStar[bucketId], chain[maxChainSize]);
            return chain[maxChainSize];
        }

    }


    private static long extendZero(final long IStar, final int offset) {
        long conMask = (1L << offset) - 1;
        return (IStar & conMask) | ((IStar << 1) & (~(conMask)) & (~(1L << offset)));
    }

    /*
     *  The 'offset' bit, is assumed to be 0.
     */
    private static long shrinkOffset(long IStar, int offset) {
        long conMask = ((1L << offset) - 1);
        return (IStar & conMask) | ((~(conMask) & IStar) >>> 1);
    }


    public static int getFirst(int chainNumber, long I0, long IStar) {
        return getNextChainIndex(chainNumber, I0, 0);
    }


    /**
     * return the requested chain offsets in the chain byte array.
     * also updates the I* offsets in the offsets byte array.
     *
     * @param fpaux
     * @param I0
     * @param IStar
     * @param offsets
     * @param chain
     * @param chainId
     * @return
     */
    public static int getChainAndUpdateOffsets(FingerPrint fpaux,
                                               long[] I0, long[] IStar, byte[] offsets, byte[] chain, int chainId) {
        //		List<Integer> itemIdx = new ArrayList<Integer>(3);
        getItemsPerLevelUpTo64New(I0, IStar, offsets, fpaux.bucketId);
        // read first item
        int firstItem = getNextChainIndex(chainId, I0[fpaux.bucketId], 0);
        chain[0] = (byte) firstItem;
        //		itemIdx.add(firstItem);
        if (firstItem < 0) // removed from comment by Yaron
            return 0; //removed from comment by Yaron

        int offset = 0;
        int i = 0;
        while (true) {
            firstItem = getNextChainIndex(firstItem, IStar[fpaux.bucketId], offset);
            if (firstItem < 0) {
                chain[++i] = -1;
                return i;// changed to i by Yaron return i-1;
            }
            offset += offsets[i];
            chain[++i] = (byte) (offset + firstItem);
        }

    }

    /**
     * Calculate an array of indices to each value.
     *
     * @param fpaux
     * @param I0
     * @param IStar
     * @param offsets - this is an array of the offset of each chain from the last chain.
     * @param chain   - this is the resulting array of indices.
     * @return
     */
    public static int getChainAndUpdateOffsets(FingerPrint fpaux, long[] I0,
                                               long[] IStar, byte[] offsets, byte[] chain) {
        return getChainAndUpdateOffsets(fpaux, I0, IStar, offsets, chain,
                fpaux.chainId);
    }

    /**
     * relatively unoptimized, as it allocates memory!
     *
     * @param chainNumber
     * @param I0
     * @param IStar
     * @return
     */
    public static IntList getChain(int chainNumber, long I0, long IStar) {
        IntArrayList itemIdx = new IntArrayList(3);
        int[] offsets = getItemsPerLevelUpTo64(I0, IStar);

        int firstItem = getNextChainIndex(chainNumber, I0, 0);

        itemIdx.add(firstItem);


        int offset = 0;
        int i = 0;
        while (firstItem >= 0) {
            firstItem = getNextChainIndex(firstItem, IStar, offset);
            if (firstItem < 0)
                return itemIdx;
            offset += offsets[i];
            // item offset within level.  + level offset
            int levelOffset = offset;

            //			if(!itemIdx.contains(levelOffset+firstItem))
            itemIdx.add(levelOffset + firstItem);
            // go for next level. - if needed.

            i++;


        }
        return itemIdx;

    }


    /**
     * used to prevent calculating offsets twice. it is slightly more efficient than getchainAndUpdateOffsets, but the offsets need to be
     * already updated. used after addition/removals when offsets can be updated manually to include a new addition/removal.
     *
     * @param fpaux
     * @param I0
     * @param IStar
     * @param offsets
     * @param chain
     * @return
     */
    public static int getChainWithoutUpdatingOffsets(FingerPrint fpaux,
                                                     long[] I0, long[] IStar, byte[] offsets, byte[] chain) {

        int nextItem = getNextChainIndex(fpaux.chainId, I0[fpaux.bucketId], 0);
        chain[0] = (byte) nextItem;

        int offset = 0;
        int i = 0;
        while (true) {
            nextItem = getNextChainIndex(nextItem, IStar[fpaux.bucketId], offset);
            if (nextItem < 0) {
                chain[++i] = -1;
                return i - 1;
            }
            offset += offsets[i];
            chain[++i] = (byte) (offset + nextItem);
        }
    }

}












