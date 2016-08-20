/* Copyright 2009 - 2010 The Stajistics Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nars.util;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.common.io.Closeables;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import nars.util.data.list.FasterList;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Arrays.stream;

/**
 *
 *
 *
 */
public enum Util {
    Util;


    public static final int PRIME3 = 524287;
    public static final int PRIME2 = 92821;
    public static final int PRIME1 = 31;

    /**
     * It is basically the same as a lookup table with 2048 entries and linear interpolation between the entries, but all this with IEEE floating point tricks.
     * http://stackoverflow.com/questions/412019/math-optimization-in-c-sharp#412988
     */
    public static double expFast(double val) {
        long tmp = (long) (1512775 * val + (1072693248 - 60801));
        return Double.longBitsToDouble(tmp << 32);
    }


//    /**
//     * Fetch the Unsafe.  Use With Caution.
//     */
//    public static Unsafe getUnsafe() {
//        // Not on bootclasspath
//        if (Util.class.getClassLoader() == null)
//            return Unsafe.getUnsafe();
//        try {
//            Field fld = Unsafe.class.getDeclaredField("theUnsafe");
//            fld.setAccessible(true);
//            return (Unsafe) fld.get(Util.class);
//        } catch (Exception e) {
//            throw new RuntimeException("Could not obtain access to Unsafe", e);
//        }
//    }

    public static String UUIDbase64() {
        long low = UUID.randomUUID().getLeastSignificantBits();
        long high = UUID.randomUUID().getMostSignificantBits();
        return new String(Base64.getEncoder().encode(
                Bytes.concat(
                        Longs.toByteArray(low),
                        Longs.toByteArray(high)
                )
        ));
    }

//    public static int hash(int a, int b) {
//        return PRIME2 * (PRIME2 + a) + b;
//    }
//
//    public static int hash(int a, int b, int c) {
//        return PRIME2 * (PRIME2 * (PRIME2 + a) + b) + c;
//    }

//    public final static int hash(int a, int b, int c, int d) {
//        return PRIME2 * (PRIME2 * (PRIME2 * (PRIME2 + a) + b) + c) + d;
//    }

//    public final static int hash(int a, int b, int c, int d, long e) {
//        long x = PRIME2 * (PRIME2 * (PRIME2 * (PRIME2 * (PRIME2 + a) + b) + c) + d) + e;
//        return (int)x;
//    }

    public static int hash(Object a, Object b) {
        return hashCombine(a.hashCode(), b.hashCode());
    }

    public static int hash(Object a, Object b, Object c) {
        return hashCombine(a.hashCode(), b.hashCode(), c.hashCode());
    }

//    public final static int hash(Object a, Object b, Object c, Object d) {
//        return hash(a.hashCode(), b.hashCode(), c.hashCode(), d.hashCode());
//    }

    public static void assertNotNull(Object test, String varName) {
        if (test == null) {
            throw new NullPointerException(varName);
        }
    }

    public static void assertNotEmpty(Object[] test, String varName) {
        if (test == null) {
            throw new NullPointerException(varName);
        }
        if (test.length == 0) {
            throw new IllegalArgumentException("empty " + varName);
        }
    }

    public static void assertNotEmpty(CharSequence test, String varName) {
        if (test == null) {
            throw new NullPointerException(varName);
        }
        if (test.length() == 0) {
            throw new IllegalArgumentException("empty " + varName);
        }
    }

    public static void assertNotBlank(CharSequence test, String varName) {
        if (test != null) {
            test = test.toString().trim();
        }
        assertNotEmpty(test, varName);
    }

    public static <E> void assertNotEmpty(Collection<E> test, String varName) {
        if (test == null) {
            throw new NullPointerException(varName);
        }
        if (test.isEmpty()) {
            throw new IllegalArgumentException("empty " + varName);
        }
    }

    public static <K, V> void assertNotEmpty(Map<K, V> test, String varName) {
        if (test == null) {
            throw new NullPointerException(varName);
        }
        if (test.isEmpty()) {
            throw new IllegalArgumentException("empty " + varName);
        }
    }

    public static boolean equalsNullAware(Object obj1, Object obj2) {
        if (obj1 == null) {
            return obj2 == null;

        }
        if (obj2 == null) {
            return false;
        }

        return obj1.equals(obj2);
    }

    public static String globToRegEx(String line) {

        line = line.trim();
        int strLen = line.length();
        StringBuilder sb = new StringBuilder(strLen);
        // Remove beginning and ending * globs because they're useless
        if (line.length() > 0 && line.charAt(0) == '*') {
            line = line.substring(1);
            strLen--;
        }
        if (line.length() > 0 && line.charAt(line.length() - 1) == '*') {
            line = line.substring(0, strLen - 1);
            strLen--;
        }
        boolean escaping = false;
        int inCurlies = 0;
        for (char currentChar : line.toCharArray()) {
            switch (currentChar) {
                case '*':
                    if (escaping)
                        sb.append("\\*");
                    else
                        sb.append(".*");
                    escaping = false;
                    break;
                case '?':
                    if (escaping)
                        sb.append("\\?");
                    else
                        sb.append('.');
                    escaping = false;
                    break;
                case '.':
                case '(':
                case ')':
                case '+':
                case '|':
                case '^':
                case '$':
                case '@':
                case '%':
                    sb.append('\\');
                    sb.append(currentChar);
                    escaping = false;
                    break;
                case '\\':
                    if (escaping) {
                        sb.append("\\\\");
                        escaping = false;
                    } else
                        escaping = true;
                    break;
                case '{':
                    if (escaping) {
                        sb.append("\\{");
                    } else {
                        sb.append('(');
                        inCurlies++;
                    }
                    escaping = false;
                    break;
                case '}':
                    if (inCurlies > 0 && !escaping) {
                        sb.append(')');
                        inCurlies--;
                    } else if (escaping)
                        sb.append("\\}");
                    else
                        sb.append('}');
                    escaping = false;
                    break;
                case ',':
                    if (inCurlies > 0 && !escaping) {
                        sb.append('|');
                    } else if (escaping)
                        sb.append("\\,");
                    else
                        sb.append(',');
                    break;
                default:
                    escaping = false;
                    sb.append(currentChar);
            }
        }
        return sb.toString();
    }



/*
 **************************************************************************
 *                                                                        *
 *          General Purpose Hash Function Algorithms Library              *
 *                                                                        *
 * Author: Arash Partow - 2002                                            *
 * URL: http://www.partow.net                                             *
 * URL: http://www.partow.net/programming/hashfunctions/index.html        *
 *                                                                        *
 * Copyright notice:                                                      *
 * Free use of the General Purpose Hash Function Algorithms Library is    *
 * permitted under the guidelines and in accordance with the most current *
 * version of the Common Public License.                                  *
 * http://www.opensource.org/licenses/cpl1.0.php                          *
 *                                                                        *
 **************************************************************************
*/


    /*class GeneralHashFunctionLibrary
    {*/


    public static long RSHash(String str) {
        int b = 378551;
        int a = 63689;
        long hash = 0;

        for (int i = 0; i < str.length(); i++) {
            hash = hash * a + str.charAt(i);
            a = a * b;
        }

        return hash;
    }
   /* End Of RS Hash Function */


    public static long JSHash(String str) {
        long hash = 1315423911;

        for (int i = 0; i < str.length(); i++) {
            hash ^= ((hash << 5) + str.charAt(i) + (hash >> 2));
        }

        return hash;
    }
   /* End Of JS Hash Function */


    public static long PJWHash(String str) {
        long BitsInUnsignedInt = (4 * 8);
        long ThreeQuarters = (BitsInUnsignedInt * 3) / 4;
        long OneEighth = BitsInUnsignedInt / 8;
        long HighBits = (0xFFFFFFFFL) << (BitsInUnsignedInt - OneEighth);
        long hash = 0;
        long test = 0;

        for (int i = 0; i < str.length(); i++) {
            hash = (hash << OneEighth) + str.charAt(i);

            if ((test = hash & HighBits) != 0) {
                hash = ((hash ^ (test >> ThreeQuarters)) & (~HighBits));
            }
        }

        return hash;
    }
   /* End Of  P. J. Weinberger Hash Function */


    public static long ELFHash(String str) {
        long hash = 0;
        long x = 0;

        for (int i = 0; i < str.length(); i++) {
            hash = (hash << 4) + str.charAt(i);

            if ((x = hash & 0xF0000000L) != 0) {
                hash ^= (x >> 24);
            }
            hash &= ~x;
        }

        return hash;
    }


    /** from clojure.Util */
    public static int hashCombine(int seed, int hash) {
        return seed ^ ( hash + 0x9e3779b9 + (seed << 6) + (seed >> 2) );

        //return seed * 31 + hash;
    }

    public static int hashCombine(int a, int b, int c) {
        //TODO decide if this is efficient and hashes well
        return hashCombine(hashCombine(a, b), c);
    }

    public static int hashCombine(Object[] t) {
        if (t.length == 0)
            return 1;
        int x = t[0].hashCode();
        for (int i = 1; i < t.length; i++) {
            x = hashCombine(x, t[i].hashCode());
        }
        return x;
    }


    public static int ELFHashNonZero(byte[] str, int seed) {
        int i  = (int) ELFHash(str, seed);
        if (i == 0) i = 1;
        return i;
    }

    public static long ELFHash(byte[] str, long seed) {

        long hash = seed;

        int len = str.length;

        for (byte aStr : str) {
            hash = (hash << 4) + aStr;

            long x;
            if ((x = hash & 0xF0000000L) != 0) {
                hash ^= (x >> 24);
            }
            hash &= ~x;
        }

        return hash;
    }



    public static long BKDRHash(String str) {
        long seed = 131; // 31 131 1313 13131 131313 etc..
        long hash = 0;

        for (int i = 0; i < str.length(); i++) {
            hash = (hash * seed) + str.charAt(i);
        }

        return hash;
    }
   /* End Of BKDR Hash Function */


    public static long SDBMHash(String str) {
        long hash = 0;

        for (int i = 0; i < str.length(); i++) {
            hash = str.charAt(i) + (hash << 6) + (hash << 16) - hash;
        }

        return hash;
    }
   /* End Of SDBM Hash Function */


    public static long DJBHash(String str) {
        long hash = 5381;

        for (int i = 0; i < str.length(); i++) {
            hash = ((hash << 5) + hash) + str.charAt(i);
        }

        return hash;
    }
   /* End Of DJB Hash Function */


    public static long DEKHash(String str) {
        long hash = str.length();

        for (int i = 0; i < str.length(); i++) {
            hash = ((hash << 5) ^ (hash >> 27)) ^ str.charAt(i);
        }

        return hash;
    }
   /* End Of DEK Hash Function */


    public static long BPHash(String str) {
        long hash = 0;

        for (int i = 0; i < str.length(); i++) {
            hash = hash << 7 ^ str.charAt(i);
        }

        return hash;
    }
   /* End Of BP Hash Function */


    public static long FNVHash(String str) {
        long fnv_prime = 0x811C9DC5;
        long hash = 0;

        for (int i = 0; i < str.length(); i++) {
            hash *= fnv_prime;
            hash ^= str.charAt(i);
        }

        return hash;
    }
   /* End Of FNV Hash Function */


    public static long APHash(String str) {
        long hash = 0xAAAAAAAA;

        for (int i = 0; i < str.length(); i++) {
            hash ^= (i & 1) == 0 ? (hash << 7) ^ str.charAt(i) * (hash >> 3) : ~((hash << 11) + str.charAt(i) ^ (hash >> 5));
        }

        return hash;
    }
   /* End Of AP Hash Function */

//    }


    /**
     * returns the next index
     */
    public static int long2Bytes(long l, byte[] target, int offset) {
        for (int i = offset + 7; i >= offset; i--) {
            target[i] = (byte) (l & 0xFF);
            l >>= 8;
        }
        return offset + 8;
    }

    /**
     * returns the next index
     */
    public static int int2Bytes(int l, byte[] target, int offset) {
        for (int i = offset + 3; i >= offset; i--) {
            target[i] = (byte) (l & 0xFF);
            l >>= 8;
        }
        return offset + 4;
    }

    /**
     * http://www.java-gaming.org/index.php?topic=24194.0
     */
    public static int floorInt(float x) {
        return (int) (x + BIG_ENOUGH_FLOOR) - BIG_ENOUGH_INT;
    }

    private static final int BIG_ENOUGH_INT = 16 * 1024;
    private static final double BIG_ENOUGH_FLOOR = BIG_ENOUGH_INT;
    private static final double BIG_ENOUGH_ROUND = BIG_ENOUGH_INT + 0.5;


    /**
     * linear interpolate between target & current, factor is between 0 and 1.0
     */
    public static float lerp(float target, float current, float factor) {
        //return target * factor + current * (1.0f - factor);
        return Math.fma(target, factor, current * (1.0f - factor));
    }
    public static double lerp(double target, double current, double factor) {
        //return target * factor + current * (1.0f - factor);
        return Math.fma(target, factor, current * (1.0f - factor));
    }
    /**
     * maximum, simpler and faster than Math.max without its additional tests
     */
    public static float max(float a, float b) {
        return (a > b) ? a : b;
    }

    public static float mean(float a, float b) {
        return (a + b) * 0.5f;
    }


    public static short f2s(float conf) {
        return (short) (conf * Short.MAX_VALUE);
    }

    public static byte f2b(float conf) {
        return (byte) (conf * Byte.MAX_VALUE);
    }

    /**
     * removal rates are approximately monotonically increasing function;
     * tests first, mid and last for this  ordering
     * first items are highest, so it is actually descending order
     * TODO improve accuracy
     */
    public static boolean isSemiMonotonicallyDec(double[] count) {


        int cl = count.length;
        return
                (count[0] >= count[cl - 1]) &&
                        (count[cl / 2] >= count[cl - 1]);
    }

    /* TODO improve accuracy */
    public static boolean isSemiMonotonicallyInc(int[] count) {

        int cl = count.length;
        return
                (count[0] <= count[cl - 1]) &&
                        (count[cl / 2] <= count[cl - 1]);
    }

    /**
     * Generic utility method for running a list of tasks in current thread
     */
    public static void run(Deque<Runnable> tasks) {
        run(tasks, tasks.size(), Runnable::run);
    }

    public static void run(Deque<Runnable> tasks, int maxTasksToRun, Consumer<Runnable> runner) {
        while (!tasks.isEmpty() && maxTasksToRun-- > 0) {
            runner.accept( tasks.removeFirst() );
        }
    }

//    /**
//     * Generic utility method for running a list of tasks in current thread (concurrency == 1) or in multiple threads (> 1, in which case it will block until they finish)
//     */
//    public static void run(Deque<Runnable> tasks, int maxTasksToRun, int threads) {
//
//        //int concurrency = Math.min(threads, maxTasksToRun);
//        //if (concurrency == 1) {
//            tasks.forEach(Runnable::run);
////            return;
//  //      }
////
////        ConcurrentContext ctx = ConcurrentContext.enter();
////        ctx.setConcurrency(concurrency);
////
////        try {
////            run(tasks, maxTasksToRun, ctx::execute);
////        } finally {
////            // Waits for all concurrent executions to complete.
////            // Re-exports any exception raised during concurrent executions.
////            if (ctx != null)
////                ctx.exit();
////        }
//
//    }

    /**
     * clamps a value to 0..1 range
     */
    public static float clamp(float p) {
        if (p > 1.0f)
            p = 1.0f;
        else if (p < 0.0f)
            p = 0.0f;
        return p;
    }

    /**
     * clamps a value to -1..1 range
     */
    public static float clampBi(float p) {
        if (p > 1f)
            return 1f;
        if (p < -1f)
            return -1f;
        return p;
    }

    /**
     * discretizes values to nearest finite resolution real number determined by epsilon spacing
     */
    public static float round(float value, float epsilon) {

        return Math.round(value / epsilon) * epsilon;

    }

    public static float clampround(float value, float epsilon) {
        return round(clamp(value), epsilon );
    }

    public static int hash(float f, int discretness) {
        return (int) (f * discretness);
    }

    public static boolean equals(double a, double b) {
        return equals(a, b, Double.MIN_VALUE * 2);
    }

    public static boolean equals(float a, float b) {
        return equals(a, b, Float.MIN_VALUE * 2);
    }

    /**
     * tests equivalence (according to epsilon precision)
     */
    public static boolean equals(float a, float b, float epsilon) {
        return Math.abs(a - b) < epsilon;
    }

    /**
     * tests equivalence (according to epsilon precision)
     */
    public static boolean equals(double a, double b, double epsilon) {
        return Math.abs(a - b) < epsilon;
    }

    public static void pause(long milli) {
        pauseWait(milli);
    }

    private final static Object waitLock = new Object();

    public static long pauseWaitUntil(long untilTargetTime) {
        long now = System.currentTimeMillis();
        long dt = untilTargetTime-now;
        if (dt > 0) {
            synchronized(waitLock) {
                try {
                    waitLock.wait(dt);
                } catch (InterruptedException e) { }
            }

            now = System.currentTimeMillis();
        }
        return now;
    }

//    /** from: http://stackoverflow.com/a/1205300 */
//    public static long pauseLockUntil(long untilTargetTime) {
//
//    // Wait until the desired next time arrives using nanosecond
//    // accuracy timer (wait(time) isn't accurate enough on most platforms)
//        long now = System.currentTimeMillis();
//        long dt = (untilTargetTime-now) * 1000000;
//        if (dt > 0) {
//            LockSupport.parkNanos(dt);
//            now = System.currentTimeMillis();
//        }
//        return now;
//    }

    /** from boofcv: */
    static void pauseWait(long milli) {
        if (milli <= 0) return;
        
        Thread t = Thread.currentThread();
        long start = System.currentTimeMillis();
        long now;
        while((now=System.currentTimeMillis()) - start < milli) {
            synchronized(t) {
                try {
                    long ignore = milli - (now - start);
                    if(ignore > 0L) {
                        t.wait(ignore);
                    }
                } catch (InterruptedException var9) {
                }
            }
        }

    }

    /** applies a quick, non-lexicographic ordering compare
     * by first testing their lengths
     */
    public static int compare(long[] x, long[] y) {
        if (x == y) return 0;

        int xlen = x.length;

        int yLen = y.length;
        if (xlen != yLen) {
            return Integer.compare(xlen, yLen);
        } else {

            for (int i = 0; i < xlen; i++) {
                int c = Long.compare(x[i], y[i]);
                if (c!=0)
                    return c; //first different chra
            }

            return 0; //equal
        }
    }

    public static byte[] intAsByteArray(int index) {

        if (index < 36) {
            byte x = base36(index);
            return new byte[] {  x};
        }
        else if (index < (36*36)){
            byte x1 = base36(index%36);
            byte x2 = base36(index/36);
            return new byte[] { x2, x1};
        }
        else {
            throw new RuntimeException("variable index out of range for this method");
        }



//        int digits = (index >= 256 ? 3 : ((index >= 16) ? 2 : 1));
//        StringBuilder cb  = new StringBuilder(1 + digits).append(type);
//        do {
//            cb.append(  Character.forDigit(index % 16, 16) ); index /= 16;
//        } while (index != 0);
//        return cb.toString();

    }

    public static int bin(float x, int bins) {
        return (int) Math.floor((x + (0.5f / bins)) * bins);
    }

    /** bins a priority value to an integer */
    public static int decimalize(float v) {
        return bin(v,10);
    }

    /** finds the mean value of a given bin */
    public static float unbinCenter(int b, int bins) {
        return ((float)b)/bins;
    }

    public static <D> D runProbability(Random rng, float[] probs, D[] choices) {
        float tProb = 0;
        for (int i = 0; i < probs.length; i++) {
            tProb += probs[i];
        }
        float s = rng.nextFloat() * tProb;
        int c = 0;
        for (int i = 0; i < probs.length; i++) {
            s -= probs[i];
            if (s <= 0) { c = i; break; }
        }
        return choices[c];
    }


    public static MethodHandle mhRef(Class<?> type, String name) {
        try {
            return MethodHandles
                    .lookup()
                    //.publicLookup(
                    .unreflect(stream(type.getMethods()).filter(m -> m.getName().equals(name)).findFirst().get());
        } catch (IllegalAccessException e) {
            throw new Error(e);
        }
    }

    public static <F> MethodHandle mh(String name, F fun) {
        return mh(name, fun.getClass(), fun);
    }

    public static <F> MethodHandle mh(String name, Class<? extends F> type, F fun) {
        return mhRef(type, name).bindTo(fun);
    }
    public static <F> MethodHandle mh(String name, F... fun) {
        F fun0 = fun[0];
        MethodHandle m = mh(name, fun0.getClass(), fun0);
        for (int i = 1; i < fun.length; i++) {
            m = m.bindTo(fun[i]);
        }
        return m;
    }


    public static byte base36(int index) {
        if (index < 10)
            return (byte) ('0' + index);
        else if (index < (10 + 26))
            return (byte) ((index - 10) + 'a');
        else
            throw new RuntimeException("out of bounds");
    }

    /** clamps output to 0..+1.  y=0.5 at x=0 */
    public static float sigmoid(float v) {
        return 1f / (1f + (float)Math.exp(-v));
    }

    public static float sigmoidDiff(float a, float b) {
        float sum = a + b;
        float delta = a - b;
        float deltaNorm = delta / sum;
        return sigmoid(deltaNorm);
    }

    public static float sigmoidDiffAbs(float a, float b) {
        float sum = a + b;
        float delta = Math.abs(a - b);
        float deltaNorm = delta / sum;
        return sigmoid(deltaNorm);
    }

    /**
     * 2 decimal representation of values between 0 and 1. only the tens and hundredth
     * decimal point are displayed - not the ones, and not a decimal point.
     * for compact display.
     * if the value=1.0, then 'aa' is the result
     */
    @NotNull
    public static String n2u(float x) {
        if ((x < 0) || (x > 1)) throw new RuntimeException("values >=0 and <=1");
        int hundreds = (int) Texts.hundredths(x);
        if (x == 100) return "aa";
        return hundreds < 10 ? "0" + hundreds : Integer.toString(hundreds);
    }

    public static List<String> inputToStrings(InputStream is) throws IOException {
        List<String> x = CharStreams.readLines(new InputStreamReader(is, Charsets.UTF_8));
        Closeables.closeQuietly(is);
        return x;
    }
    public static String inputToString(InputStream is) throws IOException {
        String s = CharStreams.toString(new InputStreamReader(is, Charsets.UTF_8));
        Closeables.closeQuietly(is);
        return s;
    }


    public static int[] reverse(IntArrayList l) {
        switch (l.size()) {
            case 0: throw new UnsupportedOperationException(); //should never happen
            case 1: return new int[] {l.get(0)};
            case 2: return new int[] {l.get(1), l.get(0)};
            case 3: return new int[] {l.get(2), l.get(1), l.get(0)};
            default:
                //reverse the array since it has been constructed in reverse
                //TODO use more efficient array reversal
                return l.asReversed().toArray();//toReversed().toArray();
        }
    }

    public static String s(String s, int maxLen) {
        if (s.length() < maxLen) return  s;
        return s.substring(0,maxLen-2) + "..";
    }

    public static void writeBits(int x, int numBits, float[] y,  int offset) {

        for (int i= 0, j =offset; i < numBits; i++, j++) {
            int mask = 1 << i;
            y[j] = ((x & mask) == 1) ? 1f :0f;
        }

    }

    /** a and b must be instances of input, and output must be of size input.length-2 */
    public static <X> X[] except(X[] input, X a, X b, X[] output) {
        int targetLen = input.length - 2;
        if (output.length!= targetLen) {
            throw new RuntimeException("wrong size");
        }
        int j = 0;
        for (X x : input) {
            if ((x!=a) && (x!=b))
                output[j++] = x;
        }

        return output;
    }


    public static double normalize(double x, double min, double max) {
        return (x - min) / (max - min);
    }
    public static float normalize(float x, float min, float max) {
        return (x - min) / (max - min);
    }

    public static int lastNonNull(Object... x) {
        int j = -1;
        if (x!=null) {
            int k = x.length;
            for (int i = 0; i < k; i++) {
                if (x[i] != null)
                    j = i;
            }
        }
        return j;
    }

    public static float variance(float[] population){
        float average = 0.0f;
        for(float p: population){
            average += p;
        }
        int n = population.length;
        average /= n;

        float variance = 0.0f;
        for(float p: population){
            float d = p - average;
            variance += d * d;
        }
        return variance / n;
    }
    public static double[] avgvar(double[] population){
        double average = 0.0;
        for(double p: population){
            average += p;
        }
        int n = population.length;
        average /= n;

        double variance = 0.0;
        for(double p: population){
            double d = p - average;
            variance += d * d;
        }
        variance /= n;

        return new double[] { average, variance };
    }

    public static String className(Object p) {
        String s = p.getClass().getSimpleName();
        if (s.isEmpty())
            return p.getClass().toString().replace("class ", "");
        return s;
    }

    public static float[] toFloat(double[] d) {
        int l = d.length;
        float[] f = new float[l];
        for (int i = 0; i < l; i++)
            f[i] = (float)d[i];
        return f;
    }
    public static double[] toDouble(float[] d) {
        int l = d.length;
        double[] f = new double[l];
        for (int i = 0; i < l; i++)
            f[i] = d[i];
        return f;
    }

    public static float[] minmax(float[] x) {
        //float sum = 0;
        float min = Float.POSITIVE_INFINITY;
        float max = Float.NEGATIVE_INFINITY;
        for (float y : x) {
            //sum += y;
            if (y < min) min = y;
            if (y > max) max = y;
        }
        return new float[] { min, max/*, sum */};
    }

    /** slightly more streamlined variatoin of Arrays.equals which assumes there are no null values */
    public static boolean equals(@NotNull Object[] a, @NotNull Object[] b) {
        if (a == b) return true;
        int al = a.length;
        if (al == b.length) {
            for (int i = 0; i < al; i++) {
                if (!a[i].equals(b[i]))
                    return false;
            }
            return true;
        }
        return false;
    }

    public static void time(Logger logger, String procName, Runnable procedure) {
        long start = System.currentTimeMillis();
        procedure.run();
        long end = System.currentTimeMillis();
        logger.info("{} ({} ms)", procName, (end-start));
    }

    public static <X> Stream<X> fileCache(Path p, String baseName, Supplier<Stream<X>> o,
                                          BiConsumer<X,DataOutput> encoder,
                                          Function<DataInput,X> decoder,
                                          Logger logger
                                          ) throws IOException {

        File f = p.toFile();
        long lastModified = f.lastModified();
        long size = f.length();
        String suffix = "_" + p.getFileName() + "_" + lastModified + "_" + size;

        List<X> buffer = new FasterList(1024 /* estimate */);

        String tempDir = System.getProperty("java.io.tmpdir");

        File cached = new File(tempDir, baseName + suffix);
        if (cached.exists()) {
            //try read
            try {

                FileInputStream ff = new FileInputStream(cached);
                DataInputStream din = new DataInputStream(new BufferedInputStream(ff));
                while (din.available() > 0) {
                    buffer.add(decoder.apply(din));
                }
                din.close();

                logger.warn("cache loaded {}: ({} bytes, from {})", cached.getAbsolutePath(), cached.length(), new Date(cached.lastModified()));

                return buffer.stream();
            } catch (Exception e) {
                logger.warn("{}, regenerating..", e);
                //continue below
            }
        }

        //save
        buffer.clear();

        Stream<X> instanced = o.get();

        DataOutputStream dout = new DataOutputStream( new BufferedOutputStream( new FileOutputStream(cached.getAbsolutePath()) ) );
        instanced.forEach(c -> {
            buffer.add(c);
            encoder.accept(c, dout);
        });
        dout.close();
        logger.warn("cache saved {}: ({} bytes)", cached.getAbsolutePath(), dout.size());

        return buffer.stream();


    }

    public static float sum(float[] x) {
        float y = 0;
        for (float f : x) {
            y += f;
        }
        return y;
    }
}
