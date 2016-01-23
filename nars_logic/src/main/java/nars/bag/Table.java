package nars.bag;

import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;

/** nearly a Map */
public interface Table<K,V> extends Iterable<V> {

    void clear();

    @Nullable
    V get(K key);

    @Nullable
    Object remove(K key);

    /** same semantics as Map.put; output value is an existing value or null if none */
    @Nullable
    V put(K k, V v);


    int size();

    default boolean isEmpty() {
        return size() == 0;
    }

//    void setOnRemoval(Consumer<V> onRemoval);
//    Consumer<V> getOnRemoval();

    default void delete() {

    }

    /** iterates in sorted order */
    void forEachKey(Consumer<? super K> each);

//    default void top(@NotNull Consumer<V> each) {
//        topWhile(e -> {
//            each.accept(e);
//            return true;
//        });
//    }

    /**
     * if predicate evaluates false, it terminates the iteration
     */
    void topWhile(Predicate<V> each);



}
