package il.technion.tinytable.hash;


public class FingerPrint {
    public final int bucketId;
    public final int chainId;
    public long fingerprint;

    public FingerPrint(int bucketid, int chainid, long fp) {
        this.bucketId = bucketid;
        this.chainId = chainid;
        this.fingerprint = fp;

    }

    //Finger prints are only eaual if they differ at most at the first bit
    // the first bit is used as an index.
    static boolean Equals(long $1, long $2) {
//		$1 = $1^$2;
        return (($1 ^ $2) < 2L);
    }

    // the item is last in chain iff it is marked by 1 on the LSB.
    static boolean isLast(long fingerPrint) {

        return ((fingerPrint & 1) == 1);

    }

    // marks an item as last.
    public static long setLast(long fingerPrint) {
        return fingerPrint | 1;
    }

    public String toString() {
        return ("BucketID: " + bucketId + " chainID:" + chainId + " fingerprint: " + fingerprint);
    }


}
