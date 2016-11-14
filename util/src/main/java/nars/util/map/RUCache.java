package nars.util.map;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Implementation which lets me keep an optimal number of elements in memory.
 *
 * The point is that I do not need to keep track of what objects are
 * currently being used since I'm using a combination of a LinkedHashMap
 * for the MRU objects and a WeakHashMap for the LRU objects.
 *
 * So the cache capacity is no less than MRU size plus whatever the
 * GC lets me keep. Whenever objects fall off the MRU they go to the
 * LRU for as long as the GC will have them.
 *
 * http://stackoverflow.com/a/11731495
 */
public class RUCache<K, V> {
    final Map<K, V> MRUdata;
    final Map<K, V> LRUdata;

    public RUCache(final int capacity) {
        LRUdata = new WeakHashMap<K, V>();

        MRUdata = new CapacityLinkedHashMap<>(capacity) {
            @Override
            protected void overflow(Map.Entry<K, V> entry) {
                LRUdata.put(entry.getKey(), entry.getValue());
            }
        };
    }

    public synchronized V tryGet(K k) {
        Map<K, V> MRU = MRUdata;
        return MRU.compute(k, (key, value) -> {
            if (value != null)
                return value;
            else {
                value = LRUdata.remove(key);
                if (value != null) {
                    MRU.put(key, value);
                }
                return value;
            }
        });
    }

    public synchronized void set(K key, V value) {
        LRUdata.remove(key);
        MRUdata.put(key, value);
    }

}