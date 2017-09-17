package jcog.bloom.hash;

import java.util.function.Function;

public class BytesHashProvider<E> extends AbstractHashProvider<E> {

    final Function<E,byte[]> bytes;

    public BytesHashProvider(Function<E, byte[]> bytes) {
        this.bytes = bytes;
    }

    @Override
    public byte[] asBytes(E element) {
        return bytes.apply(element);
    }
}
