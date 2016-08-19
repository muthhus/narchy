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
import static nars.term.Termed.termOrNull;

/**
 * Index which is supported by Map/Cache-like operations
 */
public abstract class MaplikeIndex extends TermIndex {



    private final Concept.ConceptBuilder conceptBuilder;

    public MaplikeIndex(Concept.ConceptBuilder conceptBuilder) {
        this.conceptBuilder = conceptBuilder;
    }

    @NotNull
    @Override
    public Term newCompound(@NotNull Op op, int dt, @NotNull TermContainer subterms) {
        return new GenericCompound(op, dt, subterms);
    }

    @Nullable Termed theCompound(@NotNull Compound x, boolean createIfMissing) {

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


            if (canBuildConcept(x)) {
                boolean xnormalized = x.isNormalized();

                TermContainer xs = x.subterms();
                TermContainer ys = theSubterms(xs);
                Term yy;
                if (xs != ys) {
                    //x should have already been atemporalized by this point, so any 'dt' will be from Images
                    yy = new GenericCompound(x.op(), x.dt(), ys);
                    if (xnormalized)
                        ((GenericCompound) yy).setNormalized();

                    //                if (xnormalized && !yy.isNormalized()) {
                    //                    yy = normalize(yy, false);
                    //                }
                } else {
                    yy = x;
                }

                set(y = buildConcept(yy));
            } else {
                y = x;
            }

//            if (/*xnormalized && */canBuildConcept(yy)) {
//
//            } else {
//                //set(x, y = yy);
//
//            }
        }
        return y;
    }

    static protected boolean canBuildConcept(@NotNull Term y) {
        if (y instanceof Compound) {
            if (y.op() == NEG)
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



    @Override
    public final @NotNull TermContainer theSubterms(@NotNull TermContainer s) {
        int ss = s.size();
        Term[] bb = new Term[ss];
        boolean changed = false;//, temporal = false;
        for (int i = 0; i < ss; i++) {
            Term a = s.term(i);

            Term b;
            if (a instanceof Compound) {

                if (!canBuildConcept(a) || a.hasTemporal()) {
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

        return internSubterms(changed ? TermVector.the(bb) : s);
    }


    abstract public @NotNull TermContainer internSubterms(@NotNull TermContainer s);

//    {
//
////        //early existence test:
////        TermContainer existing = getSubterms(s);
////
////        return existing != null ? existing : internSubs(s);
//
//        return s;
//    }

//    private @NotNull TermContainer internSubs(@NotNull TermContainer s) {
//        int ss = s.size();
//        Term[] bb = new Term[ss];
//        boolean changed = false;//, temporal = false;
//        for (int i = 0; i < ss; i++) {
//            Term a = s.term(i);
//
//            Term b;
//            if (a instanceof Compound) {
//
//                if (!canBuildConcept(a) || a.hasTemporal()) {
//                    //temporal = true;//dont store subterm arrays containing temporal compounds
//                    b = a;
//                } else {
//                    /*if (b != a && a.isNormalized())
//                        ((GenericCompound) b).setNormalized();*/
//                    b = theCompound((Compound) a, true).term();
//                }
//            } else {
//                b = theAtom((Atomic) a, true).term();
//            }
//            if (a != b) {
//                changed = true;
//            }
//            bb[i] = b;
//        }
//
//        return put(changed ? TermVector.the(bb) : s);
//    }



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
