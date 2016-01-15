package nars.bag.impl;

import java.util.function.Consumer;
import java.util.function.Predicate;

/** nearly a Map */
public interface Table<K,V>  {

    void clear();

    V get(Object key);

    Object remove(K key);

    /** same semantics as Map.put; output value is an existing value or null if none */
    V put(K k, V v);


    int size();

    default boolean isEmpty() {
        return size() == 0;
    }

//    void setOnRemoval(Consumer<V> onRemoval);
//    Consumer<V> getOnRemoval();

    default void delete() {

    }

    default void top(Consumer<V> each) {
        topWhile(e -> {
            each.accept(e);
            return true;
        });
    }

    /**
     * if predicate evaluates false, it terminates the iteration
     */
    void topWhile(Predicate<V> each);

    //TODO provide default impl
    void topN(int limit, Consumer<V> each);


}
