package jcog.data;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Simple object pool implemented by a Deque (ex: ArrayDeque)
 */
public abstract class DequePool<X> implements Pool<X> {

    protected final Deque<X> data;
    private int capacity;


    public DequePool(int initialCapacity, int preallocate) {
        data = new ArrayDeque(initialCapacity);
        //data = new ConcurrentLinkedDeque<>();

        setCapacity(initialCapacity);
        //data = new CircularArrayList<>(initialCapacity);

        for (int i = 0; i < preallocate; i++)
            put(create());

    }
    public DequePool(int initialCapacity) {
        this(initialCapacity, initialCapacity);
    }

    public void setCapacity(int c) {
        capacity = c;
    }

    @Override
    public void put(X i) {
        //synchronized (data) {


        //if (data.size() < capacity)
            data.offer(i);

        //else: it is forgotten

    }

    @Override
    public final X get() {
        //synchronized (data) {

        Deque<X> d = data;

        if (d.isEmpty()) {
            //System.err.println(this + " emptied, initialize larger or plug leaks");
            return create();
        }
        return d.poll();
        //}
    }

    protected void print() {
        System.out.println(data.size());
    }

    public boolean isEnabled() {
        return capacity != 0;
    }

    @Override
    public void delete() {
        capacity = 0;
        data.clear();
    }
}
