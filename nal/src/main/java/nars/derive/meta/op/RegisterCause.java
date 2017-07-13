package nars.derive.meta.op;

import nars.$;
import nars.control.Cause;
import nars.control.premise.Derivation;
import nars.derive.meta.AbstractPred;

import java.util.concurrent.atomic.AtomicInteger;

public class RegisterCause extends AbstractPred<Derivation> {


    private final static AtomicInteger serial = new AtomicInteger(0);

    public Cause cause = null;

    public RegisterCause() {
        super($.func("cause", $.the(serial.incrementAndGet())));
    }

    @Override
    public boolean test(Derivation derivation) {
        derivation.cause = cause;
        return true;
    }
}
