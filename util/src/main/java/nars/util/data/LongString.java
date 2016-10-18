package nars.util.data;

/**
 * Created by me on 7/10/16.
 */
public class LongString {


    static final char[] URIchars;
    static {
        StringBuilder x = new StringBuilder("0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_.~");

        //add some extended ascii http://www.ascii-code.com/
        for (int i = 192; i <= 255; i++) {
            x.append((char)i);
        }

        URIchars = x.toString().toCharArray();
    }

    public static final int maxBase = URIchars.length;

    /** URIchars must be at least base length */
    public static String toString(long aNumber, int base) {
        StringBuilder result = new StringBuilder(8);

        append(result, aNumber, base);

        return result.toString();
    }

    public static CharSequence toString(long l) {
        return toString(l, LongString.maxBase);
    }

    public static void append(StringBuilder target, long v) {
        append(target, v, maxBase);
    }

    public static void append(StringBuilder target, long v, int base) {
        if (v < 0) {
            target.append('-');
            v = -v;
        }

        int r = (int) (v % base);

        if (v - r != 0)
            append(target, (v - r) / base, base);

        target.append(URIchars[r]);

    }


//    public static long parseLong65(String aNumber, int base) {
//        char[] digits;
//        int sign = 1;
//        if (aNumber.charAt(0) == '-') {
//            sign = -1;
//            digits = aNumber.substring(1).toCharArray();
//        } else {
//            digits = aNumber.toCharArray();
//        }
//        BigInteger bigBase = BigInteger.valueOf(base);
//        BigInteger power = bigBase.pow(digits.length);
//        BigInteger total = BigInteger.valueOf(0);
//        for (char digit : digits) {
//            power = power.divide(bigBase);
//            total = total.add(power.multiply(BigInteger.valueOf(URIchars.indexOf(digit))));
//        }
//        return sign * total.longValue();
//    }

}
