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
import java.util.Collection;

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

        TermContainer st = c.subterms();
        Term[] newSubs = null; //oldSubs.clone();

        Op o = c.op();
        int cdt = c.dt();
        int pdt = !o.temporal ? cdt : DTERNAL; //( !o.concurrent(cdt) ? XTERNAL : DTERNAL); //preserve image dt
        int sts = st.size();
        if (st.hasAny(Op.TemporalBits)) {

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


        boolean dtChanged = (pdt != cdt);
        boolean subsChanged;
//        if (dtChanged && pdt == XTERNAL) {
//            if (newSubs == null) {
//                newSubs = st.toArray(new Term[sts], 0, sts);
//            }
//            Arrays.sort(newSubs);
//            subsChanged = !st.equalTerms(newSubs);
//        } else {
        subsChanged = newSubs != null && !st.equalTerms(newSubs);
        //   }

        if (subsChanged || dtChanged) {

            if (o.temporal && (
                    (subsChanged && newSubs.length == 1) //it was a repeat which collapsed, so use XTERNAL and repeat the subterm
                            ||
                            (sts == 2 &&
                                    Terms.reflex(st.sub(0), st.sub(1))
                            ))// && newSubs[0].unneg().equals(newSubs[1].unneg())  //preserve co-negation
                    ) {


                pdt = XTERNAL;

                newSubs = new Term[]{st.sub(0), st.sub(st.size() > 1 ? 1 : 0)};
                if (o.commutative)
                    Arrays.sort(newSubs);
                subsChanged = true;
            }/* else {
                if (o.temporal)
                    pdt = DTERNAL;
            }*/


//            if (o.temporal && newSubs!=null && newSubs.size() == 1) {
//                System.out.println("?");
//            }

            Compound xx = compoundOrNull(
                    Op.compound(o, pdt, subsChanged ?
                        Op.subterms(newSubs)
                            :
                        c.subterms()  //o.the(pdt, c.subterms().toArray()));
                    )
            );

            if (xx == null) {
                if (dtChanged) {
                    if (pdt == DTERNAL) {
                        //throw new InvalidTermException("unable to atemporalize", c);
                        @Nullable Compound x = compoundOrNull(
                                Op.compound(o, XTERNAL, subsChanged ? Op.subterms(newSubs) : st));
                        return x != null ? x : Null;
                    }
                }
            }


            //if (c.isNormalized())
            //xx.setNormalized();

            //Termed exxist = get(xx, false); //early exit: atemporalized to a concept already, so return
            //if (exxist!=null)
            //return exxist.term();


            //x = i.the(xx).term();
            if (xx == null)
                return Null;
                //throw new NullPointerException();

            return xx;
        } else {
            return c;
        }
    }


    public Term the(@NotNull Op op, int dt, Collection<Term> sub) {
        int ss = sub.size();
        @NotNull Term[] u = sub.toArray(new Term[ss]);
        return op.the(dt, u);
    }


}
