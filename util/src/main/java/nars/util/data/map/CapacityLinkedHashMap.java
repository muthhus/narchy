package nars.util.data.map;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by me on 6/28/16.
 */
public class CapacityLinkedHashMap<K, V> extends LinkedHashMap<K, V> {
    private final int capacity;

    public CapacityLinkedHashMap(int capacity) {
        super(capacity + 1, 1.0f, true);
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
