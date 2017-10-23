package il.technion.tinytable;


import il.technion.tinytable.bit.BitwiseArray;
import il.technion.tinytable.bit.Chains;
import il.technion.tinytable.hash.FingerPrint;
import il.technion.tinytable.hash.GreenHasher;
import il.technion.tinytable.hash.RankIndexing;
import org.eclipse.collections.api.list.primitive.IntList;

import java.util.function.Function;

public class TinyTable extends BitwiseArray {

    /** used as an object pool for the rank indexing technique. In order to prevent dynamic memory allocation. */
    private final byte[] offsets;
    private final byte[] chain;

    /** base index array. */
    final long[] I0;
    /** IStar array. */
    final long[] IStar;

    /** anchor distance array. */
    final short[] A;

    private static final int maxAdditionalSize = 0;


    // used for debug - counts how many items in the table.
    //protected int nrItems;

    final transient GreenHasher hash;

    public TinyTable(int itemsize, int bucketcapacity, int nrBuckets) {
        super(bucketcapacity * nrBuckets, itemsize, bucketcapacity);
        //this.maxAdditionalSize = 0;
        //this.nrItems = 0;
        I0 = new long[nrBuckets];
        IStar = new long[nrBuckets];
        A = new short[nrBuckets];
        hash = new GreenHasher(itemsize + maxAdditionalSize, nrBuckets);

        offsets = new byte[64]; //originally these were sized 64 but there must be some relation between the dimensions and these that require them larger
        chain = new byte[64];

    }

    public void add(long item) {
        this.add(hash.hash(item));
    }

    public void add(String item) {
        this.add(hash.hash(item));
    }

    public boolean remove(long i) {
        return this.remove(hash.hash(i));
    }

    public boolean remove(String i) {
        return remove(i.getBytes());
    }

    public <X> boolean remove(X x, Function<X,byte[]> toBytes) {
        return remove(toBytes.apply(x));
    }

    private boolean remove(byte[] i) {
        return remove(hash.hash(i));
    }

    public boolean contains(String item) {
        return this.contains(item.getBytes());
    }
    private boolean contains(byte[] item) {
        return this.contains(hash.hash(item));
    }
    public <X> boolean contains(X item, Function<X,byte[]> toBytes) {
        return this.contains(hash.hash(item, toBytes));
    }

    public boolean contains(long item) {

        return this.contains(hash.hash(item));
    }

    long size(int bucketId, int chainId, long fingerprint) {

        long[] chain = this.getChain(bucketId, chainId);
        return Chains.size(chain, fingerprint, this.itemSize - 1);


    }

    @Override
    public int start(int bucketId) {
        return this.bucketBitSize * bucketId + this.A[bucketId] * this.itemSize;
    }

    @Override
    public int size(int bucketId) {
        return Long.bitCount(this.I0[bucketId]) + Long.bitCount(this.IStar[bucketId]);
    }


    /**
     * Adds a new fingerPrint to the following bucketNumber and chainNumber, the maximal size
     * of supported fingerprint is 64 bits, and it is assumed that the actual data sits on the LSB bits of
     * long.
     * <p>
     * According to our protocol, addition of a fingerprint may result in expending the bucket on account of neighboring buckets,
     * or down sizing the stored fingerprints to make room for the new one.
     * <p>
     * In order to support deletions, deleted items are first logically deleted, and are fully
     * deleted only upon addition.
     *  @param bucketNumber
     * @param chainNumber
     * @param fingerPrint
     */
    int add(FingerPrint fpAux) {
        int nextBucket = this.findFreeBucket(fpAux.bucketId);
        upscaleBuckets(fpAux.bucketId, nextBucket);

        int idxToAdd = RankIndexing.addItem(fpAux, I0, IStar, offsets, chain);
        // if we need to, we steal items from other buckets.
        this.putAndPush(fpAux.bucketId, idxToAdd, fpAux.fingerprint);
        return idxToAdd;
    }


    private boolean remove(FingerPrint fpaux) {
        int m = moveToEnd(fpaux);
        if (m < 0)
            return false;

        this.removeAndShrink(fpaux.bucketId);
        removeItemFromIndex(fpaux);

        int bucket;
        int il = this.I0.length;
        for (int i = fpaux.bucketId + 1; i < fpaux.bucketId + il; i++) {
            bucket = (i) % il;
            if (A[bucket] > 0) {

                removeAndShrink(bucket);
                A[bucket]--;
            } else {

                break;
            }
        }

        return true;
    }

    private int find(FingerPrint fpaux) {
//		List<Integer> chain = RankIndexHashing.getChain(chainNumber, L[bucketNumber], IStar[bucketNumber]);
        for (byte aChain : this.chain) {
            if (aChain < 0)
                break;
            long fpToCompare = this.get(fpaux.bucketId, aChain);
            if (fpToCompare == fpaux.fingerprint)
                return aChain;
        }
        return -1;

    }

    private int moveToEnd(FingerPrint fpaux) {

        int chainoffset = RankIndexing.getChainAndUpdateOffsets(fpaux, I0, IStar, offsets, chain) - 1;
        //		for (Integer itemOffset : chain) {
        //
        //			if(itemOffset<0){
        //				throw new RuntimeException("Item is not there!");
        //			}

        int itemOffset = this.find(fpaux);
        if (itemOffset < 0)
            return -1;
            //throw new RuntimeException("Not found!");

        int lastOffset = chain[chainoffset];
        long lastItem = this.get(fpaux.bucketId, lastOffset);
//		assertTrue(chain.containsitemOffset));
        this.set(fpaux.bucketId, itemOffset, lastItem);
        this.set(fpaux.bucketId, lastOffset, 0L);
        return lastOffset;


    }


    void removeItemFromIndex(FingerPrint fpaux) {
        int chainSize = RankIndexing.getChainAndUpdateOffsets(fpaux, I0, IStar, this.offsets, this.chain, fpaux.chainId) - 1;
        RankIndexing.RemoveItem(fpaux.chainId, I0, IStar, fpaux.bucketId, offsets, chain, chainSize);
    }

    /**
     * finds a the closest bucket that can accept the new item.
     * if the current bucket is under maximal capacity it is the current bucket, otherwise we steal fingerprints from buckets until we reach
     * a free bucket.
     *
     * @param bucketId
     * @return
     */
    private int findFreeBucket(int bucketId) {
        int l = this.A.length;

        int r = l;
        bucketId = bucketId % l;

        while (this.size(bucketId) + this.A[bucketId] >= this.bucketCapacity) {

            bucketId = (++bucketId) % l;

            if (r-- <= 0)
                break;
        }
        return bucketId;
    }

    private void resizeBuckets(int bucketId, boolean IncrementAnchor) {
        if (!IncrementAnchor)
            return;
        this.replaceMany(bucketId, 0, 0L, this.start(bucketId));
        this.A[bucketId]++;
    }


    long[] getChain(int bucketId, int chainId) {
        IntList chain = RankIndexing.getChain(chainId, I0[bucketId], IStar[bucketId], bucketCapacity);
        int s = chain.size();
        long[] result = new long[s];
        for (int i = 0; i < s; i++) {
            int itemOffset = chain.get(i);
            if (itemOffset < 0)
                return null;

            result[i] = this.get(bucketId, itemOffset);
        }
        return result;
    }


    private void upscaleBuckets(int bucketNumber, int lastBucket) {
        //Bucket may be wrapped around too!
        while (lastBucket != bucketNumber) {

            resizeBuckets(lastBucket, true);


            if (--lastBucket < 0) {
                lastBucket = A.length - 1;
            }
        }

    }

    boolean contains(FingerPrint fpaux) {
        RankIndexing.getChainAndUpdateOffsets(fpaux, I0, IStar, offsets, chain, fpaux.chainId);
        return (this.find(fpaux) >= 0);
    }


    /**
     * Put a value at location idx, if the location is taken shift the items to
     * be left until an open space is discovered.
     *
     * @param idx         - index to put in
     * @param value       - value to put in
     * @param mod         - bucket mod, (in order to decode bucket)
     * @param size        - bucket item size. (in order to decode bucket)
     * @param chainNumber
     */
    private void putAndPush(int bucketId, int idx, final long value) {
        this.replaceMany(bucketId, idx, value, this.start(bucketId));
        //this.nrItems++;
    }

    void removeAndShrink(int bucketId) {
        this.replaceBackwards(bucketId, this.start(bucketId));
    }


}
