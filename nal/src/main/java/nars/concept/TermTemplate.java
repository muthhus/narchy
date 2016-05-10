package nars.concept;

import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;

/** holder for a (weighted) Termlink Template */
public final class TermTemplate implements Termed {

    public final Termed term;
    public final float strength;

    public TermTemplate(Termed term, float strength) {
        this.term = term;
        this.strength = strength;
    }

    @NotNull
    @Override
    public Term term() {
        return term.term();
    }

    @NotNull
    @Override
    public String toString() {
        return "(" + term + ',' + strength + ')';
    }
}
