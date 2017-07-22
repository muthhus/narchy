package jcog.version;

import jcog.list.FasterList;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * versioning context that holds versioned instances
 */
public class Versioning<X> extends
        //FastList<Versioned<X>> {
        FasterList<Versioned<X>> {

    public int ttl;

    public Versioning(int capacity, int ttl) {
        super(0, new Versioned[capacity]);
        this.ttl = ttl;
    }

    @NotNull
    @Override
    public String toString() {
        return size() + ":" + super.toString();
    }


    public final boolean revertAndContinue(int to) {
        revert(to);
        return live();
    }

    /**
     * reverts/undo to previous state
     */
    public final void revert(int when) {
        //assert (size >= when);

        //pop(size - when );

        int s = size;
        int c = s - when;

        while (c-- > 0) {

            //Versioned versioned =
            //removeLast();

            Versioned versioned = items[--size]; //pop()
            items[size] = null;


            versioned.pop();

            //}
            //assert(removed!=null);
            //TODO removeLastFast where we dont need the returned value
        }

    }


    @Override
    public void clear() {
        revert(0);
    }

    @Override
    public final boolean add(@NotNull Versioned<X> newItem) {
        Versioned<X>[] ii = this.items;
        if (tick() && ii.length < this.size) {
            ii[this.size++] = newItem; //cap
            return true;
        }
        return false;
    }

    @Override
    public void add(int index, Versioned<X> element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends Versioned<X>> source) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(int index, Collection<? extends Versioned<X>> source) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAllIterable(Iterable<? extends Versioned<X>> iterable) {
        throw new UnsupportedOperationException();
    }

    public final void stop() {
        setTTL(0);
    }

    public final boolean tick() {
//        if (ttl-- == 0) {
//            onDeath(); //transition from live to death occurred
//        }

        return ttl-- > 0;

    }

//    /**
//     * empty for subclass impl
//     */
//    public void onDeath() {
//
//    }

    /**
     * whether the unifier should continue: if TTL is non-zero.
     */
    public final boolean live() {
        return ttl > 0;
    }

    public final void setTTL(int ttl) {
        this.ttl = ttl;
    }
}
