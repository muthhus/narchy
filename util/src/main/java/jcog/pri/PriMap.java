/*
 * Copyright (c) 2017 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jcog.pri;

import org.eclipse.collections.api.block.function.primitive.ShortToShortFunction;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectFloatProcedure;
import org.eclipse.collections.api.map.primitive.ObjectShortMap;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;

import static jcog.Util.unitize;

/**
 * This file was automatically generated from template file objectPrimitiveHashMap.stg.
 *
 * @since 3.0.
 */
public class PriMap<K>  {
    public static final short EMPTY_VALUE = (short) -1;

    private static final long serialVersionUID = 1L;
    private static final int OCCUPIED_DATA_RATIO = 2;
    private static final int OCCUPIED_SENTINEL_RATIO = 4;
    private static final int DEFAULT_INITIAL_CAPACITY = 8;

    private static final Object NULL_KEY = new Object() {
        
        public boolean equals(Object obj) {
            throw new RuntimeException("Possible corruption through unsynchronized concurrent modification.");
        }

        
        public int hashCode() {
            throw new RuntimeException("Possible corruption through unsynchronized concurrent modification.");
        }

        
        public String toString() {
            return "ObjectShortHashMap.NULL_KEY";
        }
    };

    private static final Object REMOVED_KEY = new Object() {
        
        public boolean equals(Object obj) {
            throw new RuntimeException("Possible corruption through unsynchronized concurrent modification.");
        }

        
        public int hashCode() {
            throw new RuntimeException("Possible corruption through unsynchronized concurrent modification.");
        }

        
        public String toString() {
            return "ObjectShortHashMap.REMOVED_KEY";
        }
    };

    private Object[] keys;
    private short[] values;

    private int occupiedWithData;
    private int occupiedWithSentinels;

    public PriMap() {
        this.allocateTable(DEFAULT_INITIAL_CAPACITY << 1);
    }

    public PriMap(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("initial capacity cannot be less than 0");
        }
        int capacity = this.smallestPowerOfTwoGreaterThan(this.fastCeil(initialCapacity * OCCUPIED_DATA_RATIO));
        this.allocateTable(capacity);
    }

    public PriMap(ObjectShortMap<? extends K> map) {
        this(Math.max(map.size(), DEFAULT_INITIAL_CAPACITY));
        this.putAll(map);
    }


    private int smallestPowerOfTwoGreaterThan(int n) {
        return n > 1 ? Integer.highestOneBit(n - 1) << 1 : 1;
    }

    private int fastCeil(float v) {
        int possibleResult = (int) v;
        if (v - possibleResult > 0.0F) {
            possibleResult++;
        }
        return possibleResult;
    }

    
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof ObjectShortMap)) {
            return false;
        }

        ObjectShortMap<K> other = (ObjectShortMap<K>) obj;

        if (this.size() != other.size()) {
            return false;
        }

        for (int i = 0; i < this.keys.length; i++) {
            if (isNonSentinel(this.keys[i]) && (!other.containsKey(this.toNonSentinel(this.keys[i])) || this.values[i] != other.getOrThrow(this.toNonSentinel(this.keys[i])))) {
                return false;
            }
        }
        return true;
    }

    
    public int hashCode() {
        int result = 0;

        for (int i = 0; i < this.keys.length; i++) {
            if (isNonSentinel(this.keys[i])) {
                result += (this.toNonSentinel(this.keys[i]) == null ? 0 : this.keys[i].hashCode()) ^ (int) this.values[i];
            }
        }
        return result;
    }

    
    public String toString() {
        StringBuilder appendable = new StringBuilder();

        appendable.append("{");

        boolean first = true;

        for (int i = 0; i < this.keys.length; i++) {
            Object key = this.keys[i];
            if (isNonSentinel(key)) {
                if (!first) {
                    appendable.append(", ");
                }
                appendable.append(this.toNonSentinel(key)).append("=").append(this.values[i]);
                first = false;
            }
        }
        appendable.append("}");

        return appendable.toString();
    }

    
    public int size() {
        return this.occupiedWithData;
    }

    
    public boolean isEmpty() {
        return this.size() == 0;
    }

    
    public boolean notEmpty() {
        return this.size() != 0;
    }

    
    public String makeString() {
        return this.makeString(", ");
    }

    
    public String makeString(String separator) {
        return this.makeString("", separator, "");
    }

    
    public String makeString(String start, String separator, String end) {
        Appendable stringBuilder = new StringBuilder();
        this.appendString(stringBuilder, start, separator, end);
        return stringBuilder.toString();
    }

    
    public void appendString(Appendable appendable) {
        this.appendString(appendable, ", ");
    }

    
    public void appendString(Appendable appendable, String separator) {
        this.appendString(appendable, "", separator, "");
    }

    
    public void appendString(Appendable appendable, String start, String separator, String end) {
        try {
            appendable.append(start);

            boolean first = true;

            for (int i = 0; i < this.keys.length; i++) {
                Object key = this.keys[i];
                if (isNonSentinel(key)) {
                    if (!first) {
                        appendable.append(separator);
                    }
                    appendable.append(String.valueOf(String.valueOf(this.values[i])));
                    first = false;
                }
            }
            appendable.append(end);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public short[] toArray() {
        short[] result = new short[this.size()];
        int index = 0;

        for (int i = 0; i < this.keys.length; i++) {
            if (isNonSentinel(this.keys[i])) {
                result[index] = this.values[i];
                index++;
            }
        }
        return result;
    }

    
    public void clear() {
        this.occupiedWithData = 0;
        this.occupiedWithSentinels = 0;
        Arrays.fill(this.keys, null);
        Arrays.fill(this.values, EMPTY_VALUE);
    }


    static final float resolution = Short.MAX_VALUE - 1;
    static short shortPri(float p) {
        assert(p==p);
            return (short)Math.round(resolution * unitize(p));
    }
    static short shortPriOrNeg1(float p) {
        if (p!=p)
            return -1;
        else
            return (short)Math.round(resolution * unitize(p));
    }

    static float priShort(short p) {
        if (p < 0)
            return Float.NaN;
        else
            return ((float)p)/resolution;
    }

    public void set(K key, float pri) {
        set(key, shortPriOrNeg1(pri));
    }
    public void add(K key, float pri) {
        assert(pri == pri);
        addToValue(key, shortPri(pri));
    }

    protected void set(K key, short value) {
        int index = this.probe(key);

        if (isNonSentinel(this.keys[index]) && nullSafeEquals(this.toNonSentinel(this.keys[index]), key)) {
            // key already present in map
            this.values[index] = value;
            return;
        }

        this.addKeyValueAtIndex(key, value, index);
    }

    
    public void putAll(ObjectShortMap<? extends K> map) {
        map.forEachKeyValue(this::set);
    }

    
    public void removeKey(K key) {
        int index = this.probe(key);
        this.removeKeyAtIndex(key, index);
    }

    private void removeKeyAtIndex(K key, int index) {
        if (isNonSentinel(this.keys[index]) && nullSafeEquals(this.toNonSentinel(this.keys[index]), key)) {
            this.keys[index] = REMOVED_KEY;
            this.values[index] = EMPTY_VALUE;
            this.occupiedWithData--;
            this.occupiedWithSentinels++;
        }
    }

    
    public void remove(Object key) {
        this.removeKey((K) key);
    }

    
    public short removeKeyIfAbsent(K key, short value) {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]) && nullSafeEquals(this.toNonSentinel(this.keys[index]), key)) {
            this.keys[index] = REMOVED_KEY;
            short oldValue = this.values[index];
            this.values[index] = EMPTY_VALUE;
            this.occupiedWithData--;
            this.occupiedWithSentinels++;

            return oldValue;
        }
        return value;
    }

    
    public short getIfAbsentPut(K key, short value) {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]) && nullSafeEquals(this.toNonSentinel(this.keys[index]), key)) {
            return this.values[index];
        }
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    

    public short updateValue(K key, short initialValueIfAbsent, ShortToShortFunction function) {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]) && nullSafeEquals(this.toNonSentinel(this.keys[index]), key)) {
            this.values[index] = function.valueOf(this.values[index]);
            return this.values[index];
        }
        short value = function.valueOf(initialValueIfAbsent);
        this.addKeyValueAtIndex(key, value, index);
        return value;
    }

    private void addKeyValueAtIndex(K key, short value, int index) {
        if (this.keys[index] == REMOVED_KEY) {
            --this.occupiedWithSentinels;
        }
        this.keys[index] = toSentinelIfNull(key);
        this.values[index] = value;
        ++this.occupiedWithData;
        if (this.occupiedWithData + this.occupiedWithSentinels > this.maxOccupiedWithData()) {
            this.rehashAndGrow();
        }
    }

    
    protected short addToValue(K key, short toBeAdded) {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]) && nullSafeEquals(this.toNonSentinel(this.keys[index]), key)) {
            short v = this.values[index];
            if (v == -1)
                v = toBeAdded; //NaN -> value
            else
                v += toBeAdded;
            return (this.values[index] = v);
        }
        this.addKeyValueAtIndex(key, toBeAdded, index);
        return this.values[index];
    }

    
    public float get(Object key) {
        return priShort(getShort(key));
    }
    protected short getShort(Object key) {
        return this.getIfAbsent(key, EMPTY_VALUE);
    }

    public short getIfAbsent(Object key, short ifAbsent) {
        int index = this.probe(key);
        if (isNonSentinel(this.keys[index]) && nullSafeEquals(this.toNonSentinel(this.keys[index]), key)) {
            return this.values[index];
        }
        return ifAbsent;
    }

    
    public boolean containsKey(Object key) {
        int index = this.probe(key);
        return isNonSentinel(this.keys[index]) && nullSafeEquals(this.toNonSentinel(this.keys[index]), key);
    }

//
//    public boolean containsValue(short value) {
//        for (int i = 0; i < this.values.length; i++) {
//            if (isNonSentinel(this.keys[i]) && this.values[i] == value) {
//                return true;
//            }
//        }
//        return false;
//    }

    
//    public void forEach(ShortProcedure procedure) {
//        this.each(procedure);
//    }
//
//    /**
//     * @since 7.0.
//     */
//
//    public void each(ShortProcedure procedure) {
//        this.forEachValue(procedure);
//    }
//
//
//    public void forEachValue(ShortProcedure procedure) {
//        for (int i = 0; i < this.keys.length; i++) {
//            if (isNonSentinel(this.keys[i])) {
//                procedure.value(this.values[i]);
//            }
//        }
//    }
//
    
    public void forEachKey(Procedure<? super K> procedure) {
        for (int i = 0; i < this.keys.length; i++) {
            if (isNonSentinel(this.keys[i])) {
                procedure.value(this.toNonSentinel(this.keys[i]));
            }
        }
    }

    
    public void forEachKeyValue(ObjectFloatProcedure<? super K> procedure) {
        for (int i = 0; i < this.keys.length; i++) {
            if (isNonSentinel(this.keys[i])) {
                procedure.value(this.toNonSentinel(this.keys[i]), priShort(this.values[i]));
            }
        }
    }


    /**
     * Rehashes every element in the set into a new backing table of the smallest possible size and eliminating removed sentinels.
     */
    public void compact() {
        this.rehash(this.smallestPowerOfTwoGreaterThan(this.size()));
    }

    private void rehashAndGrow() {
        this.rehash(this.keys.length << 1);
    }

    private void rehash(int newCapacity) {
        int oldLength = this.keys.length;
        Object[] old = this.keys;
        short[] oldValues = this.values;
        this.allocateTable(newCapacity);
        this.occupiedWithData = 0;
        this.occupiedWithSentinels = 0;

        for (int i = 0; i < oldLength; i++) {
            if (isNonSentinel(old[i])) {
                this.set(this.toNonSentinel(old[i]), oldValues[i]);
            }
        }
    }

    // exposed for testing
    int probe(Object element) {
        int index = this.spread(element);

        int removedIndex = -1;
        if (isRemovedKey(this.keys[index])) {
            removedIndex = index;
        } else if (this.keys[index] == null || nullSafeEquals(this.toNonSentinel(this.keys[index]), element)) {
            return index;
        }

        int nextIndex = index;
        int probe = 17;

        // loop until an empty slot is reached
        while (true) {
            // Probe algorithm: 17*n*(n+1)/2 where n = no. of collisions
            nextIndex += probe;
            probe += 17;
            nextIndex &= this.keys.length - 1;

            if (isRemovedKey(this.keys[nextIndex])) {
                if (removedIndex == -1) {
                    removedIndex = nextIndex;
                }
            } else if (nullSafeEquals(this.toNonSentinel(this.keys[nextIndex]), element)) {
                return nextIndex;
            } else if (this.keys[nextIndex] == null) {
                return removedIndex == -1 ? nextIndex : removedIndex;
            }
        }
    }

    // exposed for testing
    int spread(Object element) {
        // This function ensures that hashCodes that differ only by
        // constant multiples at each bit position have a bounded
        // number of collisions (approximately 8 at default load factor).
        int h = element == null ? 0 : element.hashCode();
        h ^= h >>> 20 ^ h >>> 12;
        h ^= h >>> 7 ^ h >>> 4;
        return h & (this.keys.length - 1);
    }

    private static boolean nullSafeEquals(Object value, Object other) {
        if (value == null) {
            if (other == null) {
                return true;
            }
        } else if (other == value || value.equals(other)) {
            return true;
        }
        return false;
    }

    private void allocateTable(int sizeToAllocate) {
        this.keys = new Object[sizeToAllocate];
        this.values = new short[sizeToAllocate];
    }

    private static boolean isRemovedKey(Object key) {
        return key == REMOVED_KEY;
    }

    private static <K> boolean isNonSentinel(K key) {
        return key != null && !isRemovedKey(key);
    }

    private K toNonSentinel(Object key) {
        return key == NULL_KEY ? null : (K) key;
    }

    private static Object toSentinelIfNull(Object key) {
        return key == null ? NULL_KEY : key;
    }

    private int maxOccupiedWithData() {
        int capacity = this.keys.length;
        // need at least one free slot for open addressing
        return Math.min(capacity - 1, capacity / OCCUPIED_DATA_RATIO);
    }

    private int maxOccupiedWithSentinels() {
        return this.keys.length / OCCUPIED_SENTINEL_RATIO;
    }

    public Set<K> keySet() {
        return new KeySet();
    }


    private class KeySet implements Set<K> {
        
        public boolean equals(Object obj) {
            if (obj instanceof Set) {
                Set<?> other = (Set<?>) obj;
                if (other.size() == this.size()) {
                    return this.containsAll(other);
                }
            }
            return false;
        }

        
        public int hashCode() {
            int hashCode = 0;
            Object[] table = PriMap.this.keys;
            for (int i = 0; i < table.length; i++) {
                Object key = table[i];
                if (PriMap.isNonSentinel(key)) {
                    K nonSentinelKey = PriMap.this.toNonSentinel(key);
                    hashCode += nonSentinelKey == null ? 0 : nonSentinelKey.hashCode();
                }
            }
            return hashCode;
        }

        
        public int size() {
            return PriMap.this.size();
        }

        
        public boolean isEmpty() {
            return PriMap.this.isEmpty();
        }

        
        public boolean contains(Object o) {
            return PriMap.this.containsKey(o);
        }

        
        public Object[] toArray() {
            int size = PriMap.this.size();
            Object[] result = new Object[size];
            this.copyKeys(result);
            return result;
        }

        
        public <T> T[] toArray(T[] result) {
            int size = PriMap.this.size();
            if (result.length < size) {
                result = (T[]) Array.newInstance(result.getClass().getComponentType(), size);
            }
            this.copyKeys(result);
            if (size < result.length) {
                result[size] = null;
            }
            return result;
        }

        
        public boolean add(K key) {
            throw new UnsupportedOperationException("Cannot call add() on " + this.getClass().getSimpleName());
        }

        
        public boolean remove(Object key) {
            int oldSize = PriMap.this.size();
            PriMap.this.removeKey((K) key);
            return oldSize != PriMap.this.size();
        }

        
        public boolean containsAll(Collection<?> collection) {
            for (Object aCollection : collection) {
                if (!PriMap.this.containsKey(aCollection)) {
                    return false;
                }
            }
            return true;
        }

        
        public boolean addAll(Collection<? extends K> collection) {
            throw new UnsupportedOperationException("Cannot call addAll() on " + this.getClass().getSimpleName());
        }

        
        public boolean retainAll(Collection<?> collection) {
            int oldSize = PriMap.this.size();
            Iterator<K> iterator = this.iterator();
            while (iterator.hasNext()) {
                K next = iterator.next();
                if (!collection.contains(next)) {
                    iterator.remove();
                }
            }
            return oldSize != PriMap.this.size();
        }

        
        public boolean removeAll(Collection<?> collection) {
            int oldSize = PriMap.this.size();
            for (Object object : collection) {
                PriMap.this.removeKey((K) object);
            }
            return oldSize != PriMap.this.size();
        }

        
        public void clear() {
            PriMap.this.clear();
        }

        
        public Iterator<K> iterator() {
            return new KeySetIterator();
        }

        private void copyKeys(Object[] result) {
            int count = 0;
            for (int i = 0; i < PriMap.this.keys.length; i++) {
                Object key = PriMap.this.keys[i];
                if (PriMap.isNonSentinel(key)) {
                    result[count++] = PriMap.this.keys[i];
                }
            }
        }
    }

    private class KeySetIterator implements Iterator<K> {
        private int count;
        private int position;
        private K currentKey;
        private boolean isCurrentKeySet;

        
        public boolean hasNext() {
            return this.count < PriMap.this.size();
        }

        
        public K next() {
            if (!this.hasNext()) {
                throw new NoSuchElementException();
            }
            this.count++;
            Object[] keys = PriMap.this.keys;
            while (!isNonSentinel(keys[this.position])) {
                this.position++;
            }
            this.currentKey = (K) PriMap.this.keys[this.position];
            this.isCurrentKeySet = true;
            this.position++;
            return PriMap.this.toNonSentinel(this.currentKey);
        }

        
        public void remove() {
            if (!this.isCurrentKeySet) {
                throw new IllegalStateException();
            }

            this.isCurrentKeySet = false;
            this.count--;

            if (isNonSentinel(this.currentKey)) {
                int index = this.position - 1;
                PriMap.this.removeKeyAtIndex(PriMap.this.toNonSentinel(this.currentKey), index);
            } else {
                PriMap.this.removeKey(this.currentKey);
            }
        }
    }
}
