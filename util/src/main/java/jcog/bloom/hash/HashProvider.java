package jcog.bloom.hash;

/**
 * Created by jeff on 14/05/16.
 */
public interface HashProvider<E> {

    int hash1(E element);

    int hash2(E element);

}
