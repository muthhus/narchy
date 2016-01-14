package nars.op.mental;

import nars.NAR;

/**
 * To rememberAction an internal action as an operation
 * <p>
 * called from Concept
 */
public class FullInperience extends Inperience {

    public FullInperience(NAR n) {
        super(n);
    }


    @Override
    public boolean isFull() {
        return true;
    }

}