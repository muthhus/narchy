package il.technion.tinytable.bit;


public class BitHelper {

    // 1 set, 0 unset
    private static boolean bit(final long word, final int idx) {
        return ((1L << idx) & word) != 0;
    }

    public static long fingerPrint(int size, long fingerPrint) {
        //long mask = ((1<<size)-1);
        //assertTrue(mask == BitHelper.generateMask(0, size, false));
        return fingerPrint & ((1 << size) - 1);
    }

    public static long fingerPrintDeleted(long fingerPrint) {
        return (0L) | (fingerPrint & 1L);
    }


    public static long replace(long word, final int fromBit, final int toBit, final long bitsToReplace, boolean inclusive) {
        //		final int fromBit = fromIdx & 63;
        //		final int toBit = toIdex & 63;
        //		long word = word1;

        // shift item left to the mask bits and add it to the mask.
        long mask = mask(fromBit, toBit, inclusive);


        // put the item in the mask.
        return (word & (~mask)) | (mask & (bitsToReplace << fromBit));
    }

    public static long replace(long word, final int fromBit, final int toBit, final long bitsToReplace) {
        //		final int fromBit = fromIdx & 63;
        //		final int toBit = toIdex & 63;
        //		long word = word1;

        // shift item left to the mask bits and add it to the mask.
        long mask = mask(fromBit, toBit);

        // put the item in the mask.
        return (word & (~mask)) | (mask & (bitsToReplace << fromBit));
    }


    public static long value(final long word, final int fromBit, final int toBit, final boolean inclusive) {

        return ((word & mask(fromBit, toBit, inclusive)) >>> (fromBit));
    }

    public static long value(final long word, final int fromBit, final int toBit) {


        return ((word & ((-1L << (fromBit)) ^ (-1L << (toBit)))) >>> (fromBit));
    }


    private static long mask(final int fromBit, final long toBit, boolean inclusive) {

        //		if(fromBit>toBit)
        //		{
        //			throw new RuntimeException("from: "+ fromBit +">=to bit:" +toBit +" inclusive is: " + inclusive);
        //		}
        // include from
		// zeros until from bit - then 1.
        long l2 = -1L << (toBit);
        if (inclusive)
            l2 <<= 1;
		final long l1 = -1L << (fromBit);
		return (l1 ^ l2);
    }

    public static long mask(final int fromBit, final long toBit) {
        return ((-1L << (fromBit)) ^ (-1L << (toBit)));
    }

    @Deprecated public static long replace(long[] word, final int fromBitIdx, final int toBitIdx, final long value, int wordidx) {
        //		final int fromBit = fromIdx & 63;
        //		final int toBit = toIdex & 63;
        //		long word = word1;
        // shift item left to the mask bits and add it to the mask.
        long mask = ((-1L << (fromBitIdx)) ^ (-1L << (toBitIdx)));
        long $ = (word[wordidx] & (mask)) >>> fromBitIdx;
        word[wordidx] &= (~mask);
        // put the item in the mask.
        mask &= (value << (fromBitIdx));
        word[wordidx] |= mask;
        return $;
    }

    @Deprecated public static long replace(long[] word, final int fromBit, final int toBit, final long bitsToReplace, int wordidx, boolean inclusive) {
        //		final int fromBit = fromIdx & 63;
        //		final int toBit = toIdex & 63;
        //		long word = word1;
        // shift item left to the mask bits and add it to the mask.
        long mask = mask(fromBit, toBit, inclusive);
        long $ = (word[wordidx] & (mask)) >>> fromBit;
        word[wordidx] &= (~mask);
        // put the item in the mask.
        mask &= (bitsToReplace << (fromBit));
        word[wordidx] |= mask;
        return $;
    }


    @Deprecated public static long[] ReplaceAndPush(final long word, final int fromIdx, final int toIdex, final long bitsToReplace) {
        final int fromBit = fromIdx & 63;
        final int toBit = toIdex & 63;

        final int len = toBit - fromBit;

        final long lastBit = BitHelper.bit(word, 63) ? 1 : 0;
        boolean inclusive = toIdex == 63;
        final long overflowingPart = len != 0 ? (word >>> 64 - len) & mask(0, len, false) : lastBit;

        final long constantPart = fromIdx != 0 ? word & mask(0, fromIdx, false) : 0;

        // make room for the word
        final long shiftedPart = (word << len) & mask(toBit, 63, false);
        final long newValuePart = mask(fromBit, toBit, inclusive) & (bitsToReplace << (fromBit));
        assert (Long.bitCount(newValuePart) <= len);

        final long[] retVal = new long[2];
        retVal[0] = (constantPart | shiftedPart | newValuePart);
        retVal[1] = overflowingPart;
        return retVal;
    }

    @Deprecated public static long generateMaskInclusive(final int fromBit, final long toBit) {
        return ((-1L << (fromBit)) ^ (-1L << (toBit + 1)));
    }

    @Deprecated public static long getIndexBit(final long word, final int idx) {
        return ((1L << idx) & word) >> idx;
    }


    @Deprecated private static long setOn(final long word, final int idx) {
        return ((1L << idx) | word);
    }

    @Deprecated private static long setOff(final long word, final int idx) {
        return (~(1L << idx) & word);
    }

    @Deprecated public static long set(final long word, final int idx, final boolean bit) {
        return bit ? setOn(word, idx) : setOff(word, idx);
    }

}
