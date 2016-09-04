package il.technion.tinytable.bit;


public class BitwiseArray extends AuxilaryBitSet {

    public final int bucketCapacity;
    protected int itemSize;
    protected int bucketBitSize;
    protected int maxAdditionalSize = 15;

    /**
     * Represent an array with bit elements. Implemented over the AuxilaryBitSet, its job is to calculate the appropriate bit offsets in order
     * to produce the abstraction of an array.
     *
     * @param capacity
     * @param itemSize
     * @param bucketcapacity
     */
    public BitwiseArray(final int capacity, final int itemSize, int bucketcapacity) {
        super((capacity * itemSize) / 64 + 1);
        this.itemSize = itemSize;
        this.bucketCapacity = bucketcapacity;
        this.bucketBitSize = bucketcapacity * itemSize;
    }

    public long FastGet(int bucketStart, int idx) {
        int start = bucketStart + idx * this.itemSize;
        return super.getBits(start, start + this.itemSize);

    }

    public long FastReplace(int bucketStart, int idx, long value) {
        int start = bucketStart + idx * this.itemSize;
        return super.replaceBits(start, start + this.itemSize, value);

    }

    /**
     * Retrieve the element at index idx. The array calculates the required
     * offset and length of item.
     *
     * @param idx - logical array index.
     * @return the item (bits of length fingerSize) stored at that index.
     */
    public long get(int bucketId, int idx) {
        return this.get(bucketId, idx, this.itemSize, 0);
    }

    /**
     * Get the value according to predefined idx, custom size, and mod.
     *
     * @param idx
     * @param customSize
     * @param mod
     * @return
     */
    protected long get(int bucketId, int idx, int customSize, int mod) {
        int effectiveItemSize = customSize;
        int bucketStart = this.start(bucketId);
        int start = bucketStart + idx * customSize;
        return super.getBits(start, start + effectiveItemSize);
    }

    protected long replace(int bucketId, int idx, int customSize, long value) {
        int effectiveItemSize = customSize;
        int bucketStart = this.start(bucketId);
        int start = bucketStart + idx * customSize;
        int end = start + effectiveItemSize;
        long res = super.getBits(start, end);
        super.setBits(start, end, value);
        return res;
    }

    protected void replaceBackwards(final int bucketStart, final int maxToShift, long value) {
        int start = bucketStart + maxToShift * this.itemSize;
        int end = start + this.itemSize;
        do {
            value = super.replaceBits(start, end, value);
            start -= this.itemSize;
            end -= this.itemSize;
        }
        while (value != 0L);

    }

    /**
     * bubble items in buckets backwords until reaching 0.
     *
     * @param bucketId
     * @param bucketStart
     */
    protected void replaceBackwards(int bucketId, int bucketStart) {
        // optimization only for my functionality I already know nrItems!
        int end = bucketStart + (this.size(bucketId)) * this.itemSize;
        int start = end - this.itemSize;
        long value = 0L;
        do {
            long res = super.replaceBits(start, end, value);
            start -= this.itemSize;
            end -= this.itemSize;
            value = res;
        }
        while (value != 0L);

    }

    /**
     * bubbles the items until value is at end of bucket.
     *
     * @param bucketId
     * @param idx
     * @param value
     * @param bucketStart
     */
    protected void replaceMany(int bucketId, int idx, long value, int bucketStart) {

        int start = bucketStart + idx * this.itemSize;
        int end = start + this.itemSize;
        do {
            value = super.replaceBits(start, end, value);
            start += this.itemSize;
            end += this.itemSize;
        } while (value != 0L);
        return;
    }

    protected void shrinkBucket(int bucketId, int idx, int customSize, long value) {
        int effectiveItemSize = customSize;
        int bucketStart = start(bucketId);
        int start = bucketStart + idx * customSize;
        int end = start + effectiveItemSize;
        super.setBits(start, end, 0);

        int bucketEnd = bucketStart + (this.size(bucketId) - 1) * this.itemSize;
        value = 0L;
        do {
            long res = super.getBits(bucketEnd, bucketEnd + this.itemSize);
            super.setBits(bucketEnd, bucketEnd + this.itemSize, value);
//			System.out.println("replaced value: "+ value + " res: "+res);
            bucketEnd -= customSize;
            value = res;
        }
        while (value != 0L);


    }


    /**
     * You must override this method, for it to work.
     *
     * @param bucketID
     * @return
     */
    public int size(int bucketID) {
        return 0;
    }

    /**
     * You must override this method to use anchors, for it to work.
     *
     * @param bucketID
     * @return
     */
    public int start(int bucketId) {
        return this.bucketBitSize * bucketId;
    }


    /**
     * Put an item according to predefined idx, customSize and mod.
     *
     * @param idx
     * @param value
     * @param customSize
     * @param mod        TODO: get and put should consider Anchors (rather than shrink it all).
     */

    private void set(int bucketId, int idx, long value, int customSize, int mod) {
        int bucketStart = start(bucketId);
        int start = bucketStart + idx * customSize;
        super.setBits(start, start + customSize, value);
    }

    public void fastSet(int bucketStart, int idx, long value) {
        int s = this.itemSize;
        int start = bucketStart + idx * s;
        super.setBits(start, start + s, value);
    }


    /**
     * puts a value at location idx. The previous item at location idx is
     * erased.
     *
     * @param idx
     * @param value
     */
    public void set(int bucketId, int idx, final long value) {
        this.set(bucketId, idx, value, this.itemSize, 0);
    }


}
