package org.intelligentjava.machinelearning.decisiontree;

import java.util.Optional;

import org.intelligentjava.machinelearning.decisiontree.data.Value;

public class TestValue<L> implements Value<L> {
    
    private L label;
    
    public TestValue(L label) {
        super();
        this.label = label;
    }

    @Override
    public L get(String what) {
        return label;
    }

}
