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


    /**
     * reverts/undo to previous state
     */
    public final boolean revert(int when) {
        //assert (size >= when);

        //pop(size - when );

        int s = size();
        int c = s - when;

        while (c-- > 0) {

            Versioned versioned =
                    removeLast();
                    //remove(--s);

            if (versioned == null) {
                throw new NullPointerException();
                //continue;
            }

            //if (!versioned.isEmpty()) { //HACK wtf would it be empty
            int vs = versioned.size();
            if (vs > 0) { //HACK wtf would it be empty
                Object removed =
                    // versioned.remove(vs - 1);
                    versioned.removeLast();

                if (removed == null) {
                    throw new NullPointerException();
                }
            }

            //}
            //assert(removed!=null);
            //TODO removeLastFast where we dont need the returned value
        }
        return live();
    }


    @Override
    public void clear() {
        revert(0);
    }

    @Override
    public boolean add(@NotNull Versioned newItem) {
        return tick() && super.add(newItem);
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

    public final boolean tick() {
        if (ttl <= 0)
            return false;
        else {
            --ttl;
            return true;
        }
    }

    public final boolean live() {
        return ttl > 0;
    }

    public final void setTTL(int ttl) {
        this.ttl = ttl;
    }
}
