package jcog.pri.classify;

import org.roaringbitmap.RoaringBitmap;

import java.util.function.ToIntFunction;
import java.util.stream.IntStream;

public class EnumClassifier<Y, X> extends AbstractClassifier<X> {

    //private final String[] labels;
    private final ToIntFunction<X> which;
    private final int dim;
    private final String[] names;

    public EnumClassifier(Y id, int n, ToIntFunction<X> which) {
        this(id, IntStream.range(0, n).mapToObj(String::valueOf).toArray(String[]::new), which);
    }

    public EnumClassifier(Y id, String[] names, ToIntFunction<X> which) {
        super(id);
        this.dim = names.length;
        this.names = names;
        this.which = which;
    }

    @Override
    public String name(int i) {
        return names[i];
    }

    @Override
    public int dimension() {
        return dim;
    }

    @Override
    public void classify(X x, RoaringBitmap bmp, int offset) {
        int w = which.applyAsInt(x);
        if (w >= 0) {
            assert (w < dim);
            bmp.add(offset + w);
        }
    }

}
