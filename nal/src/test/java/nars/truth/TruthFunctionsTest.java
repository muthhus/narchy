package nars.truth;

import static nars.$.t;
import static nars.Param.TRUTH_EPSILON;

/**
 * Created by me on 5/26/16.
 */
public class TruthFunctionsTest {

//    @Test
//    public void testXNOR() {
//
//        assertEquals(1f, xnor(1f,1f), 0.01f );
//        assertEquals(0.5f, xnor(0.5f,1f), 0.01f );
//        assertEquals(0f, xnor(0f,1f), 0.01f );
//
//        assertEquals(0.5f, xnor(0.5f,0.5f), 0.01f );
//        assertEquals(0.48f, xnor(0.4f,0.6f), 0.01f );
//
//        assertEquals(1f, xnor(0f,0f), 0.01f );
//        assertEquals(0.625f, xnor(0.25f,0.25f), 0.01f );
//    }

//    @Test
//    public void testANDB() {
//        assertEquals(1f, andb(1f,1f), 0.01f );
//        assertEquals(0f, andb(0f,0f), 0.01f );
//        assertEquals(0.5f, andb(0.5f,0.5f), 0.01f );
//        assertEquals(0.5f, andb(0.5f,0.75f), 0.01f );
//
//        assertEquals(0.5f, andb(0f,1f), 0.01f );
//
//    }




    public static void printTruthChart() {
        float c = 0.9f;
        for (float f1 = 0f; f1 <= 1.001f; f1+=0.1f) {
            for (float f2 = 0f; f2 <= 1.001f; f2+=0.1f) {
                Truth t1 = t(f1, c);
                Truth t2 = t(f2, c);
                System.out.println(t1 + " " + t2 + ":\t" +
                        TruthFunctions.comparison(t1, t2, TRUTH_EPSILON));
            }
        }
    }
}