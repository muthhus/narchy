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
    protected Termed theCompound(@NotNull Compound x, boolean create) {
        //??
//            Termed existing = data.get(x);
//            if (existing!=null)
//                return existing;
//
//            Termed c = internCompound(internSubterms(x.subterms(), x.op(), x.relation(), x.dt()));
//            data.put(c, c);
//            return c;
        return create ?
                theCompoundCreated(x) :
                get(x);
    }


    protected Termed theCompoundCreated(@NotNull Compound x) {

//        if (x.hasTemporal()) {
//            x = theTemporalCompound(x);
//            return x;
//        }

        Termed y = get(x);
        if (y == null) {
            y = internCompoundSubterms(x.subterms(), x.op(), x.relation(), x.dt()  /* TODO make this sometimes false */);
            if (!(y.term() instanceof Compound && y.term().hasTemporal())) {
                y = internCompound(y);

                y = set(y);
            }
        }
        return y;

        //doesnt work due to recursive concurrent modification exception:
//        return data.computeIfAbsent(x, (X) -> {
//            Compound XX = (Compound) X; //??
//            return internCompound(internCompound(XX.subterms(), XX.op(), XX.relation(), XX.dt()));
//        });
    }


    abstract public Termed remove(Termed entry);

    public abstract Termed get(@NotNull Termed x);

    @Override
    @Nullable
    abstract public Termed set(@NotNull Termed src, Termed target);

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
        boolean changed = false;
        for (int i = 0; i < ss; i++) {
            Term a = s.term(i);

            Term b;
            if (a instanceof Compound) {
                if (a.hasTemporal())
                    return s; //dont store subterm arrays containing temporal compounds

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
            TermContainer existing2 = putIfAbsent(s, s);
            if (existing2!=null)
                s = existing2;
        }

        return s;
    }

    /**
     * subterms put
     */
    abstract protected TermContainer putIfAbsent(TermContainer s, TermContainer s1);

}
