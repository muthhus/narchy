/*
 * Copyright (C) 2012 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package jcog;

import com.google.common.annotations.GwtIncompatible;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Modifications to guava's ServiceManager
 * <p>
 * A manager for monitoring and controlling a set of {@linkplain Service services}. This class
 * provides methods for {@linkplain #startAsync() starting}, {@linkplain #stopAsync() stopping} and
 * {@linkplain #servicesByState inspecting} a collection of {@linkplain Service services}.
 * Additionally, users can monitor state transitions with the {@linkplain Listener listener}
 * mechanism.
 * <p>
 * <p>While it is recommended that service lifecycles be managed via this class, state transitions
 * initiated via other mechanisms do not impact the correctness of its methods. For example, if the
 * services are started by some mechanism besides {@link #startAsync}, the listeners will be invoked
 * when appropriate and {@link #awaitHealthy} will still work as expected.
 * <p>
 * <p>Here is a simple example of how to use a {@code ServiceManager} to start a server.
 * <pre>   {@code
 * class Server {
 *   public static void main(String[] args) {
 *     Set<Service> services = ...;
 *     ServiceManager manager = new ServiceManager(services);
 *     manager.addListener(new Listener() {
 *         public void stopped() {}
 *         public void healthy() {
 *           // Services have been initialized and are healthy, start accepting requests...
 *         }
 *         public void failure(Service service) {
 *           // Something failed, at this point we could log it, notify a load balancer, or take
 *           // some other action.  For now we will just exit.
 *           System.exit(1);
 *         }
 *       },
 *       MoreExecutors.directExecutor());
 *
 *     Runtime.getRuntime().addShutdownHook(new Thread() {
 *       public void run() {
 *         // Give the services 5 seconds to stop to ensure that we are responsive to shutdown
 *         // requests.
 *         try {
 *           manager.stopAsync().awaitStopped(5, TimeUnit.SECONDS);
 *         } catch (TimeoutException timeout) {
 *           // stopping timed out
 *         }
 *       }
 *     });
 *     manager.startAsync();  // start all the services asynchronously
 *   }
 * }}</pre>
 * <p>
 * <p>This class uses the ServiceManager's methods to start all of its services, to respond to
 * service failure and to ensure that when the JVM is shutting down all the services are stopped.
 *
 * @author Luke Sandberg
 * @since 14.0
 */
@GwtIncompatible
public class Services<C, X>  {

    private final C id;
    private final Executor exe;

//    abstract public static class SubService<P,C,X> extends Services<C,X> implements Service<P> {
//        private final P parent;
//
//        public SubService(C id, P parent) {
//            super(id);
//            this.parent = parent;
//        }
//    }

    enum ServiceState {
        Off, OffToOn, On, OnToOff, Deleted
    }

    public interface Service<C> {
        void start(C x, Executor exe);

        ServiceState state();

        void stop(C x, Executor exe, @Nullable Runnable afterDelete);

        default void delete() {

        }

    }

    public static abstract class AbstractService<C> extends AtomicReference<ServiceState> implements Service<C> {

        protected AbstractService() {
            super(ServiceState.Off);
        }

        @Override
        public String toString() {
            return getClass() + "[" + super.toString() +"]";
        }

        @Override
        public final void start(C x, Executor exe) {
            if (compareAndSet(ServiceState.Off, ServiceState.OffToOn)) {
                exe.execute(() -> {
                    try {
                        start(x);
                        assert (
                                compareAndSet(ServiceState.OffToOn, ServiceState.On)
                        );
                    } catch (Throwable e) {
                        e.printStackTrace();
                        delete();
                    }
                });
            }
        }

        @Override
        public Services.ServiceState state() {
            return get();
        }

        @Override
        public final void stop(C x, Executor exe, @Nullable Runnable afterDelete) {
            if (compareAndSet(ServiceState.On, ServiceState.OnToOff)) {
                exe.execute(() -> {
                    try {
                        stop(x);
                        assert (
                                compareAndSet(ServiceState.OnToOff, ServiceState.Off)
                        );
                        if (afterDelete!=null) {
                            delete();
                            afterDelete.run();
                        }
                    } catch (Throwable e) {
                        e.printStackTrace();
                        delete();
                    }
                });
            }
        }

        @Override
        public void delete() {
            set(ServiceState.Deleted);
        }

        abstract protected void start(C x);

        abstract protected void stop(C x);


    }

    public void printServices(PrintStream out) {
        services.forEach((k, s) -> {
            out.println(k + " " + s.state());
        });
    }


    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Services.class);

    private final ConcurrentMap<X, Service> services;

    public Services(C id) {
        this(id, ForkJoinPool.commonPool());
    }

    protected Services() {
        this(null, ForkJoinPool.commonPool());
    }

    /**
     * Constructs a new instance for managing the given services.
     *
     * @param services The services to manage
     * @param x
     * @throws IllegalArgumentException if not all services are {@linkplain ServiceState#NEW new} or if there
     *                                  are any duplicate services.
     */
    protected Services(@Nullable C id, Executor exe) {
        this.id = id == null ? (C)this : id;
        this.exe = exe;
        this.services = new ConcurrentHashMap<>();
    }

    public Stream<Service> stream() {
        return services.values().stream();
    }

    public void add(X key, Service s) {
        add(key, s, true);
    }


    public void add(X key, Service s, boolean start) {
        Service removed = services.put(key, s);

        if (removed == s)
            return; //no change

        if (removed != null) {
            //if start, then start after the previous stopped
            removed.stop(id, exe, start ? ()->{
                s.start(id, exe);
            } : null);
        } else {
            if (start)
                s.start(id, exe);
        }


    }


    /**
     * Initiates service {@linkplain Service#stopAsync shutdown} if necessary on all the services
     * being managed.
     *
     * @return this
     */
    @CanIgnoreReturnValue
    public Services stop() {
        for (Service service : services.values()) {
            service.stop(id, exe, null);
        }
        return this;
    }

    public void delete() {
        services.values().removeIf(x -> { x.stop(id, exe, ()->{}); return true; });
    }


//    /**
//     * Waits for the all the services to reach a terminal state. After this method returns all
//     * services will either be {@linkplain Service.State#TERMINATED terminated} or
//     * {@linkplain Service.State#FAILED failed}.
//     */
//    public void awaitStopped() {
//        state.awaitStopped();
//    }
//
//    /**
//     * Waits for the all the services to reach a terminal state for no more than the given time. After
//     * this method returns all services will either be {@linkplain Service.State#TERMINATED
//     * terminated} or {@linkplain Service.State#FAILED failed}.
//     *
//     * @param timeout the maximum time to wait
//     * @param unit    the time unit of the timeout argument
//     * @throws TimeoutException if not all of the services have stopped within the deadline
//     */
//    public void awaitStopped(long timeout, TimeUnit unit) throws TimeoutException {
//        state.awaitStopped(timeout, unit);
//    }
//
//    /**
//     * Returns true if all services are currently in the {@linkplain State#RUNNING running} state.
//     * <p>
//     * <p>Users who want more detailed information should use the {@link #servicesByState} method to
//     * get detailed information about which services are not running.
//     */
//    public boolean isHealthy() {
//        for (Service service : services) {
//            if (!service.isRunning()) {
//                return false;
//            }
//        }
//        return true;
////    }
//
//    /**
//     * Provides a snapshot of the current state of all the services under management.
//     * <p>
//     * <p>N.B. This snapshot is guaranteed to be consistent, i.e. the set of states returned will
//     * correspond to a point in time view of the services.
//     */
//    public ImmutableMultimap<State, Service> servicesByState() {
//        return state.servicesByState();
//    }
//
//    /**
//     * Returns the service load times. This value will only return startup times for services that
//     * have finished starting.
//     *
//     * @return Map of services and their corresponding startup time in millis, the map entries will be
//     * ordered by startup time.
//     */
//    public ImmutableMap<Service, Long> startupTimes() {
//        return state.startupTimes();
//    }

}