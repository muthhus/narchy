package jcog.pri.mix;

import jcog.pri.Priority;

public interface PSinks<X, Y extends Priority> {
    PSink<X, Y> stream(X x);
}
