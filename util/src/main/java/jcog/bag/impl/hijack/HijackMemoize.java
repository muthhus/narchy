package jcog.bag.impl.hijack;

import jcog.Texts;
import jcog.bag.impl.HijackBag;
import jcog.data.MwCounter;
import jcog.pri.PLink;
import jcog.pri.PriReference;
import org.eclipse.collections.api.block.procedure.primitive.ObjectLongProcedure;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * TODO add an instrumentation wrapper to collect statistics
 * about cache efficiency and also processing time of the calculations
 */
public class HijackMemoize<K, V> extends PriorityHijackBag<K, PriReference<Pair<K, V>>> implements Function<K, V> {

    float CACHE_HIT_BOOST;
    float CACHE_DENY_DAMAGE; //damage taken by a cell in rejecting an attempted hijack

    final Function<K, V> func;

    public final MwCounter
            hit = new MwCounter(),  //existing item retrieved
            miss = new MwCounter(),  //a new item inserted that has not existed
            reject = new MwCounter(), //item prevented from insertion by existing items
            evict = new MwCounter(); //removal of existing item on insertion of new item

    //hit + miss + reject = total insertions


    public HijackMemoize(int initialCapacity, int reprobes, @NotNull Function<K, V> f) {
        super(reprobes);
        setCapacity(initialCapacity);
        this.func = f;
    }

    public float statReset(ObjectLongProcedure<String> eachStat) {
        //eachStat.accept("S" /* size */, size() );
        long H, M, R, E;
        eachStat.accept("H" /* hit */, H = hit.getThenZero());
        eachStat.accept("M" /* miss */, M = miss.getThenZero());
        eachStat.accept("R" /* reject */, R = reject.getThenZero());
        eachStat.accept("E" /* evict */, E = evict.getThenZero());
        return (H / ((float) (H + M + R + E)));
    }

    /**
     * estimates the value of computing the input.
     * easier items will introduce lower priority, allowing
     * harder items to sustain longer
     */
    public float value(@NotNull K k) {
        return 0.5f;
        //return reprobes * 2 * CACHE_HIT_BOOST;
    }

    @Override
    public boolean setCapacity(int i) {
        if (super.setCapacity(i)) {
            this.CACHE_HIT_BOOST = i > 0 ?
                    (0.5f / capacity()) : 0;
            //reprobes / (float)Math.sqrt(i) : 0;
            this.CACHE_DENY_DAMAGE = CACHE_HIT_BOOST / (reprobes);
            return true;
        }
        return false;
    }


    @NotNull
    @Override
    public HijackBag<K, PriReference<Pair<K, V>>> commit(@Nullable Consumer<PriReference<Pair<K, V>>> update) {
        //nothing
        return this;
    }

    @Nullable
    public V getIfPresent(@NotNull Object k) {
        PriReference<Pair<K, V>> exists = get(k);
        if (exists != null) {
            exists.priAdd(CACHE_HIT_BOOST);
            hit.inc();
            return exists.get().getTwo();
        }
        return null;
    }

    @Nullable
    public V removeIfPresent(@NotNull K k) {
        PriReference<Pair<K, V>> exists = remove(k);
        if (exists != null) {
            return exists.get().getTwo();
        }
        return null;
    }

    @Override
    @Nullable
    public V apply(@NotNull K k) {
        V v = getIfPresent(k);
        if (v == null) {
            v = func.apply(k);
            if (put(new PLink<>(Tuples.pair(k, v), value(k))) != null) {
                miss.inc();
            } else {
                reject.inc();
            }
        }
        return v;
    }


    @Override
    protected boolean replace(PriReference<Pair<K, V>> incoming, PriReference<Pair<K, V>> existing) {
        if (!super.replace(incoming, existing)) {
            existing.priSub(CACHE_DENY_DAMAGE);
            return false;
        }
        return true;
    }


    @NotNull
    @Override
    public K key(PriReference<Pair<K, V>> value) {
        return value.get().getOne();
    }


    @Override
    protected Consumer<PriReference<Pair<K, V>>> forget(float rate) {
        return null;
    }

    @Override
    public void onRemoved(@NotNull PriReference<Pair<K, V>> value) {
        evict.inc();
    }

    /**
     * clears the statistics
     */
    public String summary() {
        StringBuilder sb = new StringBuilder(32);
        float rate = statReset((k, v) -> {
            sb.append(k).append('=').append(v).append(' ');
        });
        sb.setLength(sb.length() - 1); //remove last ' '
        sb.insert(0, Texts.n2(100f * rate) + "% ");
        return sb.toString();
    }


    /**
     * Agnostic Cache for Method Invokation using Reflection
     * https://raw.githubusercontent.com/ggrandes/memoizer/master/src/main/java/org/javastack/memoizer/Memoizer.java
     * <p>
     * TODO integrate this with HijackMemoize
     */
    static class Memoizer implements InvocationHandler {
        /**
         * Default: 1024 elements
         */
        public static final int DEFAULT_CACHE_MAX_ELEMENTS = 1024;
        /**
         * Default: 1000millis
         */
        public static final long DEFAULT_CACHE_EXPIRE_MILLIS = 1000L; // 1 second

        private final Object object;
        private final Map<CacheKey, CacheValue> cache;
        private final long expireMillis;

        /**
         * Memoize object using default maxElement and default expireMillis
         *
         * @param origin object to speedup
         * @return proxied object
         * @see #memoize(Object, int, long)
         */
        public static Object memoize(final Object origin) //
                throws InstantiationException, IllegalAccessException {
            return memoize(origin, DEFAULT_CACHE_MAX_ELEMENTS, DEFAULT_CACHE_EXPIRE_MILLIS);
        }

        /**
         * Memoize object
         *
         * @param origin       object to speedup
         * @param maxElements  limit elements to cache
         * @param expireMillis expiration time in millis
         * @return proxied object
         */
        public static Object memoize(final Object origin, //
                                     final int maxElements, final long expireMillis) //
                throws InstantiationException, IllegalAccessException {
            final Class<?> clazz = origin.getClass();
            final Memoizer memoizer = new Memoizer(origin, maxElements, expireMillis);
            return Proxy.newProxyInstance(clazz.getClassLoader(), //
                    clazz.getInterfaces(), memoizer);
        }

        private Memoizer(final Object object, final int size, final long expireMillis) {
            this.object = object;
            this.expireMillis = expireMillis;
            this.cache = allocCache(size);
        }

        private static final Map<CacheKey, CacheValue> allocCache(final int maxSize) {
            return Collections.synchronizedMap(new LinkedHashMap<CacheKey, CacheValue>() {
                private static final long serialVersionUID = 42L;

                @Override
                protected boolean removeEldestEntry(java.util.Map.Entry<CacheKey, CacheValue> eldest) {
                    return size() > maxSize;
                }
            });
        }

        /**
         * Internal method
         */
        @Override
        public Object invoke(final Object proxy, //
                             final Method method, //
                             final Object[] args) throws Throwable {
            if (method.getReturnType().equals(Void.TYPE)) {
                // Don't cache void methods
                return invoke(method, args);
            } else {
                final CacheKey key = new CacheKey(method, Arrays.asList(args));
                CacheValue cacheValue = cache.get(key);
                if (cacheValue != null) {
                    final Object ret = cacheValue.getValueIfNotExpired();
                    if (ret != CacheValue.EXPIRED) {
                        return ret;
                    }
                }
                final Object ret = invoke(method, args);
                cache.put(key, new CacheValue(ret, expireMillis));
                return ret;
            }
        }

        private Object invoke(final Method method, final Object[] args) //
                throws Throwable {
            try {
                return method.invoke(object, args);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
        }

        private static final class CacheKey {
            private final Method method;
            private final List<Object> params;

            public CacheKey(final Method method, final List<Object> params) {
                this.method = method;
                this.params = params;
            }

            @Override
            public int hashCode() {
                return (method.hashCode() ^ params.hashCode());
            }

            @Override
            public boolean equals(final Object obj) {
                if (obj instanceof CacheKey) {
                    final CacheKey o = (CacheKey) obj;
                    return o.method.equals(this.method) && //
                            o.params.equals(this.params);
                }
                return false;
            }
        }

        private static final class CacheValue {
            private static final Object EXPIRED = new Object();
            private final Object value;
            private final long expire;

            public CacheValue(final Object value, final long expire) {
                this.value = value;
                this.expire = System.currentTimeMillis() + expire;
            }

            public Object getValueIfNotExpired() {
                if (expire > System.currentTimeMillis()) {
                    return value;
                }
                return EXPIRED;
            }
        }
    }

    /**
     * https://github.com/ggrandes/memoizer/blob/master/src/main/java/org/javastack/memoizer/example/Example.java
     */
    static class Example {
        /**
         * Sample Interface to Memoize
         */
        public static interface SampleInterface {
            public String hash(final String in) throws NoSuchAlgorithmException;
        }

        /**
         * Sample Slow Implementation (MessageDigest with SHA-512)
         */
        public static class SampleSlowImpl implements SampleInterface {
            private static final Charset UTF8 = Charset.forName("UTF-8");

            public String hash(final String in) throws NoSuchAlgorithmException {
                final MessageDigest md = MessageDigest.getInstance("SHA-512");
                final byte[] buf = md.digest(in.getBytes(UTF8));
                return Base64.getEncoder().encodeToString(buf);
            }
        }

        private static final String getHeader(final Class<?> b1, //
                                              final Class<?> b2) {
            final String s1 = b1.getSimpleName();
            final String s2 = b2.getSimpleName();
            if (s1.equals(s2))
                return s1 + ":direct";
            return s1 + ":memoize";
        }

        /**
         * Simple Test / Benchmark
         */
        public static void main(final String[] args) throws Throwable {
            final int TOTAL = (int) 1e6;
            final String TEST_TEXT = "hello world";
            final int cacheElements = 1024;
            final long cacheMillis = 1000; // 1 second
            final SampleInterface[] samples = new SampleInterface[]{
                    new SampleSlowImpl(), //
                    (SampleInterface) Memoizer.memoize(new SampleSlowImpl(), cacheElements, cacheMillis)
            };
            //
            long ts, diff;
            for (int k = 0; k < samples.length; k++) {
                final SampleInterface base = samples[k & ~1];
                final SampleInterface test = samples[k];
                final String hdr = getHeader(base.getClass(), test.getClass());
                ts = System.currentTimeMillis();
                for (int i = 0; i < TOTAL; i++) {
                    test.hash(TEST_TEXT);
                }
                diff = System.currentTimeMillis() - ts;
                System.out.println(hdr + "\t" + "diff=" + diff + "ms" + "\t" + //
                        test.hash(TEST_TEXT));
            }
        }
    }
}
