package jcog.version;

import jcog.list.FasterList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** versioning context that holds versioned instances */
public class Versioning extends FasterList<Versioned> {


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

        pop(size - when );

        return live();
    }

    public final boolean live() {
        return ttl > 0;
    }

    public final void pop(int count) {
        for (int i = 0; i < count; i++) {
            Versioned versioned = removeLast();
            if (versioned == null) {
                throw new NullPointerException();
            }

            Object removed = versioned.removeLast();

            assert(removed!=null);
            //TODO removeLastFast where we dont need the returned value
        }
    }

    @Override
    public void clear() {
        revert(0);
    }

    @Override
    public final boolean add(@NotNull Versioned newItem) {
        Versioned[] ii = this.items;
        if (ii.length == this.size) {
            return false;
        } else {
            if (!tick())
                return false;

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
