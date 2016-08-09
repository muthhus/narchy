package nars.index;

import nars.Op;
import nars.concept.Concept;
import nars.nal.TermBuilder;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.term.compound.GenericCompound;
import nars.term.container.TermContainer;
import nars.term.container.TermVector;
import nars.term.var.Variable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

import static nars.Op.*;
import static nars.nal.Tense.DTERNAL;
import static nars.term.Termed.termOrNull;
import static nars.term.Terms.compoundOrNull;

/**
 * Index which is supported by Map/Cache-like operations
 */
public abstract class MaplikeIndex extends TermBuilder implements TermIndex {


    public static final int invalidConceptBitVector = Op.NEG.bit | Op.VariableBits;
    protected final Concept.ConceptBuilder conceptBuilder;

    public MaplikeIndex(Concept.ConceptBuilder conceptBuilder) {
        this.conceptBuilder = conceptBuilder;
    }

    @NotNull
    @Override
    public Term newCompound(@NotNull Op op, int dt, @NotNull TermContainer subterms) {
        return new GenericCompound(op, dt, subterms);
    }

    @Nullable
    protected Termed theCompound(@NotNull Compound x, boolean createIfMissing) {

        return createIfMissing ?
                getConceptCompound(x) :
                get(x);
    }

    @Override
    protected boolean transformImmediates() {
        return true;
    }

    @Nullable
    protected Termed theAtom(@NotNull Atomic x, boolean createIfMissing) {
        return createIfMissing ?
                getNewAtom(x) :
                get(x);
    }

    /**
     * default lowest common denominator impl, subclasses may reimpl for more efficiency
     */
    @NotNull
    protected Termed getNewAtom(@NotNull Atomic x) {
        Termed y = get(x, false);
        if (y == null) {
            set(y = buildConcept(x));
        }
        return y;
    }

    /**
     * default lowest common denominator impl, subclasses may reimpl for more efficiency
     */
    @Nullable
    protected Termed getConceptCompound(@NotNull Compound x) {
        Termed y = get(x, false);
        if (y == null) {
            //Term yy = x;//buildCompound(x.op(), x.dt(), x.subterms()    /* TODO make this sometimes false */);
            //if (yy == null)
                //return null;
            boolean xnormalized = x.isNormalized();

            @NotNull TermContainer xs = x.subterms();
            TermContainer ys = internSubs(xs);
            Term yy;
            if (xs!=ys) {
                yy = buildCompound(x.op(), x.op().temporal ? DTERNAL : x.dt(), ys);
                if (xnormalized)
                    ((GenericCompound)yy).setNormalized();
            } else {
                yy = x;
            }

            if (xnormalized && canBuildConcept(yy)) {
                set(y = buildConcept(yy));
            } else {
                y = yy;
            }
        }
        return y;
    }

    static protected boolean canBuildConcept(@NotNull Term y) {
        if (y instanceof Compound) {
            if (y.op() == NEG)
                //throw new InvalidConceptException(y, "should have been un-neg");
                return false;
            return true; //return !y.isAny(invalidConceptBitVector) && !y.hasTemporal();
        } else {
            return !(y instanceof Variable);
        }

    }


    @Override
    abstract public void remove(Termed entry);

    @Nullable
    @Override
    public abstract Termed get(@NotNull Termed x);

    @Override
    abstract public void set(@NotNull Termed src, Termed target);

    /* default */
    @Nullable
    protected TermContainer getSubterms(@NotNull TermContainer t) {
        return null;
    }


    @Override public @Nullable TermContainer theSubterms(@NotNull TermContainer s) {

        TermContainer r = s;

        //early existence test:
        TermContainer existing = getSubterms(s);
        if (existing != null)
            return existing;

        s = internSubs(s);

        if (r == s) {
            return s;
        } else {
            TermContainer existing2 = put(s);
            if (existing2 != null)
                s = existing2;

            return s;
        }
    }

    public @NotNull TermContainer internSubs(@NotNull TermContainer s) {
        int ss = s.size();
        Term[] bb = new Term[ss];
        boolean changed = false;//, temporal = false;
        for (int i = 0; i < ss; i++) {
            Term a = s.term(i);

            Term b;
            if (a instanceof Compound) {

                if (a.hasTemporal()) {
                    //temporal = true;//dont store subterm arrays containing temporal compounds
                    b = a;
                } else {
                    /*if (b != a && a.isNormalized())
                        ((GenericCompound) b).setNormalized();*/
                    b = theCompound((Compound) a, true).term();
                }
            } else {
                b = theAtom((Atomic) a, true).term();
            }
            if (a != b) {
                changed = true;
            }
            bb[i] = b;
        }

        return changed ? TermVector.the(bb) : s;
    }

    /**
     * subterms put
     */
    abstract protected TermContainer put(TermContainer s);

    @NotNull
    @Override
    public final TermBuilder builder() {
        return this;
    }

    @Nullable
    @Override
    public final Termed get(@NotNull Termed key, boolean createIfMissing) {

        return key instanceof Compound ?
                theCompound((Compound) key, createIfMissing)
                : theAtom((Atomic) key, createIfMissing);
    }

    @NotNull
    protected Termed buildConcept(@NotNull Termed interned) {
        return conceptBuilder.apply(interned.term());
    }

    @Nullable
    protected final Term buildCompound(@NotNull Op op, int dt, @NotNull TermContainer subs) {
        @Nullable TermContainer s = theSubterms(subs);
        if (s == null)
            return null;
        if (op == INH && (subs.term(1).op() == OPER) && subs.term(0).op() == PROD)
            return termOrNull(build(INH, dt, s.terms())); //HACK send through the full build process in case it is an immediate transform
        else
            return finish(op, dt, s);
    }

    @Override
    public final Concept.ConceptBuilder conceptBuilder() {
        return conceptBuilder;
    }

    @Override
    public abstract void forEach(Consumer<? super Termed> c);
}
