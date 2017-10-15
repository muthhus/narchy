package jcog.bit;

import java.util.Arrays;

/**
 * Bare metal bitset implementation. For performance reasons, this
 * implementation does not check for index bounds nor expand the bitset size if
 * the specified index is greater than the size.
 */
public class MetalBitSet {

	final long[] data;

	public MetalBitSet(long bits) {
		this(new long[(int) Math.ceil((double) bits / Long.SIZE)]);
	}

	/**
	 * Deserialize long array as bitset.
	 * 
	 * @param data
	 */
	public MetalBitSet(long[] data) {
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
	public void set(long index) {
		data[(int) (index >>> 6)] |= (1L << index);
	}

	public boolean getAndSet(int index, boolean next) {
		int i = (int) (index >>> 6);
		int j = (int) (1L << index);
		long[] d = this.data;
		boolean prev = (d[i] & j) != 0;
		if (prev!=next) {
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
	public boolean get(long index) {
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
	public void putAll(MetalBitSet array) {
        assert data.length == array.data.length :
                "BitArrays must be of equal length (" + data.length + "!= " + array.data.length + ')';
        for (int i = 0; i < data.length; i++) {
            data[i] |= array.data[i];
        }
    }


}
