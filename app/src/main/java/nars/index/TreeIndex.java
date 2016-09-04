package nars.index;

import nars.Op;
import nars.concept.Concept;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.compound.GenericCompound;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * concurrent radix tree index
 */
public class TreeIndex extends TermIndex {

    public final TermTree terms = new TermTree();

    private final Concept.ConceptBuilder conceptBuilder;

    public TreeIndex(Concept.ConceptBuilder conceptBuilder) {
        this.conceptBuilder = conceptBuilder;
    }

    @Override
    public @Nullable Termed get(@NotNull Termed tt, boolean createIfMissing) {
        Term t = tt.term();

        if (t instanceof Compound)
            t = conceptualize((Compound)t);

        if (createIfMissing) {
            Term finalT = t;
            return terms.putIfAbsent(new TermKey(t), ()->conceptBuilder.apply(finalT));
        } else {
            return terms.get(new TermKey(t));
        }
    }

    public TermKey key(@NotNull Term t) {

        if (t instanceof Compound)
            t = conceptualize((Compound)t);

        return new TermKey(t);
    }


    @Override
    public void set(@NotNull Termed src, Termed target) {
        terms.put(key(src.term()), target);
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("yet");
    }

    @Override
    public void forEach(Consumer<? super Termed> c) {
        terms.forEach(c);
    }

    @Override
    public int size() {
        return terms.size(); //WARNING may be slow
    }

    @Override
    public @Nullable Concept.ConceptBuilder conceptBuilder() {
        return conceptBuilder;
    }

    @Override
    public int subtermsCount() {
        return -1;
    }

    @Override
    public @NotNull String summary() {
        return size() + " terms";
    }

    @Override
    public void remove(@NotNull Termed entry) {
        terms.remove(key(entry.term()));
    }

    @Override
    public @NotNull Term newCompound(@NotNull Op op, int dt, @NotNull TermContainer subterms) {
        return new GenericCompound(op, dt, subterms);
    }

    @Override
    protected boolean transformImmediates() {
        return true;
    }
}
