package jcog.pri.classify;

import org.roaringbitmap.RoaringBitmap;

import java.util.function.ToIntFunction;

public class EnumClassifier<Y, X> extends AbstractClassifier<X,Y> {

    //private final String[] labels;
    private final ToIntFunction<X> which;
    private final int dim;

    public EnumClassifier(Y id, int size, ToIntFunction<X> which) {
        super(id);
        this.dim = size;
        this.which = which;
    }

    @Override
    public int dimension() {
        return dim;
    }

    @Override
    public void classify(X x, RoaringBitmap bmp, int offset) {
        int w = which.applyAsInt(x);
        if (w > 0) {
            assert (w < dim);
            bmp.add(offset + w);
        }
    }

}
