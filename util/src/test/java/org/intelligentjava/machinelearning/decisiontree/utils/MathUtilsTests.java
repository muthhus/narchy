package org.intelligentjava.machinelearning.decisiontree.utils;

import org.junit.Assert;
import org.junit.Test;

import static com.google.common.math.DoubleMath.log2;


public class MathUtilsTests {

    @Test
    public void testLog2() {
        Assert.assertEquals(1.0, log2(2.0), 0.01);
        Assert.assertEquals(3.32192809, log2(10.0), 0.00000001);
    }

}
