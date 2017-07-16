
package nars.util;

import com.google.common.collect.Sets;
import com.netflix.servo.MonitorRegistry;
import com.netflix.servo.monitor.Monitor;
import com.netflix.servo.util.Preconditions;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Set;

/**
 * MonitorRegistry implementation for Servo monitoring API
 * (not NARchy specific)
 */
public class ConcurrentMonitorRegistry implements MonitorRegistry {

    private final Set<Monitor<?>> monitors = Sets.newConcurrentHashSet();


    /**
     * The set of registered Monitor objects.
     * be careful to not modify it. otherwise it will have to be wrapped in Unmodifidable and that reduces perf
     */
    @Override
    public Collection<Monitor<?>> getRegisteredMonitors() {
        return monitors;// UnmodifiableList.copyOf(monitors);
    }

    /**
     * Register a new monitor in the registry.
     */
    @Override
    public void register(@NotNull Monitor<?> monitor) {
        //Preconditions.checkNotNull(monitor, "monitor");
        try {
            monitors.add(monitor);
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid object", e);
        }
    }

    /**
     * Unregister a Monitor from the registry.
     */
    @Override
    public void unregister(@NotNull Monitor<?> monitor) {
        //Preconditions.checkNotNull(monitor, "monitor");
        try {
            monitors.remove(monitor);
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid object", e);
        }
    }

    @Override
    public boolean isRegistered(Monitor<?> monitor) {
        return monitors.contains(monitor);
    }
}
