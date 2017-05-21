package nars.control;

import nars.Task;
import nars.premise.Derivation;

public final class DirectDerivation extends Derivation {

    @Override
    public void derive(Task x) {
        nar.input(x);
    }

}
