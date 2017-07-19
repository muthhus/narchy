package jcog.data;

import jcog.Texts;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public class QuickLZTest {

    float testCompressDecompress(String s) {
        System.out.print(s + "\n\t");
        return testCompressDecompress(s.getBytes());
    }

    float testCompressDecompress(byte[] input) {
        byte[] compressed = QuickLZ.compress(input);
        byte[] decompress = QuickLZ.decompress(compressed);

        //System.out.println(new String(input));
        //System.out.println(new String(decompress));

        assertArrayEquals(input, decompress);

        float ratio = ((float)compressed.length) / (input.length);
        System.out.println(input.length + " input, " + compressed.length + " compressed = "  +
                Texts.n2(100f * ratio) + "%");
        return ratio;
    }


    @Test
    public void testSome() {

//        float minRatio = Float.POSITIVE_INFINITY;
//
//        for (int matchThresh = 1; matchThresh < 15; matchThresh++) {
//            for (int unc = 1; unc < 15; unc++) {
//                QuickLZ.MATCH_THRESH = matchThresh;
//                QuickLZ.UNCONDITIONAL_MATCHLEN = unc;
//                System.out.println("matchThresh=" + matchThresh + "," + unc);
                testCompressDecompress("x");
                testCompressDecompress("abc");
                testCompressDecompress("abcsdhfjdklsfjdklsfjd;s fja;dksfj;adskfj;adsfkdas;fjadksfj;kasdf");
                testCompressDecompress("222222222222211111111111111112122222222222111111122222");
                float r = testCompressDecompress("(a --> (b --> (c --> (d --> e))))");
//                if (r < minRatio) {
//                    minRatio = r;
//                    System.out.println("BEST so far");
//                }
//                System.out.println();
//            }
//        }
    }
}