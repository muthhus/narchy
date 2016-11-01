package nars.util.data.rope;

import ch.qos.logback.core.encoder.ByteArrayUtil;
import com.google.common.primitives.Bytes;
import nars.util.UnsafeUtils;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * Direct access to String private fields
 */
public   enum StringHack {
    ;

    static final Unsafe unsafe = UnsafeUtils.getUnsafe();

    public static final Field sbval;
    public static final Field String_value;

    private static final long svo;

    //Add reflection for String value access
    static {
        Field sv = null, sbv = null;
        long svov = 0;
        try {
            sv = String.class.getDeclaredField("value");
            svov = unsafe.objectFieldOffset(sv);

            //o = String.class.getDeclaredField("offset");
            sbv = StringBuilder.class.getSuperclass().getDeclaredField("value");

            sv.setAccessible(true);

            sbv.setAccessible(true);
            //o.setAccessible(true);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
        String_value = sv;
        sbval = sbv;
        svo = svov;
    }

    public static byte[] bytes(String s) {

        return (byte[])unsafe.getObject(s, svo);

//        try {
//            return (byte[]) String_value.get(s);
//        } catch (IllegalAccessException e) {
//            //e.printStackTrace();
//            throw new RuntimeException(e);
//        }

        //return s.toCharArray();
    }

    public static int compare(String x, String y) {
        return (x == y) ? 0 : Arrays.compare(bytes(x), bytes(y));

        //return x.compareTo(y);
    }
}
