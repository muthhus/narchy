package jcog.version;

import jcog.list.FasterList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** versioning context that holds versioned instances */
public class Versioning extends FasterList<Versioned> {


    private int ttl;

    public Versioning(int capacity, int ttl) {
        super(0, new Versioned[capacity]);
        this.ttl = ttl;
    }

    @NotNull
    @Override
    public String toString() {
        return size() + ":" + super.toString();
    }

    /** start a new version with a commit, returns true if add was successful or false if unsuccessful (capacity exceeded)
     *  @return null if capacity exceeded
     * */
    public final boolean nextChange(@Nullable Versioned v, @Nullable Object x) {
        return add(v) && !(v != null && !v.add(x));
    }

    public final boolean nextChange() {
        return nextChange(null, null);
    }


    /** reverts/undo to previous state */
    public final void revert(int when) {
        pop(size - when );
    }

    public final void pop(int count) {
        for (int i = 0; i < count; i++) {
            Versioned versioned = removeLast();
            if (versioned!=null)
                versioned.removeLast();
        }
    }

    @Override
    public void clear() {
        revert(0);
    }

    @Override
    public final boolean add(@Nullable Versioned newItem) {
        if (!tick())
            return false;

        Versioned[] ii = this.items;
        if (ii.length == this.size) {
            return false;
        } else {
            ii[this.size++] = newItem;
            return true;
        }
    }

    public boolean tick() {
        return --ttl > 0;
    }

    public void setTTL(int ttl) {
        this.ttl = ttl;
    }
}
