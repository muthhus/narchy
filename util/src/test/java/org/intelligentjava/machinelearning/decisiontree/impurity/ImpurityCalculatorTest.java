package org.intelligentjava.machinelearning.decisiontree.impurity;


import java.util.Arrays;

import org.intelligentjava.machinelearning.decisiontree.data.Value;
import org.intelligentjava.machinelearning.decisiontree.data.SimpleValue;
import org.intelligentjava.machinelearning.decisiontree.label.BooleanLabel;
import org.junit.Assert;
import org.junit.Test;

public class ImpurityCalculatorTest {

    @Test
    public void testGetEmpiricalProbability50_50() {
        Value value1 = SimpleValue.data(new String[]{"a"}, BooleanLabel.TRUE_LABEL);
        Value value2 = SimpleValue.data(new String[]{"a"}, BooleanLabel.FALSE_LABEL);
        double p = ImpurityCalculator.
                getEmpiricalProbability("a", Arrays.asList(value1, value2), BooleanLabel.TRUE_LABEL, BooleanLabel.FALSE_LABEL);
        Assert.assertEquals(0.5, p, 0.001);
    }
    
}
