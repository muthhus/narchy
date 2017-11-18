package jcog.data.map;

import java.util.function.Function;

/** minimal metadata map interface */
public interface MetaMap {
    /** Map.get */
    <X> X meta(String key);

    /** Map.put */
    void meta(String key, Object value);

    /** Map.computeIfAbsent */
    <X> X meta(String key, Function<String, Object> valueIfAbsent);
}
