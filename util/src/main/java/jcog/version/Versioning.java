package jcog.version;

import jcog.list.FasterList;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * versioning context that holds versioned instances
 */
public class Versioning extends
        //FastList<Versioned> {
        FasterList<Versioned> {



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
        //assert (size >= when);

        //pop(size - when );

        int s = size();
        int c = s - when;

        while (c-- > 0) {

            //Versioned versioned =
                    //removeLast();

            Versioned versioned = items[--size];
            items[size] = null; //GC help



//            if (versioned == null) {
//                throw new NullPointerException();
//                //continue;
//            }

            //if (!versioned.isEmpty()) { //HACK wtf would it be empty

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
    public void add(int index, Versioned element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends Versioned> source) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(int index, Collection<? extends Versioned> source) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAllIterable(Iterable<? extends Versioned> iterable) {
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
