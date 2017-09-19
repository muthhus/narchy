package nars.derive;

import nars.$;
import nars.control.Derivation;
import org.roaringbitmap.PeekableIntIterator;

public class EvaluateChoices extends AbstractPred<Derivation> {

    public final ValueFork[] targets;

    public EvaluateChoices(ValueFork[] targets) {
        super($.func("Evaluate", targets));
        this.targets = targets;
    }

    @Override
    public boolean test(Derivation d) {

        //TODO sort the choices by their value
        PeekableIntIterator c = d.choices.getIntIterator();
        while (d.live() && c.hasNext()) {
            int n = c.next();

            targets[n].test(d);
        }

        return false;
    }
}
