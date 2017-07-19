package nars.index;

import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.atom.IntAtom;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.Op.Null;
import static nars.term.Terms.compoundOrNull;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

/**
 * Created by me on 1/2/16.
 */
public abstract class TermBuilder {


    //        int arity = u.length;
//        switch (op) {
////            case INT:
////            case INTRANGE:
////                System.out.println(op + " " + dt + " " + Arrays.toString(u));
////                break;
//
//            case NEG:
//                if (arity != 1)
//                    throw new InvalidTermException(op, dt, "negation requires 1 subterm", u);
//
//                return neg(u[0]);
//
////            case INTRANGE:
////                System.err.println("intRange: " + Arrays.toString(u));
////                break;
//
//            case INSTANCE:
//                if (arity != 2 || dt != DTERNAL) throw new InvalidTermException(INSTANCE, dt, "needs 2 arg", u);
//                return inst(u[0], u[1]);
//            case PROPERTY:
//                if (arity != 2 || dt != DTERNAL) throw new InvalidTermException(PROPERTY, dt, "needs 2 arg", u);
//                return prop(u[0], u[1]);
//            case INSTANCE_PROPERTY:
//                if (arity != 2 || dt != DTERNAL)
//                    throw new InvalidTermException(INSTANCE_PROPERTY, dt, "needs 2 arg", u);
//                return instprop(u[0], u[1]);
//
//
//            case DISJ:
//                if (dt != DTERNAL)
//                    throw new InvalidTermException(op, dt, "Disjunction must be DTERNAL", u);
//                return disjunction(u);
//            case CONJ:
//                return conj(dt(dt), u);
//
//            case IMGi:
//            case IMGe:
//                //if no relation was specified and it's an Image,
//                //it must contain a _ placeholder
//
////                if ((arity < 1) || (dt > arity))
////                    throw new InvalidTermException(op, dt, "image requires size=2 excluding _ imdex", u);
//
//                if (hasImdex(u)) {
//                    return image(op, u);
//                }
//
////                if ((dt < 0) && !(u[0].varPattern() > 0 || u[1].varPattern() > 0))
////                    throw new InvalidTermException(op, dt, "Invalid Image", u);
//
//
//                break; //construct below
//
//
//            case DIFFe:
//            case DIFFi:
//                return newDiff(op, u);
//            case SECTe:
//                return newIntersection(u,
//                        SECTe,
//                        SETe,
//                        SETi);
//            case SECTi:
//                return newIntersection(u,
//                        SECTi,
//                        SETi,
//                        SETe);
//
//            case EQUI:
//            case IMPL:
//                dt = dt(dt);
//                //fall-through:
//            case INH:
//            case SIM:
//                if (arity == 1)
//                    return True;
//                if (arity != 2)
//                    throw new InvalidTermException(op, dt, "Statement without exactly 2 arguments", u);
//                return statement(op, dt, u[0], u[1]);
//
//            case PROD:
//                return (arity != 0) ? compound(op, u) : Terms.ZeroProduct;
//
//        }
//
//
//        return finish(op, dt, u);
    //}


//    /**
//     * should only be applied to subterms, not the outer-most compound
//     */
//    @NotNull
//    public Term productNormalize(@NotNull Term u) {
//        if (!(u instanceof Compound))
//            return u;
//
//        int b = u.structure();
//        if (!((b & Op.InhAndIMGbits) > 0) || !((b & INH.bit) > 0) || u.varPattern() > 0)
//            return u;
//
//        Term t = u.unneg();
//        boolean neg = (t != u);
//
//        if (t.op() == INH) {
//            Compound ct = (Compound) t;
//            Term[] sp = ct.toArray();
//            Term s = sp[0];
//            Op so = s.op();
//            Term p = sp[1];
//            Op po = p.op();
//            if (so == Op.IMGi && !po.image) {
//                Compound ii = (Compound) s;
//                t = the(Op.INH, ii.sub(0), imageUnwrapToProd(p, ii));
//            } else if (po == Op.IMGe && !so.image) {
//                Compound jj = (Compound) p;
//                t = the(Op.INH, imageUnwrapToProd(s, jj), jj.sub(0));
//            } else {
//                return u; //original value
//            }
//
//        }
//
//        return !neg ? t : $.neg(t);
//    }
//
//    @NotNull
//    private Term imageUnwrapToProd(Term p, @NotNull Compound ii) {
//        return the(Op.PROD, imageUnwrap(ii, p));
//    }
//
//    @NotNull
//    public static Term[] imageUnwrap(@NotNull Compound image, Term other) {
//        int l = image.size();
//        Term[] t = new Term[l];
//        int r = image.dt();
//        @NotNull Term[] imageTerms = image.toArray();
//        for (int i = 0 /* skip the first element of the image */, j = 0; j < l; ) {
//            t[j++] = ((j) == r) ? other : imageTerms[++i];
//        }
//        return t;
//    }


//    public TermContainer subterms(@NotNull TreeSet<Term> s) {
//        return subterms(s.toArray(new Term[s.size()]));
//    }


//    protected Compound newCompound(@NotNull Op op, Term[] subterms) {
//        if (!op.image && !(this instanceof PatternTermIndex) /* HACK */ && subterms.length == 1) {
//            return new UnitCompound1(op, subterms[0]); //HACK avoid creating the TermContainer if possible
//        }
//        return newCompound(op, subterms(subterms));
//    }

//    /**
//     * directly constructs a new instance, applied at the end.
//     */
//    protected Compound newCompound(@NotNull Op op, TermContainer subterms) {
//        if (!op.image && !(this instanceof PatternTermIndex) /* HACK */ && subterms.size() == 1) {
//            return new UnitCompound1(op, subterms.sub(0)); //HACK avoid creating the TermContainer if possible
//        }
//
//
//        return new GenericCompound(op, subterms);
//    }

    @NotNull
    public final Term the(@NotNull Op op, @NotNull Term... tt) {
        return op.the(DTERNAL, tt);
    }

//    /**
//     * NOTE: terms must be sorted, if they need to be, before calling.
//     */
//    @NotNull
//    protected Term compound(@NotNull Op op, @NotNull Term... args) {
//
//        int s = args.length;
//        assert (s != 0);
//
//        for (int i = 0; i < s; i++) {
//            Term x = args[i];
//
//            if (isAbsolute(x))
//                return Null; //may have become False through eval()
//
//            if ((i == 0) && (s == 1) && (op.minSize > 1) && !(x instanceof Ellipsislike)) {
//                //special case: allow for ellipsis to occupy one item even if minArity>1
//                return x;
//            }
//
//            args[i] = x;
//        }
//
//
//        if (s == 1 && op.minSize > 1) {
//
//            Term a0 = args[0];
//            if (!(a0 instanceof Ellipsislike)) {
//                //return null;
//                //throw new RuntimeException("invalid size " + s + " for " + op);
//                return a0; //reduction
//            }
//        }
//
//        return newCompound(op, args);
//    }


//    public Term replace(@NotNull Term c, @NotNull Term x, @NotNull Term y) {
//        return $.terms.replace(c, x, y);
//    }


    //    /**
//     * whether to apply immediate transforms during compound building
//     */
//    protected boolean transformImmediates() {
//        return true;
//    }


//    @Nullable
//    public Term subtractSet(@NotNull Op setType, @NotNull Compound A, @NotNull Compound B) {
//        return difference(setType, A, B);
//    }


    @NotNull
    public Term the(@NotNull Compound csrc, @NotNull Term... newSubs) {
        return csrc.op().the(csrc.dt(), newSubs);
    }


    @Nullable
    public Term the(@NotNull Object o) {
        if (o instanceof Term)
            return ((Term) o);

        if (o instanceof Number)
            return the((Number) o);

        //if (o instanceof String || o instanceof StringBuilder)
        return Atomic.the(o.toString());

        //return null;
    }

    public Atomic the(@NotNull Number o) {

        if (o instanceof Integer) return IntAtom.the(o.intValue());

        return Atomic.the(o.toString());

    }


    @NotNull
    public Term atemporalize(final @NotNull Compound x) {

        if (!x.hasAny(Op.TemporalBits))// isTemporal())
            return x;

        Term[] s = x.toArray();

        //1. determine if any subterms (excluding the term itself) get rewritten
        boolean subsChanged = false;
        if (x.subterms().hasAny(Op.TemporalBits)) {

            //atemporalize subterms first

            int cs = s.length;
            for (int i = 0; i < cs; i++) {

                Term xi = s[i];
                if (xi instanceof Compound) {
                    Term yi = atemporalize((Compound) xi);
                    if (yi instanceof Bool)
                        return Null;
                    if (!xi.equals(yi)) {
                        s[i] = yi;
                        subsChanged = true;
                    }
                }
            }
        }


        //2. anonymize the term itself if anything has changed.
        Op o = x.op();
        int dt = x.dt();
        int nextDT = !o.temporal ? dt : DTERNAL;

        if (o.temporal)
            nextDT = XTERNAL;

        if (o.temporal && s.length == 2) {


//            if (s.length s[0].equals(s[1])) {
//                s = new Term[]{s[0], s[0] /* repeated */};
//                subsChanged = true;
//            } else
            if (o.commutative) {
                if (s[0].compareTo(s[1]) > 0) { //lexical sort
                    s = new Term[]{s[1], s[0]};
                    subsChanged = true;
                }
            }
        }

        boolean dtChanging = (nextDT != dt);

        if (!subsChanged && !dtChanging) {
            return x; //no change is necessary
        }

        Compound y = compoundOrNull(
                subsChanged ? o.the(nextDT, s) : x.dt(nextDT)
        );

        return y;
    }


    @NotNull
    public Term atemporalize(@NotNull Term t) {
        return t instanceof Compound ? atemporalize((Compound) t) : t;
    }
}
