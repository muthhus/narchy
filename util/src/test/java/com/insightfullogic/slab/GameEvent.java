package com.insightfullogic.slab;

public interface GameEvent extends Cursor {

    int getId();

    void setId(int value);

    long getStrength();

    void setStrength(long value);

    int getTarget();
    
    void setTarget(int value);

}
