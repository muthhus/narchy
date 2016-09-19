package il.technion.tinytable.hash;

import il.technion.tinytable.hash.MurmurHash.ModifiedMurmurHash;

import java.util.function.Function;


public class GreenHasher {

    //currently chain is bounded to be 64.

    private final static long Seed64 = 0xe17a1465;
    private final static long m = 0xc6a4a7935bd1e995L;
    private final static int r = 47;
    private static final long chainMask = 63L;

    private final int fpSize;
    private final long fpMask;
    private final int bucketRange;

    public GreenHasher(int fingerprintsize, int bucketrange, int chainrange) {
        this.fpSize = fingerprintsize;
        // finger print cannot be zero so you must choose a finger-print size greater than zero.
        assert (fpSize > 0);
        this.bucketRange = bucketrange;
        fpMask = (1L << fpSize) - 1;
        //fpaux = new FingerPrint(0, 0, 0);
    }


    public FingerPrint hash(String item) {
        return hash(item.getBytes());
    }
    public <X> FingerPrint hash(X x, Function<X,byte[]> bytes) {
        return hash(bytes.apply(x));
    }

    public FingerPrint hash(byte[] data) {
        long hash = ModifiedMurmurHash.hash64(data, data.length);
//		long hash = item.hashCode()^0xe17a1465;
//		hash ^= (hash >>> 20) ^ (hash >>> 12);
//		 hash ^= (hash >>> 7) ^ (hash >>> 4);

        long fp = hash & fpMask;
        if (fp == 0L)
            fp = 1L;  //avoid 0

        hash >>>= fpSize;
        int chainId = (int) (hash & chainMask);
        hash >>>= 6;
        int bucketId = (int) ((hash & Long.MAX_VALUE) % bucketRange);

        return new FingerPrint(bucketId, chainId, fp);
    }


    public FingerPrint hash(long item) {

        item *= m;
        item ^= item >>> r;
        item *= m;

        long h = (Seed64) ^ (m);
        h ^= item;
        h *= m;

        long fp = (h & fpMask);
        if (fp == 0L)
            fp = 1L; //avoid 0

        h >>>= fpSize;
        int chainId = (int) (h & chainMask);
        h >>>= 6;
        int bucketId = (int) ((h & Long.MAX_VALUE) % bucketRange);

        return new FingerPrint(bucketId, chainId, fp);

    }


    //	public  FingerPrintAux createHash(final byte[] data) {
    //
    //		long hash =  MurmurHashTinyTable.hash64(data, data.length,0xe17a1465);
    //
    //		fpaux.fingerprint = hash&fpMask;
    //		if(fpaux.fingerprint ==0l)
    //		{
    //			fpaux.fingerprint++;
    //		}
    //
    //
    //
    //		hash>>>=fpSize;
    //		fpaux.chainId = (int) (hash&chainMask);
    //		hash>>>=6;
    //		fpaux.bucketId =  (int) ((hash&Long.MAX_VALUE)%bucketRange);
    //
    //		return fpaux;
    //
    //	}


}
