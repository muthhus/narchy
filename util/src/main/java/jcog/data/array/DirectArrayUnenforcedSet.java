package jcog.data.array;

import jcog.list.ArrayUnenforcedSet;
import jcog.data.sorted.AbstractSet;
import org.apache.commons.collections4.iterators.ArrayIterator;
import org.apache.commons.lang3.ArrayUtils;

import java.util.*;
import java.util.Arrays;

/**
 * wraps an array which you promise is duplicate free and/or sorted
 */
public class DirectArrayUnenforcedSet<X> extends AbstractSet<X>  {
    private final X[] x;

    /**
     * Constructs a new empty set
     */
    public DirectArrayUnenforcedSet(X... x)  {
        this.x = x;
    }

    @Override
    public boolean contains(Object o) {
        return ArrayUtils.contains(x, o);
    }

    @Override
    public int hashCode()    {
        return Arrays.hashCode(x);
    }

    @Override
    public Iterator<X> iterator() {
        return new ArrayIterator<X>(x);
    }

    @Override
    public int size() {
        return x.length;
    }

}