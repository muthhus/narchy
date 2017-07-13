package nars.index;

import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Terms;
import nars.term.atom.Atomic;
import nars.term.atom.IntAtom;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

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

    @NotNull
    public Term the(@NotNull Op op, int dt, @NotNull TermContainer newSubs) {
        return op.the(dt, newSubs.toArray());
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

//        if (o instanceof Byte) return the(o.intValue());
//        if (o instanceof Short) return the(o.intValue());
        if (o instanceof Integer) return IntAtom.the(o.intValue());
//
//        if (o instanceof Long) {
//            if (((int) o) == o.longValue())
//                return the(o.intValue());
//            else
//                return Atomic.the(Long.toString((long) o));
//        }
//
//        if ((o instanceof Float) || (o instanceof Double))
//            return the(o.floatValue());

        return Atomic.the(o.toString());
    }


    @NotNull
    public Term atemporalize(final @NotNull Compound c) {

        if (!c.isTemporal())
            return c;

        Term[] newSubs = null; //oldSubs.clone();

        Op o = c.op();
        int cdt = c.dt();
        int pdt = !o.temporal ? cdt : DTERNAL; //( !o.concurrent(cdt) ? XTERNAL : DTERNAL); //preserve image dt


        TermContainer st = c.subterms();
        int sts = st.size();
        if (st.hasAny(Op.TemporalBits)) {

            //atemporalize subterms first

            boolean subsChanged = false;
            int cs = sts;
            newSubs = new Term[cs];
            for (int i = 0; i < cs; i++) {

                Term x = st.sub(i), y;
                if (x instanceof Compound) {
                    subsChanged |= (x != (y = atemporalize((Compound) x)));
                } else {
                    y = x;
                }

                newSubs[i] = y;

            }

            if (!subsChanged)
                newSubs = null;
        }

//        //resolve XTERNAL temporals to lexical order //TODO does this even matter?
//        if (pdt == XTERNAL && cs == 2*/) {
//            if (newSubs[0].compareTo(newSubs[1]) > 0) {
//                newSubs = (newSubs == oldSubs) ? newSubs.clone() : newSubs;
//                Term x = newSubs[0];
//                newSubs[0] = newSubs[1];
//                newSubs[1] = x;
//            }
//        }


        boolean dtChanging = (pdt != cdt);
        boolean subsChanged = (newSubs != null) && !st.equalTerms(newSubs);
//        if (dtChanged && pdt == XTERNAL) {
//            if (newSubs == null) {
//                newSubs = st.toArray(new Term[sts], 0, sts);
//            }
//            Arrays.sort(newSubs);
//            subsChanged = !st.equalTerms(newSubs);
//        } else {

        //   }

        if (subsChanged || dtChanging) {

            if (o.temporal) {// && newSubs[0].unneg().equals(newSubs[1].unneg())  //preserve co-negation

                //introduce an XTERNAL temporal placeholder in the following conditions
                if ((subsChanged && newSubs.length == 1) //it was a repeat which collapsed, so use XTERNAL and repeat the subterm
                        ||
                        (sts == 2 &&

                                //repeat or non-lexical ordering for commutive compound; must re-arrange
                                (o.commutative && (st.sub(0).compareTo(st.sub(1)) >= 0))

                                ||

                                Terms.reflex(st.sub(0), st.sub(1))))


                // && newSubs[0].unneg().equals(newSubs[1].unneg())  //preserve co-negation
                {


                    pdt = DTERNAL;
                    if (!dtChanging)
                        dtChanging = pdt != cdt; //in case that now the dt has changed

                    newSubs = new Term[]{st.sub(0), st.sub(sts > 1 ? 1 : 0)};
                    if (o.commutative)
                        Arrays.sort(newSubs);

                    if (!subsChanged)
                        subsChanged = newSubs.length != sts || !st.equalTerms(newSubs);

                }
            }
        }

        if (!subsChanged && !dtChanging) {
            return c;
        }


        Compound xx = compoundOrNull(
                o.the(pdt, subsChanged ?
                        newSubs
                        :
                        c.toArray()
                )
        );


        if (xx == null && pdt == DTERNAL) {

            //as a last resort, use the XTERNAL form to allow it
            //TODO decide if all commutive temporal concepts (&&, <=>) should be named by their raw XTERNAL form

            xx = compoundOrNull(
                    Op.compound(o, XTERNAL, subsChanged ? Op.subterms(newSubs) : st));
        }

        if (xx == null)
            return Null; //failed to atemporalize
        else
            return xx;
    }


    @NotNull
    public Term atemporalize(@NotNull Term t) {
        return t instanceof Compound ? atemporalize((Compound) t) : t;
    }
}
