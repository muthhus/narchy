package jcog.data.bit;

import jcog.TODO;

import java.util.Arrays;

/**
 * Bare metal bitset implementation. For performance reasons, this
 * implementation does not check for index bounds nor expand the bitset size if
 * the specified index is greater than the size.
 */
abstract public class MetalBitSet {

    public abstract boolean get(int i);

    public abstract void set(int i);

    public abstract void clear();

    public static class LongArrayBitSet extends MetalBitSet {
        final long[] data;

        protected LongArrayBitSet(long bits) {
            this(new long[(int) Math.ceil((double) bits / Long.SIZE)]);
        }

        /**
         * Deserialize long array as bitset.
         *
         * @param data
         */
        public LongArrayBitSet(long[] data) {
            assert data.length > 0;
            this.data = data;
        }

        public void clear() {
            Arrays.fill(data, 0);
        }

        /**
         * Sets the bit at specified index.
         *
         * @param index
         */
        public void set(int index) {
            data[(int) (index >>> 6)] |= (1L << index);
        }


        /**
         * number of bits set to true
         */
        public int cardinality() {
            int sum = 0;
            for (long l : data)
                sum += Long.bitCount(l);
            return sum;
        }

        public boolean getAndSet(int index, boolean next) {
            int i = (int) (index >>> 6);
            int j = (int) (1L << index);
            long[] d = this.data;
            boolean prev = (d[i] & j) != 0;
            if (prev != next) {
                if (next) {
                    d[i] |= j;
                } else {
                    //clear
                    d[i] &= ~j;
                }
            }
            return prev;
        }

        /**
         * Returns true if the bit is set in the specified index.
         *
         * @param index
         * @return
         */
        public boolean get(int index) {
            return (data[(int) (index >>> 6)] & (1L << index)) != 0;
        }

        /**
         * Number of bits
         */
        public long bitSize() {
            return (long) data.length * Long.SIZE;
        }

        public long[] getData() {
            return data;
        }

        /**
         * Combines the two BitArrays using bitwise OR.
         */
        public void putAll(LongArrayBitSet array) {
            assert data.length == array.data.length :
                    "BitArrays must be of equal length (" + data.length + "!= " + array.data.length + ')';
            for (int i = 0; i < data.length; i++) {
                data[i] |= array.data[i];
            }
        }


        /**
         * Returns the index of the first bit that is set to {@code false}
         * that occurs on or after the specified starting index.
         *
         * @param fromIndex the index to start checking from (inclusive)
         * @return the index of the next clear bit
         * @throws IndexOutOfBoundsException if the specified index is negative
         * @since 1.4
         */
        public int nextClearBit() {

            if (data.length > 1)
                throw new TODO();
            return Long.numberOfLeadingZeros(~data[0]);

            //        int u = 0;
            //
            //        long word = ~words[u] & (WORD_MASK << fromIndex);
            //
            //        while (true) {
            //            if (word != 0)
            //                return (u * BITS_PER_WORD) + Long.numberOfTrailingZeros(word);
            //            if (++u == wordsInUse)
            //                return wordsInUse * BITS_PER_WORD;
            //            word = ~words[u];
            //        }

        }

    }


    public static class IntBitSet extends MetalBitSet {
        private int x;

        @Override
        public boolean get(int i) {
            return (x & (1 << i)) != 0;
        }

        @Override
        public void set(int i) {
            x |= (1 << i);
        }

        @Override
        public void clear() {
            x = 0;
        }

    }

    public static MetalBitSet bits(int choices) {
        if (choices < 32) {
            return new IntBitSet();
        } else {
            return new LongArrayBitSet(choices);
        }
    }
}
