package jcog.version;

import jcog.list.FasterList;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * versioning context that holds versioned instances
 */
public class Versioning<X> extends
        //FastList<Versioned> {
        FasterList<Versioned<X>> {



    public Versioning(int capacity) {
        super(0, new Versioned[capacity]);
    }

    @NotNull
    @Override
    public String toString() {
        return size() + ":" + super.toString();
    }


    /**
     * reverts/undo to previous state
     */
    public final void revert(int when) {

        int s = size();
        int c = s - when;

        while (c-- > 0) {
            Versioned versioned = items[--size];
            items[size] = null; //GC help

            versioned.pop();
        }
    }

    public final void revert(int when, Consumer<X> each) {

        int s = size();
        int c = s - when;

        while (c-- > 0) {
            Versioned<X> versioned = items[--size];
            items[size] = null; //GC help

            each.accept( versioned.getAndPop() );
        }
    }

    @Override
    public void clear() {
        revert(0);
    }


    @Override
    public void add(int index, Versioned element) {
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

    //    @Override
//    public final boolean add(@NotNull Versioned newItem) {
//        Versioned[] ii = this.items;
//        if (ii.length == this.size) {
//            return false;
//        } else {
//            if (!tick())
//                return false;
//
//            assert(this.size <= ii.length);
//            ii[this.size++] = newItem;
//            return true;
//        }
//    }


}
