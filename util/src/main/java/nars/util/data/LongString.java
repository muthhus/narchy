package nars.util.data;

import java.math.BigInteger;

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

    public static int maxBase() { return URIchars.length; }

    /** URIchars must be at least base length */
    public static String toString(long aNumber, int base) {
        StringBuilder result = new StringBuilder();
        if (aNumber < 0) {
            result.append('-');
            aNumber = -aNumber;
        }
        int r = (int) (aNumber % base);
        if (aNumber - r == 0)
            result.append(URIchars[r]);
        else
            result.append(LongString.toString((aNumber - r) / base, base) + URIchars[r]);
        return result.toString();
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
