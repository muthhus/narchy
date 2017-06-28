package jcog.byt;

/**
 * caches the hashcode when compact() is called
 */
public class HashCachedDynByteSeq extends DynByteSeq {

    private int hash;

    public HashCachedDynByteSeq(int bufferSize) {
        super(bufferSize);
        compact(); //initialize
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof DynByteSeq)) return false;
        if (hash != obj.hashCode()) return false;
        return super.equals(obj);
    }

    /** must be called after construction */
    @Override public void compact() {
        super.compact();
        hash = super.hashCode();
    }

}
