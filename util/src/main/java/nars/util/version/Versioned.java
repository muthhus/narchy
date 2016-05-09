package nars.util.version;

import nars.util.data.list.FasterIntArrayList;
import nars.util.data.list.FasterList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Maintains a versioned snapshot history (stack) of a changing value
 */
public class Versioned<X> extends FasterIntArrayList /*Comparable<Versioned>*/ {

    public final FasterList<X> value;
    @NotNull
    private final Versioning context;

    @Nullable X current;

    /**
     * id, unique within the context this has registered with
     */
    private final int id;

    public Versioned(@NotNull Versioning context) {
        this(context, context.newIntStack(), context.newValueStack());
    }

    public Versioned(@NotNull Versioning context, int[] buffer, FasterList<X> value) {
        super(buffer);
        this.context = context;
        this.value = value;
        id = context.track();
    }

    /** called when this versioned is removed/Deleted from a context */
    void delete() {
        clear();
        current = null;
        context.onDeleted(this);
    }

    @Override
    public final boolean equals(Object otherVersioned) {
        return this == otherVersioned;
    }

    @Override
    public final int hashCode() {
        return id;
    }


    boolean revertNext(int before) {
        int p = size - 1;
        if (p >= 0) {
            int[] a = items;
            if (a[p--] > before) {
                popTo(p);
                value.popTo(p);
                this.current = getUncached();
                return true;
            }
        }
        return false;
    }


    public int lastUpdatedAt() {
        int s = size();
        if (s == 0) return -1;
        return get(s-1);
    }

    /*@Override
    public int compareTo(Versioned o) {
        return Integer.compare(o.now(), now());
    }*/

    /**
     * gets the latest value
     */
    @Nullable
    public final X get() {
        return current;
    }

    private final X getUncached() {
        int s = size();
        if (s == 0) return null;
        return value.get(s-1);
    }

//    /**
//     * gets the latest value at a specific time, rolling back as necessary
//     */
//    public X revertThenGet(int now) {
//        revert(now);
//        return latest();
//    }

    /**
     * sets thens commits
     */
    @NotNull
    public Versioned set(X nextValue) {
        if (this.current!=nextValue) {
            set(context.newChange(this), this.current = nextValue);
        }
        return this;
    }

    /**
     * set but does not commit;
     * a commit should precede this call otherwise it will have the version of a previous commit
     */
    @NotNull
    public void thenSet(X nextValue) {
        if (this.current!=nextValue) {
            set(context.continueChange(this), this.current = nextValue);
        }
    }

    /**
     * sets at a specific time but does not commit;
     * make sure to call commit on the returned context after
     * all concurrent set() are finished
     */
    @NotNull
    final void set(int now, X nextValue) {
        add(now);
        value.add(nextValue);
    }


    @Override
    public void clear() {
        //super.clear();
        super.clearFast();

        //value.clearFast();
        value.clear();

        this.current = null;
    }

    @Override
    public final String toString() {
        X v = get();
        if (v != null)
            return v.toString();
        return "null";
    }

    public final String toStackString() {
        StringBuilder sb = new StringBuilder("(");
        int s = size();
        for (int i = 0; i < s; i++) {
            //sb.append('(');
            sb.append(get(i)).append(':').append(value.get(i));
            //sb.append(')');
            if (i < s - 1)
                sb.append(", ");
        }
        return sb.append(')').toString();

    }

//    public int setInt(IntToIntFunction f) {
//        Integer x = (Integer) get();
//        Integer y = f.valueOf(x);
//        set(y);
//    }

//    @Nullable
//    public X getIfAbsent(X valueIfMissing) {
//        X x = get();
//        if (x == null) return valueIfMissing;
//        return x;
//    }
//
////    public long getIfAbsent(long valueIfMissing) {
////        if (isEmpty()) return valueIfMissing;
////        return ((Long) get());
////    }
//
//    @Deprecated public int getIfAbsent(int valueIfMissing) {
//        Integer i  = (Integer) get();
//        return i == null ? valueIfMissing : i;
//    }

//    public char getIfAbsent(char valueIfMissing) {
//        if (isEmpty()) return valueIfMissing;
//        return ((Character) get());
//    }

}
