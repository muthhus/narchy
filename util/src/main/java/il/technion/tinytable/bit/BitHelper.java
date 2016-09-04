package il.technion.tinytable.bit;


public class BitHelper {


    public static long adjustFingerPrint(int size, long fingerPrint) {
        //long mask = ((1<<size)-1);
        //Assert.assertTrue(mask == BitHelper.generateMask(0, size, false));
        return fingerPrint & ((1 << size) - 1);

        //return fingerPrint;
    }

    public static long markFingerPrintAsDeleted(long fingerPrint) {
        return (0L) | (fingerPrint & 1L);
    }


    public static long ReplaceBitsInWord(long word, final int fromBit, final int toBit, final long bitsToReplace, boolean inclusive) {
        //		final int fromBit = fromIdx & 63;
        //		final int toBit = toIdex & 63;
        //		long word = word1;
        // shift item left to the mask bits and add it to the mask.
        long mask = generateMask(fromBit, toBit, inclusive);

        word &= (~mask);
        // put the item in the mask.
        mask &= (bitsToReplace << (fromBit));
        return word |= mask;
    }

    public static long ReplaceBitsInWord(long word, final int fromBit, final int toBit, final long bitsToReplace) {
        //		final int fromBit = fromIdx & 63;
        //		final int toBit = toIdex & 63;
        //		long word = word1;
        // shift item left to the mask bits and add it to the mask.
        long mask = generateMask(fromBit, toBit);

        word &= (~mask);
        // put the item in the mask.
        mask &= (bitsToReplace << (fromBit));
        return word |= mask;
    }

    public static long ReplaceBitsInWord(long[] word, final int fromBitIdx, final int toBitIdx, final long value, int wordidx) {
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

    public static long ReplaceBitsInWord(long[] word, final int fromBit, final int toBit, final long bitsToReplace, int wordidx, boolean inclusive) {
        //		final int fromBit = fromIdx & 63;
        //		final int toBit = toIdex & 63;
        //		long word = word1;
        // shift item left to the mask bits and add it to the mask.
        long mask = generateMask(fromBit, toBit, inclusive);
        long $ = (word[wordidx] & (mask)) >>> fromBit;
        word[wordidx] &= (~mask);
        // put the item in the mask.
        mask &= (bitsToReplace << (fromBit));
        word[wordidx] |= mask;
        return $;
    }


    public static long[] ReplaceAndPush(final long word, final int fromIdx, final int toIdex, final long bitsToReplace) {
        final int fromBit = fromIdx & 63;
        final int toBit = toIdex & 63;

        final int len = toBit - fromBit;

        final long lastBit = BitHelper.getBit(word, 63) ? 1 : 0;
        boolean inclusive = toIdex == 63;
        final long overflowingPart = len != 0 ? (word >>> 64 - len) & generateMask(0, len, false) : lastBit;

        final long constantPart = fromIdx != 0 ? word & generateMask(0, fromIdx, false) : 0;

        // make room for the word
        final long shiftedPart = (word << len) & generateMask(toBit, 63, false);
        final long newValuePart = generateMask(fromBit, toBit, inclusive) & (bitsToReplace << (fromBit));
        assert (Long.bitCount(newValuePart) <= len);

        final long[] retVal = new long[2];
        retVal[0] = (constantPart | shiftedPart | newValuePart);
        retVal[1] = overflowingPart;
        return retVal;
    }

    public static long getValueFromWord(final long word, final int fromBit, final int toBit, final boolean inclusive) {

        return ((word & generateMask(fromBit, toBit, inclusive)) >>> (fromBit));
    }

    public static long getValueFromWord(final long word, final int fromBit, final int toBit) {


        return ((word & ((-1L << (fromBit)) ^ (-1L << (toBit)))) >>> (fromBit));
    }


    public static long generateMask(final int fromBit, final long toBit, boolean inclusive) {

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

    public static long generateMask(final int fromBit, final long toBit) {
        return ((-1L << (fromBit)) ^ (-1L << (toBit)));
    }

    public static long generateMaskInclusive(final int fromBit, final long toBit) {
        return ((-1L << (fromBit)) ^ (-1L << (toBit + 1)));
    }

    public static long getIndexBit(final long word, final int idx) {
        return ((1L << idx) & word) >> idx;
    }


    // 1 set, 0 unset
    public static boolean getBit(final long word, final int idx) {
        return ((1L << idx) & word) != 0;
    }

    // 1 set, 0 unset
    private static long setBitTo1(final long word, final int idx) {
        return ((1L << idx) | word);
    }

    private static long setBitTo0(final long word, final int idx) {
        return (~(1L << idx) & word);
    }

    public static long setBit(final long word, final int idx, final boolean bit) {
        if (bit)
            return setBitTo1(word, idx);
        return setBitTo0(word, idx);
    }

}
