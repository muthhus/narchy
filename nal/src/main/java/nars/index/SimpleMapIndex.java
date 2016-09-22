package nars.index;

import nars.concept.PermanentConcept;
import nars.concept.util.ConceptBuilder;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/** implements AbstractMapIndex with one ordinary map implementation. does not cache subterm vectors */
abstract public class SimpleMapIndex extends MaplikeIndex {

    protected final Map<Term,Termed> concepts;

    SimpleMapIndex(ConceptBuilder conceptBuilder, Map<Term, Termed> compounds) {
        super(conceptBuilder);
        this.concepts = compounds;
    }

    @Override
    public Termed get(Term x, boolean createIfMissing) {
        if (createIfMissing) {
            return concepts.computeIfAbsent(x, conceptBuilder::apply);
        } else {
            return concepts.get(x);
        }
    }

    @Override
    public void remove(Term entry) {
        concepts.remove(entry);
    }



    @Override
    public void set(@NotNull Term src, Termed target) {
        concepts.merge(src, target, setIfNotAlreadyPermanent);
    }

    @Override
    public void clear() {
        concepts.clear();
    }

    @Override
    public void forEach(@NotNull Consumer<? super Termed> c) {
        concepts.forEach((k, v)-> c.accept(v));
    }

    @Override
    public int size() {
        return concepts.size() /* + atoms.size? */;
    }


}