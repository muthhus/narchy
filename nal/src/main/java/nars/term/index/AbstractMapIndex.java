package nars.term.index;

import nars.Op;
import nars.concept.ConceptBuilder;
import nars.term.Compound;
import nars.term.TermBuilder;
import nars.term.TermIndex;
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.function.Consumer;


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


    @Nullable
    @Override
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


    @NotNull
    protected Termed<Compound> internCompound(@NotNull Termed interned) {
        return conceptBuilder.apply(interned.term());
    }

    @NotNull
    protected final Termed internSubterms(@NotNull TermContainer subs, @NotNull Op op, int rel, int dt) {
        Termed interned = termBuilder.make(op, rel, subs, dt);
        assert(interned!=null); //should not fail unless the input was invalid to begin with
        return interned;
    }

    @Override
    public void print(@NotNull PrintStream out) {

        atoms.print(System.out);
        forEach(out::println);

    }
    @Override
    public final ConceptBuilder conceptBuilder() {
        return conceptBuilder;
    }

    @Override
    public abstract void forEach(Consumer<? super Termed> c);

}
