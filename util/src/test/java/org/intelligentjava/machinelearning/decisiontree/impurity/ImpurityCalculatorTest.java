package org.intelligentjava.machinelearning.decisiontree.impurity;


import org.intelligentjava.machinelearning.decisiontree.data.SimpleValue;
import org.intelligentjava.machinelearning.decisiontree.label.BooleanLabel;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ImpurityCalculatorTest {

    @Test
    public void testGetEmpiricalProbability50_50() {
        Function value1 = SimpleValue.data(new String[]{"a"}, BooleanLabel.TRUE_LABEL);
        Function value2 = SimpleValue.data(new String[]{"a"}, BooleanLabel.FALSE_LABEL);
        double p = ImpurityCalculator.
                getEmpiricalProbability("a", Arrays.asList(value1, value2), BooleanLabel.TRUE_LABEL, BooleanLabel.FALSE_LABEL);
        assertEquals(0.5, p, 0.001);
    }
    
}
