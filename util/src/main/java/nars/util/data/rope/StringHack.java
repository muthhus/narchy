package nars.util.data.rope;

import java.lang.reflect.Field;

/**
 * Direct access to String private fields
 */
public   enum StringHack {
    ;

    public static final Field sbval;
    public static final Field String_value;

    //Add reflection for String value access
    static {
        Field sv = null, sbv = null;
        try {
            sv = String.class.getDeclaredField("value");
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
    }

    public static byte[] bytes(String s) {

        try {
            return (byte[]) String_value.get(s);
        } catch (IllegalAccessException e) {
            //e.printStackTrace();
            throw new RuntimeException(e);
        }
        //return s.toCharArray();
    }
}
