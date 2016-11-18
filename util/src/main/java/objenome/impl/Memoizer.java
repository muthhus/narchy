package objenome.impl;

/**
 * Represents an operation whose results are memoized. Results returned by invocations of
 * {@link #create(Object)} are memoized so that the same object is returned for multiple invocations
 * of {@link #get(Object)} for the same key.
 */
public interface Memoizer<K,V> {

    V get(K key);

    V create(K key);

}
