package jcog.util;

@FunctionalInterface public interface ObjectObjectToIntFunction<X,Y> {
    int intValueOf(X x, Y y);
}
