package jcog.table;

import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/** nearly a Map */
public interface Table<K,V> extends Iterable<V> {

    void clear();

    @Nullable
    V get(/*@NotNull*/ Object key);

    @Nullable
    Object remove(/*@NotNull*/ K key);

    int size();


//    void setOnRemoval(Consumer<V> onRemoval);
//    Consumer<V> getOnRemoval();

    default void delete() {

    }

    /** iterates in sorted order */
    void forEachKey(/*@NotNull*/ Consumer<? super K> each);

    int capacity();

    void setCapacity(int i);



    default boolean isFull() {
        return size() >= capacity();
    }


//    default void top(/*@NotNull*/ Consumer<V> each) {
//        topWhile(e -> {
//            each.accept(e);
//            return true;
//        });
//    }



}
