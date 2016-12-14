package jcog.map;

import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;

/** synchronized in a few methods for NARS bag purposes.  conserves memory but at the cost of some CPU usage */
public final class SynchronizedUnifiedMap<K, V> extends UnifiedMap<K, V> {

    public SynchronizedUnifiedMap(int cap, float loadFactor) {
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
