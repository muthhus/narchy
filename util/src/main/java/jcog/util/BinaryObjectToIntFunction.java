package jcog.util;

public interface BinaryObjectToIntFunction<X,Y> {
    int intValueOf(X x, Y y);
}
