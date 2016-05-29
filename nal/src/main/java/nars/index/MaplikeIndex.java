package nars.index;

import nars.concept.Concept;
import nars.term.Compound;
import nars.term.Term;
import nars.term.TermBuilder;
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.term.container.TermContainer;
import nars.term.container.TermVector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by me on 5/28/16.
 */
public abstract class MaplikeIndex extends AbstractMapIndex {

    public MaplikeIndex(TermBuilder termBuilder, Concept.ConceptBuilder conceptBuilder) {
        super(termBuilder, conceptBuilder);
    }

    @Nullable
    @Override
    protected Termed theCompound(@NotNull Compound x, boolean createIfMissing) {
        return createIfMissing ?
                getNewCompound(x) :
                get(x);
    }

    @Override
    protected Termed theAtom(@NotNull Atomic x, boolean createIfMissing) {
        return createIfMissing ?
                getNewAtom(x) :
                get(x);
    }

    /** default lowest common denominator impl, subclasses may reimpl for more efficiency */
    protected Termed getNewAtom(@NotNull Atomic x) {
        Termed y = get(x);
        if (y == null)  {
            set(y = build(x));
        }
        return y;
    }

    /** default lowest common denominator impl, subclasses may reimpl for more efficiency */
    protected Termed getNewCompound(@NotNull Compound x) {
        Termed y = get(x);
        if (y == null) {
            y = build(x.subterms(), x.op(), x.relation(), x.dt()  /* TODO make this sometimes false */);
            if (!(y.term() instanceof Compound && y.term().hasTemporal())) {
                set(y = build(y));
            }
        }
        return y;
    }


    abstract public Termed remove(Termed entry);

    public abstract Termed get(@NotNull Termed x);

    @Override
    @Nullable
    abstract public void set(@NotNull Termed src, Termed target);

    /* default */ protected TermContainer getSubterms(@NotNull TermContainer t) {
        return null;
    }


    @Override
    public final @Nullable TermContainer theSubterms(TermContainer s) {

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

        if (changed) {
            s = TermVector.the(bb);
        }
        if (!temporal) {
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

}
