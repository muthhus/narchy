package jcog.math;

import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RecyclingPolynomialFitterTest {

    @Test
    public void testLinearSimple() {
        RecyclingPolynomialFitter w = new RecyclingPolynomialFitter(1, 4, 8);
        w.learn(1,1);
        w.learn(2,2);
        w.learn(3,3);
        assertEquals(4, w.guess(4), 0.01f);
        assertEquals(-3, w.guess(-3), 0.01f);
    }

    @Test
    public void testLinearOffset() {
        RecyclingPolynomialFitter w = new RecyclingPolynomialFitter(1, 3, Integer.MAX_VALUE);
        w.learn(1,2+1);
        w.learn(2,4+1);
        w.learn(3,6+1);
        assertEquals(9, w.guess(4), 0.01f);
        assertEquals(-5, w.guess(-3), 0.01f);
    }

    @Test
    public void testCurve() {
        RecyclingPolynomialFitter w = new RecyclingPolynomialFitter(3, 4, 8);
        w.learn(0,0);
        w.learn(1,1);
        w.learn(2,4);
        w.learn(3,9);
        assertEquals(25, w.guess(5), 0.01f);
    }

    @Test
    public void testRecycling() {
        RecyclingPolynomialFitter w = new RecyclingPolynomialFitter(3, 8, Integer.MAX_VALUE);

        FloatToFloatFunction f = (x) -> (float) (-0.1*x*x*x + 1.15*x + 5);

        float noise = 0.25f;

        for (int i = 0; i < 20; i++) {
            float x;
            //0..4
            w.learn( x = (float) (Math.random() * 4), (float) (f.valueOf(x) + 2f*(Math.random()-0.5f)*noise));
        }
        //validate
        for (int x = 0; x < 4; x++) {
            float yActual = f.valueOf(x);
            double yEstim = w.guess(x);
            System.out.println(x + " " + " " + yActual + " " + yEstim);
            assertEquals(yActual, yEstim, 1f);
        }
    }

}