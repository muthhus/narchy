package jcog.list;


import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.IntFunction;

/**
 * Thread safe list which produces arrays for fast iteration
 * these arrays are like copy-on-write-array-list except
 * are reusable and null-terminated (and padded). so if the size shrinks,
 * it does not need to reallocate.
 *
 * null padding is applied to assist garbage collection fully
 *
 * unless the synchronized methods are used,
 * udpates may contain inconsistent data.
 *
 * use C[] nullTerminatedArray() to access this array, don't
 * change it without a good reason (it will be shared), and
 * iterate it in sequence and stop at the first null (this is the
 * end).
 */
public class ArraySharingList<C> extends FasterList<C> {

    private final IntFunction<C[]> arrayBuilder;
    protected transient C[] array;
    private final transient AtomicBoolean change = new AtomicBoolean(true);

    public ArraySharingList(IntFunction<C[]> arrayBuilder) {
        this.arrayBuilder = arrayBuilder;
    }

//    @Override
//    public void writeExternal(ObjectOutput objectOutput) throws IOException {
//        objectOutput.writeInt(size());
//        this.forEach(t -> {
//            try {
//                objectOutput.writeObject(t);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        });
//    }
//
//    @Override
//    public void readExternal(ObjectInput objectInput) throws IOException, ClassNotFoundException {
//        int num = objectInput.readInt();
//        for (int i = 0; i < num; i++)
//            add((C) objectInput.readObject());
//    }

    @Override
    public final boolean add(C x) {
        if (super.add(x)) {
            change.set(true);
            return true;
        }
        return false;
    }



    @Override
    public final void add(int index, C element) {
        super.add(index, element);
        change.set(true);
    }

    @Override
    public boolean addAll(int index, Collection<? extends C> source) {
        if (super.addAll(index, source)) {
            change.set(true);
            return true;
        }
        return false;
    }


    @Override
    public final C remove(int index) {
        C removed = super.remove(index);
        if (removed!=null) {
            change.set(true);
        }
        return removed;
    }

    @Override
    public final boolean remove(Object x) {
        if (super.remove(x)) {
            change.set(true);
            return true;
        }
        return false;
    }

    @Override
    public final boolean isEmpty() {
        C[] cc = getCachedNullTerminatedArray();
        return cc == null || cc[0] == null;
    }

    @Override
    public final void clear() {
        if (!isEmpty()) {
            super.clear();
            change.set(true);
        } else {
            return;
        }
    }

    @Override
    public final int size() {
        C[] a = getCachedNullTerminatedArray();
        return a == null ? 0 : a.length - 1;
    }

    /** may be null; ignore its size, it will be at least 1 element larger than the size of the list */
    public final C[] getCachedNullTerminatedArray() {
        return (change.compareAndSet(true,false)) ?
            updateArray() : this.array;
    }

    /** for thread-safe mode */
    public final synchronized C[] getCachedNullTerminatedArraySynch() {
        return getCachedNullTerminatedArray();
    }

    private C[] updateArray() {

        //TODO for safe atomicity while the events are populated, buffer additions to a sub-list,
        //and apply them if a flag is set on the next read



        C[] a;

        int s = super.size();
        if (s > 0) {
            if ((a = array) == null)
                a = arrayBuilder.apply(s+1);  //+1 for padding
            a = fillArrayNullPadded(a);
        }
        else {
            a = null;
        }

        return this.array = a;
    }

    @Override
    public void forEach(Consumer with) {
        forEach(with, -1);
    }

    private void forEach(Consumer with, int max) {
        C[] a = getCachedNullTerminatedArray();
        if (a == null) return;
        if (max == -1) max = a.length;
        for (int i = 0; i < max; i++) {
            C c = a[i];
            if (c != null) {
                with.accept(c);
            } else {
                break;
            }
        }
    }

    /** not recommended to use this since it involves instantiating an iterator */
    @Override public Iterator<C> iterator() {
        C[] a = getCachedNullTerminatedArray();
        if (a == null) return Collections.emptyIterator();

        return new Iterator<>() {

            public C next;
            final C[] array = a;

            int i;

            @Override
            public boolean hasNext() {
                return (next = array[i]) != null;
            }

            @Override
            public C next() {
                i++;
                return next;
            }
        };
    }
}
