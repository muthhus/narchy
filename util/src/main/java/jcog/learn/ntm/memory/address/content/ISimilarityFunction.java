package jcog.learn.ntm.memory.address.content;

import jcog.learn.ntm.control.Unit;

public interface ISimilarityFunction   
{
    Unit calculate(Unit[] u, Unit[] v);

    void differentiate(Unit similarity, Unit[] u, Unit[] v);
}


