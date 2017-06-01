package org.intelligentjava.machinelearning.decisiontree;

import java.util.Optional;
import java.util.function.Function;



public class TestValue<L> implements Function<String,L> {
    
    private L label;
    
    public TestValue(L label) {
        super();
        this.label = label;
    }

    @Override
    public L apply(String what) {
        return label;
    }

}
