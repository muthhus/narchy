package nars.term.index;

import nars.concept.AtomConcept;
import nars.concept.Concept;
import nars.term.Term;
import nars.term.atom.Atomic;

import java.util.function.Function;

/**
 * Created by me on 3/13/16.
 */
public interface SymbolMap {

    AtomConcept resolve(String id);

    default AtomConcept resolve(Atomic a) {
        return resolve(a.toString());
    }

    AtomConcept resolveOrAdd(String s,Function<Term, Concept> conceptBuilder);

    default AtomConcept resolveOrAdd(Atomic a,Function<Term, Concept> conceptBuilder) {
        return resolveOrAdd(a.toString(), conceptBuilder);
    }

    void print(Appendable out);

}
