package nars.util.data;

import nars.NAR;
import nars.concept.Concept;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/** uses a predefined set of terms that will be mapped */
public abstract class SeedConceptMap extends ConceptMap {

    public final Set<Term> terms;

    protected SeedConceptMap(@NotNull NAR nar, Set<Term> terms) {
        super(nar);
        this.terms = terms;
    }


    @Override
    public boolean contains(@NotNull Concept c) {
        Term s = c.get();
        return terms.contains(s);
    }

    public boolean contains(Term t) { return terms.contains(t); }
}
