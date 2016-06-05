package nars.bag;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;

/** nearly a Map */
public interface Table<K,V> extends Iterable<V> {



    void clear();

    @Nullable
    V get(@NotNull Object key);

    @Nullable
    Object remove(@NotNull K key);

    /** same semantics as Map.put; output value is an existing value or null if none */
    @Nullable
    V put(@NotNull K k, @NotNull V v);

    int size();


//    void setOnRemoval(Consumer<V> onRemoval);
//    Consumer<V> getOnRemoval();

    default void delete() {

    }

    /** iterates in sorted order */
    void forEachKey(@NotNull Consumer<? super K> each);

    int capacity();

    void setCapacity(int i);

    default boolean isEmpty() {
        return size() == 0;
    }

    default boolean isFull() {
        return size() >= capacity();
    }


//    default void top(@NotNull Consumer<V> each) {
//        topWhile(e -> {
//            each.accept(e);
//            return true;
//        });
//    }

    /**
     * if predicate evaluates false, it terminates the iteration
     */
    void topWhile(@NotNull Predicate<V> each);


}
