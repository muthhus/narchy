package nars.index;

import nars.concept.util.ConceptBuilder;
import nars.term.Compound;
import nars.term.Termed;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Consumer;

/** implements AbstractMapIndex with one ordinary map implementation. does not cache subterm vectors */
abstract public class SimpleMapIndex extends MaplikeIndex {

    protected final Map<Termed,Termed> concepts;

    SimpleMapIndex(ConceptBuilder conceptBuilder, Map<Termed, Termed> compounds) {
        super(conceptBuilder);
        this.concepts = compounds;
    }

    @NotNull
    @Override
    protected Termed getNewAtom(@NotNull Atomic x) {
        return concepts.computeIfAbsent(x, this::buildConcept);
    }

    @NotNull
    protected Termed getConceptCompound(@NotNull Compound x) {
        return concepts.computeIfAbsent(x, this::buildConcept);
    }


    @Override
    public void remove(Termed entry) {
        concepts.remove(entry);
    }


    @Override public final Termed get(@NotNull Termed x) {
        return concepts.get(x);
    }


    @Override
    public final void set(@NotNull Termed src, Termed target) {
        concepts.put(src, target);

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