package nars.index;

import nars.Op;
import nars.concept.Concept;
import nars.term.Compound;
import nars.term.TermBuilder;
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.function.Consumer;


public abstract class AbstractMapIndex implements TermIndex {

    //public final SymbolMap atoms;
    protected final TermBuilder termBuilder;
    protected final Concept.ConceptBuilder conceptBuilder;


    public AbstractMapIndex(TermBuilder termBuilder, Concept.ConceptBuilder conceptBuilder) {
        this(new HashSymbolMap(), termBuilder, conceptBuilder);
    }

    public AbstractMapIndex(SymbolMap symbolMap, TermBuilder termBuilder, Concept.ConceptBuilder conceptBuilder) {
        super();
        this.termBuilder = termBuilder;
        //this.atoms = symbolMap;
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


    @Nullable
    abstract protected Termed theCompound(@NotNull Compound x, boolean createIfMissing);

    abstract protected Termed theAtom(@NotNull Atomic t, boolean createIfMissing);



    @NotNull
    protected Termed build(@NotNull Termed interned) {
        return conceptBuilder.apply(interned.term());
    }

    @NotNull
    protected final Termed build(@NotNull TermContainer subs, @NotNull Op op, int rel, int dt) {
        return termBuilder.make(op, rel, theSubterms(subs), dt);
    }


    @Override
    public final Concept.ConceptBuilder conceptBuilder() {
        return conceptBuilder;
    }

    @Override
    public abstract void forEach(Consumer<? super Termed> c);



}
