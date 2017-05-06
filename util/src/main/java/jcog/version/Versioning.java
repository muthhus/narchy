package jcog.version;

import org.eclipse.collections.impl.list.mutable.FastList;
import org.jetbrains.annotations.NotNull;

/** versioning context that holds versioned instances */
public class Versioning extends
        FastList<Versioned> {
        //FasterList<Versioned> {



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


    /** reverts/undo to previous state */
    public final boolean revert(int when) {
        assert(size >= when);

        //pop(size - when );

        while (size > when) {

            Versioned versioned =
                    //removeLast();
                    remove(size-1);

            if (versioned == null) {
                throw new NullPointerException();
                //continue;
            }

            //if (!versioned.isEmpty()) { //HACK wtf would it be empty
                Object removed = versioned.remove(versioned.size()-1); //removeLast();
                if (removed == null) {
                    throw new NullPointerException();
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
    public boolean add(/*@NotNull */Versioned newItem) {
        if (newItem == null)
            throw new NullPointerException();
        return tick() && super.add(newItem);
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
