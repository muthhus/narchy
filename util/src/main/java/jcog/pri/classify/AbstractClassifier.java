package jcog.pri.classify;

import org.roaringbitmap.RoaringBitmap;

public abstract class AbstractClassifier<X, Y> {

    public final Y name;

    public AbstractClassifier(Y name) {
        this.name = name;
    }

    /** dimensionality of this, ie. how many bits it requires */
    abstract public int dimension();

    /** sets the applicable bits between offset and offset+dimensoinality (exclusive) */
    abstract public void classify(X x, RoaringBitmap bmp, int offset);

}
