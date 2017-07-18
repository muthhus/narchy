package nars.derive.meta.op;

import nars.$;
import nars.control.Cause;
import nars.control.premise.Derivation;
import nars.derive.meta.AbstractPred;

import java.util.concurrent.atomic.AtomicInteger;

/** applies a cause id to a derived task. TODO see if this can just be combined
 * with Conclude which was the original design but it didnt quite work with
 * something involving the derivation trie and object uniqueness which may not
 * be the case anymore.
 * */
public class Caused extends AbstractPred<Derivation> {


    private final static AtomicInteger serial = new AtomicInteger(0);

    public Cause cause = null;

    public Caused() {
        super($.func("cause", $.the(serial.incrementAndGet())));
    }

    @Override
    public boolean test(Derivation derivation) {
        derivation.cause = cause;
        return true;
    }
}
