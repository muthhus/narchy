package org.intelligentjava.machinelearning.decisiontree;

import java.util.function.Function;



public class TestValue implements Function<Object,Object> {
    
    private Object label;
    
    public TestValue(Object label) {
        super();
        this.label = label;
    }

    @Override
    public Object apply(Object what) {
        return label;
    }

}
