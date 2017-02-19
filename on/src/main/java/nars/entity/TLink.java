package nars.entity;

public interface TLink<T> {

    short getIndex(final int i);
    
    T getTarget();
    
    float getPriority();
    
}
