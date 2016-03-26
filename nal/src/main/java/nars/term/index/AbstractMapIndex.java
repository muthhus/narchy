package nars.term.index;

import nars.concept.Concept;
import nars.concept.ConceptBuilder;
import nars.nal.meta.match.Ellipsis;
import nars.term.*;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.function.Consumer;
import java.util.function.Function;


public abstract class AbstractMapIndex implements TermIndex {

    public final SymbolMap atoms;
    protected final TermBuilder termBuilder;
    protected final ConceptBuilder conceptBuilder;


    public AbstractMapIndex(TermBuilder termBuilder, ConceptBuilder conceptBuilder) {
        this(new HashSymbolMap(), termBuilder, conceptBuilder);
    }

    public AbstractMapIndex(SymbolMap symbolMap, TermBuilder termBuilder, ConceptBuilder conceptBuilder) {
        super();
        this.termBuilder = termBuilder;
        this.atoms = symbolMap;
        this.conceptBuilder = conceptBuilder;
    }

    @Override
    public final TermBuilder builder() {
        return termBuilder;
    }


    public final Termed get(@NotNull Termed key, boolean createIfMissing) {

        return key instanceof Compound ?
                theCompound((Compound) key, createIfMissing)
                : theAtom((Atomic)key.term(), createIfMissing);
    }


    Termed theAtom(@NotNull Atomic t, boolean createIfMissing) {
        SymbolMap a = this.atoms;
        return (createIfMissing ? a.resolveOrAdd(t, conceptBuilder) : a.resolve(t)) ;
    }

    @Nullable
    abstract protected Termed theCompound(@NotNull Compound x, boolean create);

    @Nullable
    @Override public Termed the(@NotNull Termed t) {
        return get(t, true);
    }

    @Nullable
    @Override public Termed get(@NotNull Termed t) {
        return get(t, false);
    }


    @Override
    public void print(@NotNull PrintStream out) {

        atoms.print(System.out);
        forEach(out::println);

    }


    @Override
    public abstract void forEach(Consumer<? super Termed> c);

}
