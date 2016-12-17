package jcog.map;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by me on 6/28/16.
 */
public class MRUCache<K, V> extends LinkedHashMap<K, V> {

    private int capacity;

    public MRUCache(int capacity) {
        super(capacity + 1, 1.0f, true);
        this.capacity = capacity;
    }


    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> entry) {
        if (this.size() > capacity) {
            overflow(entry);
            return true;
        }
        return false;
    }

    protected void overflow(Map.Entry<K, V> entry) {

    }
}
