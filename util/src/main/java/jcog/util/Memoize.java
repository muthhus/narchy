package jcog.util;

import java.util.function.Function;

public interface Memoize<K, V> extends Function<K, V> {

    String summary();

    void clear();

}
