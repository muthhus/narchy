package jcog.pri.mix;

import jcog.pri.Priority;

import java.util.function.Consumer;

public interface PSinks<X extends Priority, Y extends Priority> {
    PSink<X> newStream(Object streamID, Consumer<Y> each);
}
