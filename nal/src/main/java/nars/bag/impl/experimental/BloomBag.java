package nars.bag.impl.experimental;

import il.technion.tinytable.TinyCountingTable;
import nars.$;

import java.util.List;
import java.util.function.Function;

/**
 * Not complete yet
 */
public class BloomBag<X> {
    final TinyCountingTable pri;
    final List<X> items;
    private final Function<X, byte[]> toBytes;
    private final int capacity;

    public BloomBag(int capacity, Function<X, byte[]> toBytes) {
        int itemSize = 4;
        int bucketCapacity = capacity;
        int numBuckets = capacity/2;
        this.pri = new TinyCountingTable(itemSize, bucketCapacity, numBuckets);
        this.items = $.newArrayList(capacity);
        this.toBytes = toBytes;
        this.capacity = capacity;
    }

    public long pri(X b) {
        return pri.get(toBytes.apply(b));
    }

//    public void set(X b, long n) {
//        pri.set(b, toBytes, n);
//    }

    public void add(X b, long n) {
        byte[] key = toBytes.apply(b);
        long prev = pri.get(key);
        long next = prev + n;
        pri.set(key, next);
        if (prev == 0) {
            items.add(b);
            onAdd(b, next);
        } else {
            onChange(b, prev, next);
        }
    }

    private void onAdd(X b, long next) {
        System.out.println("add: " + b + " " + next + "\t(" + items.size() + ")");
    }
    private void onChange(X b, long prev, long next) {
        System.out.println("chg: " + b + " " + next + "\t(" + items.size() + ")");
    }
    private void onRemove(X b, long value) {
        System.out.println("rem: " + b + " " + value + "\t(" + items.size() + ")");
    }

    public void commit() {
        while (items.size() > capacity) {
            removeWeakest();
        }
//        Iterator<X> ii = items.iterator();
//        while (ii.hasNext()) {
//            X v = ii.next();
//            if (pri.get(toBytes.apply(v)) == 0) {
//                ii.remove();
//                onRemove(v);
//            }
//        }
    }

    private void removeWeakest() {
        int s = items.size();
        if (s == 0)
            return;

        long min = Long.MAX_VALUE;
        int whichMin = -1;

        for (int i = 0, itemsSize = s; i < itemsSize; i++) {
            X x = items.get(i);
            long p = pri(x);
            if (p < min) {
                min = p;
                whichMin = i;
            }
        }


        X removed = items.remove(whichMin);
        pri.remove(removed, toBytes);
        onRemove(removed, min);

    }

    public static void main(String[] args) {
        int unique = 16;
        int capacity = 4;
        BloomBag<String> b = new BloomBag<String>(capacity, x -> x.getBytes());
        for (int i = 0; i < 1000; i++) {
            b.add("x" + (int)(Math.random() * unique), 1 );
            b.commit();
        }
    }

}
