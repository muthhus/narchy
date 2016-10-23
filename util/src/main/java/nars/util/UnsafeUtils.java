package nars.util;




import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 *
 *
 *
 * Simple class to obtain access to the {@link Unsafe} object.  {@link Unsafe}
 * is required to allow efficient CAS operations on arrays.  Note that the
 * versions in java.util.concurrent.atomic, such as {@link
 * java.util.concurrent.atomic.AtomicLongArray}, require extra memory ordering
 * guarantees which are generally not needed in these algorithms and are also
 * expensive on most processors.
 *
 * https://github.com/h2oai/h2o-3/blob/master/h2o-core/src/main/java/water/util/UnsafeUtils.java
 * https://github.com/h2oai/h2o-3/blob/master/h2o-core/src/main/java/water/nbhm/UtilUnsafe.java
 * */
public class UnsafeUtils {

  private UnsafeUtils() { } // dummy private constructor

  /** Fetch the Unsafe.  Use With Caution. */
  public static Unsafe getUnsafe() {
    // Not on bootclasspath
    if( UnsafeUtils.class.getClassLoader() == null )
      return Unsafe.getUnsafe();
    try {
      final Field fld = Unsafe.class.getDeclaredField("theUnsafe");
      fld.setAccessible(true);
      return (Unsafe) fld.get(UnsafeUtils.class);
    } catch (Exception e) {
      throw new RuntimeException("Could not obtain access to Unsafe", e);
    }
  }

  private static final Unsafe _unsafe = getUnsafe();
  private static final long _Bbase  = _unsafe.arrayBaseOffset(byte[].class);
  public static byte   get1 ( byte[] buf, int off ) { return _unsafe.getByte  (buf, _Bbase+off); }
  public static int    get2 ( byte[] buf, int off ) { return _unsafe.getShort (buf, _Bbase+off); }
  public static int    get4 ( byte[] buf, int off ) { return _unsafe.getInt   (buf, _Bbase+off); }
  public static long   get8 ( byte[] buf, int off ) { return _unsafe.getLong  (buf, _Bbase+off); }
  public static float  get4f( byte[] buf, int off ) { return _unsafe.getFloat (buf, _Bbase+off); }
  public static double get8d( byte[] buf, int off ) { return _unsafe.getDouble(buf, _Bbase+off); }

  public static int set1 (byte[] buf, int off, byte x )  {_unsafe.putByte  (buf, _Bbase+off, x); return 1;}
  public static int set2 (byte[] buf, int off, short x ) {_unsafe.putShort (buf, _Bbase+off, x); return 2;}
  public static int set4 (byte[] buf, int off, int x   ) {_unsafe.putInt   (buf, _Bbase+off, x); return 4;}
  public static int set4f(byte[] buf, int off, float f ) {_unsafe.putFloat (buf, _Bbase+off, f); return 4;}
  public static int set8 (byte[] buf, int off, long x  ) {_unsafe.putLong  (buf, _Bbase+off, x); return 8;}
  public static int set8d(byte[] buf, int off, double x) {_unsafe.putDouble(buf, _Bbase+off, x); return 8;}
}