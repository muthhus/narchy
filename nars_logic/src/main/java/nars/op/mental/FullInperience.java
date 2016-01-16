package nars.op.mental;

import nars.NAR;
import org.jetbrains.annotations.NotNull;

/**
 * To rememberAction an internal action as an operation
 * <p>
 * called from Concept
 */
public class FullInperience extends Inperience {

    public FullInperience(@NotNull NAR n) {
        super(n);
    }


    @Override
    public boolean isFull() {
        return true;
    }

}