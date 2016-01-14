package nars.op.mental;

import nars.NAR;

/**
 * To rememberAction an internal action as an operation
 * <p>
 * called from Concept
 */
public class FullInnerperience extends Innerperience {

    public FullInnerperience(NAR n) {
        super(n);
    }


    @Override
    public boolean isFull() {
        return true;
    }

}