package nars.util.signal;

/**
 * generates concepts for reading, writing, and invoking components of a live Java instance
 * via reflection
 */
public class NObj<X> {

    private final X o;

    public NObj(X o) {
        this.o = o;
    }
    
}
