package nars.index;

import nars.Op;
import nars.concept.Concept;
import nars.term.Compound;
import nars.term.Term;
import nars.term.TermBuilder;
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.term.compound.GenericCompound;
import nars.term.container.TermContainer;
import nars.term.container.TermVector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Index which is supported by Map/Cache-like operations
 */
public abstract class MaplikeIndex extends TermBuilder implements TermIndex {


    protected final Concept.ConceptBuilder conceptBuilder;

    public MaplikeIndex(Concept.ConceptBuilder conceptBuilder) {
        this.conceptBuilder = conceptBuilder;
    }

    @Override
    public Term newCompound(Op op, int dt, TermContainer subterms) {
        return new GenericCompound(op, dt, subterms);
    }

    @Nullable
    protected Termed theCompound(@NotNull Compound x, boolean createIfMissing) {
        return createIfMissing ?
                getNewCompound(x) :
                get(x);
    }

    @Override
    protected boolean transforms() {
        return true;
    }

    @Nullable
    protected Termed theAtom(@NotNull Atomic x, boolean createIfMissing) {
        return createIfMissing ?
                getNewAtom(x) :
                get(x);
    }

    /** default lowest common denominator impl, subclasses may reimpl for more efficiency */
    @Nullable
    protected Termed getNewAtom(@NotNull Atomic x) {
        Termed y = get(x);
        if (y == null)  {
            set(y = buildConcept(x));
        }
        return y;
    }

    /** default lowest common denominator impl, subclasses may reimpl for more efficiency */
    @Nullable
    protected Termed getNewCompound(@NotNull Compound x) {
        Termed y = get(x);
        if (y == null) {
            y = buildCompound(x.op(), x.dt(), x.subterms()    /* TODO make this sometimes false */);
            Term yt = y.term();
            if (!(yt instanceof Compound && yt.hasTemporal())) {
                set(y = buildConcept(y));
            }
        }
        return y;
    }


    @Override
    abstract public Termed remove(Termed entry);

    @Nullable
    @Override
    public abstract Termed get(@NotNull Termed x);

    @Override
    @Nullable
    abstract public void set(@NotNull Termed src, Termed target);

    /* default */ @Nullable
    protected TermContainer getSubterms(@NotNull TermContainer t) {
        return null;
    }


    @Override
    public final @Nullable TermContainer theSubterms(@NotNull TermContainer s) {

        //early existence test:
        TermContainer existing = getSubterms(s);
        if (existing!=null)
            return existing;

        int ss = s.size();
        Term[] bb = new Term[ss];
        boolean changed = false, temporal = false;
        for (int i = 0; i < ss; i++) {
            Term a = s.term(i);

            Term b;
            if (a instanceof Compound) {
                if (a.hasTemporal()) {
                    temporal = true;//dont store subterm arrays containing temporal compounds
                }
                b = theCompound((Compound) a, true).term();
            } else {
                b = theAtom((Atomic) a, true).term();
            }
            if (a != b) {
                changed = true;
            }
            bb[i] = b;
        }

        if (changed && !temporal) {
            s = TermVector.the(bb);
            TermContainer existing2 = putIfAbsent(s, s);
            if (existing2 != null)
                s = existing2;
        }
        return s;
    }

    /**
     * subterms put
     */
    abstract protected TermContainer putIfAbsent(TermContainer s, TermContainer s1);

    @Override
    public final TermBuilder builder() {
        return this;
    }

    @Nullable
    @Override
    public final Termed get(@NotNull Termed key, boolean createIfMissing) {

        return key instanceof Compound ?
                theCompound((Compound) key, createIfMissing)
                : theAtom((Atomic)key.term(), createIfMissing);
    }

    @NotNull
    protected Termed buildConcept(@NotNull Termed interned) {
        return conceptBuilder.apply( interned.term() );
    }

    protected final Termed buildCompound(@NotNull Op op, int dt, @NotNull TermContainer subs) {
        return newCompound(op, dt, theSubterms(subs));
    }

    @Override
    public final Concept.ConceptBuilder conceptBuilder() {
        return conceptBuilder;
    }

    @Override
    public abstract void forEach(Consumer<? super Termed> c);
}
