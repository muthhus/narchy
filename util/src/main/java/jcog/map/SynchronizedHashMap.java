package jcog.map;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.function.BiFunction;

/** synchronized in a few methods for NARS bag purposes.
 *
 *  should perform a little better than UnifiedMap, since it has the Java8 lambda compute() etc
 *  the downside is somewhat higher memory usage
 */
public final class SynchronizedHashMap<K, V> extends HashMap<K, V> {

    public SynchronizedHashMap(int cap, float loadFactor) {
        super(cap, loadFactor);
    }

    @Override
    public synchronized V remove(@NotNull Object key) {
        return super.remove(key);
    }

    @Override
    public synchronized V compute(@NotNull K key, @NotNull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return super.compute(key, remappingFunction);
    }
}
