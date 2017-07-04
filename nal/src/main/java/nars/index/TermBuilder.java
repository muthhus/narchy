package nars.index;

import nars.$;
import nars.Op;
import nars.derive.meta.match.Ellipsislike;
import nars.index.term.PatternTermIndex;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Terms;
import nars.term.atom.Atomic;
import nars.term.atom.IntAtom;
import nars.term.compound.GenericCompound;
import nars.term.compound.UnitCompound1;
import nars.term.container.TermContainer;
import nars.term.container.TermVector;
import nars.term.util.InvalidTermException;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static java.util.Arrays.copyOfRange;
import static nars.Op.*;
import static nars.term.Terms.compoundOrNull;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

/**
 * Created by me on 1/2/16.
 */
public abstract class TermBuilder {



    /**
     * main entry point for compound construction - creates an immutable result
     */
    @NotNull
    public Term the(@NotNull Op op, int dt, @NotNull Term... u) throws InvalidTermException {
        return op.the(dt, u);
    }

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


    /**
     * should only be applied to subterms, not the outer-most compound
     */
    @NotNull
    public Term productNormalize(@NotNull Term u) {
        if (!(u instanceof Compound))
            return u;

        int b = u.structure();
        if (!((b & Op.InhAndIMGbits) > 0) || !((b & INH.bit) > 0) || u.varPattern() > 0)
            return u;

        Term t = u.unneg();
        boolean neg = (t != u);

        if (t.op() == INH) {
            Compound ct = (Compound) t;
            Term[] sp = ct.toArray();
            Term s = sp[0];
            Op so = s.op();
            Term p = sp[1];
            Op po = p.op();
            if (so == Op.IMGi && !po.image) {
                Compound ii = (Compound) s;
                t = the(Op.INH, ii.sub(0), imageUnwrapToProd(p, ii));
            } else if (po == Op.IMGe && !so.image) {
                Compound jj = (Compound) p;
                t = the(Op.INH, imageUnwrapToProd(s, jj), jj.sub(0));
            } else {
                return u; //original value
            }

        }

        return !neg ? t : $.neg(t);
    }

    @NotNull
    private Term imageUnwrapToProd(Term p, @NotNull Compound ii) {
        return the(Op.PROD, imageUnwrap(ii, p));
    }

    @NotNull
    public static Term[] imageUnwrap(@NotNull Compound image, Term other) {
        int l = image.size();
        Term[] t = new Term[l];
        int r = image.dt();
        @NotNull Term[] imageTerms = image.toArray();
        for (int i = 0 /* skip the first element of the image */, j = 0; j < l; ) {
            t[j++] = ((j) == r) ? other : imageTerms[++i];
        }
        return t;
    }

//    /**
//     * collection implementation of the conjunction true/false filter
//     */
//    @NotNull
//    private static Set<Term> conjTrueFalseFilter(@NotNull Set<Term> terms) {
//        Iterator<Term> ii = terms.iterator();
//        while (ii.hasNext()) {
//            Term n = ii.next();
//            if (isTrue(n))
//                ii.remove();
//            else if (isFalse(n))
//                return Collections.emptySet();
//        }
//        return terms;
//    }





    private static boolean hasImdex(@NotNull Term... r) {
        for (Term x : r) {
            //        if (t instanceof Compound) return false;
//        byte[] n = t.bytes();
//        if (n.length != 1) return false;
            if (x.equals(Imdex)) return true;
        }
        return false;
    }


    /**
     * override to possibly intern termcontainers
     */
    @NotNull
    public TermContainer subterms(@NotNull Term[] s) {
        return TermVector.the(s);
    }
    public TermContainer subterms(@NotNull TreeSet<Term> s) {
        return subterms(s.toArray(new Term[s.size()]));
    }


    protected Compound newCompound(@NotNull Op op, Term[] subterms) {
        if (!op.image && !(this instanceof PatternTermIndex) /* HACK */ && subterms.length == 1) {
            return new UnitCompound1(op, subterms[0]); //HACK avoid creating the TermContainer if possible
        }
        return newCompound(op, subterms(subterms));
    }

    /**
     * directly constructs a new instance, applied at the end.
     */
    protected Compound newCompound(@NotNull Op op, TermContainer subterms) {
        if (!op.image && !(this instanceof PatternTermIndex) /* HACK */ && subterms.size() == 1) {
            return new UnitCompound1(op, subterms.sub(0)); //HACK avoid creating the TermContainer if possible
        }


        return new GenericCompound(op, subterms);
    }


    @NotNull
    public final Term the(@NotNull Op op, @NotNull Term... tt) {
        return the(op, DTERNAL, tt);
    }







    /**
     * NOTE: terms must be sorted, if they need to be, before calling.
     */
    @NotNull
    protected Term compound(@NotNull Op op, @NotNull Term... args) {

        int s = args.length;
        assert (s != 0);


//        if (s < op.minSize) {
//            throw new RuntimeException("invalid size " + s + " for " + op);
//        }
//        //assert(s >= op.minSize);

        for (int i = 0; i < s; i++) {
            Term x = args[i];

            //x = productNormalize(x);

            if (isAbsolute(x))
                return Null; //may have become False through eval()

            if ((i == 0) && (s == 1) && (op.minSize > 1) && !(x instanceof Ellipsislike)) {
                //special case: allow for ellipsis to occupy one item even if minArity>1
                return x;
            }

            args[i] = x;
        }


        if (s == 1 && op.minSize > 1) {

            Term a0 = args[0];
            if (!(a0 instanceof Ellipsislike)) {
                //return null;
                //throw new RuntimeException("invalid size " + s + " for " + op);
                return a0; //reduction
            }
        }

        return newCompound(op, args);
        //}
    }


    @NotNull
    public Term inst(Term subj, Term pred) {
        return the(INH, the(SETe, subj), pred);
    }

    @NotNull
    public Term prop(Term subj, Term pred) {
        return the(INH, subj, the(SETi, pred));
    }

    @NotNull
    public Term instprop(@NotNull Term subj, @NotNull Term pred) {
        return the(INH, the(SETe, subj), the(SETi, pred));
    }

    @NotNull
    public static Term[] neg(@NotNull Term... modified) {
        int l = modified.length;
        Term[] u = new Term[l];
        for (int i = 0; i < l; i++) {
            u[i] = NEG.the(modified[i]);
        }
        return u;
    }



//    @NotNull
//    private Term image(@NotNull Op o, @NotNull Term... res) {
//
//        int index = DTERNAL, j = 0;
//        boolean hasPatternVar = false;
//        for (Term x : res) {
//            if (x.equals(Imdex)) {
//                assert (index == DTERNAL);
//                index = j;
//            } else if (!hasPatternVar && x.varPattern() > 0) {
//                hasPatternVar = true;
//            }
//            j++;
//        }
//
//        Term[] ser;
//        if (hasPatternVar && index == DTERNAL) {
//            ser = res;
//        } else {
//
//            if (index == DTERNAL)
//                throw new InvalidTermException(o, DTERNAL, "image missing '_' (Imdex)", res);
//
//            int serN = res.length - 1;
//            ser = new Term[serN];
//            System.arraycopy(res, 0, ser, 0, index);
//            System.arraycopy(res, index + 1, ser, index, (serN - index));
//        }
//
//        return finish(o, index, ser);
//    }


    public Term replace(@NotNull Term c, @NotNull Term x, @NotNull Term y) {
        return $.terms.replace(c, x, y);
    }





    @NotNull
    private Term compound(Op op, int dt, Term... cs) {
        return compound(op, cs).dt(dt);
    }


    /**
     * for commutive conjunction
     *
     * @param dt will be either 0 or DTERNAL (commutive relation)
     */
    public static boolean flatten(@NotNull Op op, @NotNull Term[] u, int dt, ObjectByteHashMap<Term> s) {
        for (Term x : u) {
            if (!flatten(op, dt, x, s))
                return false;
        }
        return true;
    }

    public static boolean flatten(@NotNull Op op, @NotNull TermContainer u, int dt, ObjectByteHashMap<Term> s) {
        int l = u.size();
        for (int i = 0; i < l; i++) {
            if (!flatten(op, dt, u.sub(i), s))
                return false;
        }
        return true;
    }

    public static boolean flattenMatchDT(int candidate, int target) {
        if (candidate == target) return true;
        if (target == 0 && candidate == DTERNAL)
            return true; //promote to parallel
        return false;
    }

    public static boolean flatten(@NotNull Op op, int dt, Term x, ObjectByteHashMap<Term> s) {
        Op xo = x.op();

        if ((xo == op) && flattenMatchDT(((Compound) x).dt(), dt)) {
            return flatten(op, ((Compound) x).subterms(), dt, s); //recurse
        } else {
            byte polarity;
            Term t;
            if (xo == NEG) {
                polarity = -1;
                t = x.unneg();
            } else {
                polarity = +1;
                t = x;
            }
            if (s.getIfAbsentPut(t, polarity) != polarity)
                return false; //CoNegation
        }
        return true;
    }



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
    public static Term intersect(@NotNull Op o, @NotNull Compound a, @NotNull Compound b) {
        if (a.equals(b))
            return a;

        Term[] c = TermContainer.intersect(a, b);
        return (c == null || c.length== 1) ? Null : (Compound)(o.the(c));
    }


    @NotNull
    public static Compound union(@NotNull Op o, @NotNull Compound a, @NotNull Compound b) {
        if (a.equals(b))
            return a;

        TreeSet<Term> t = new TreeSet<>();
        a.copyInto(t);
        b.copyInto(t);
        int as = a.size();
        int bs = b.size();
        int maxSize = Math.max(as, bs);
        if (t.size() == maxSize) {
            //the smaller is contained by the larger other
            return as > bs ? a : b;
        }
        return (Compound) $.the(o, t);
    }

    @NotNull
    public Term the(@NotNull Compound csrc, @NotNull Term... newSubs) {
        return the(csrc.op(), csrc.dt(), newSubs);
    }

    @NotNull
    public Term the(@NotNull Op op, int dt, @NotNull TermContainer newSubs) {
        return the(op, dt, newSubs.toArray());
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
    public Compound atemporalize(final @NotNull Compound c) {

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
                                    reflex(st.sub(0), st.sub(1))
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
                    subsChanged ? o.the(pdt, newSubs)
                            :
                            o.the(pdt, c.subterms().toArray()));
                            //c.dt(pdt)

            if (xx == null && dtChanged && pdt == DTERNAL) {
                //throw new InvalidTermException("unable to atemporalize", c);
                return compoundOrNull(
                        o.the(XTERNAL, subsChanged ? newSubs : st.toArray()) );
            }


            //if (c.isNormalized())
            //xx.setNormalized();

            //Termed exxist = get(xx, false); //early exit: atemporalized to a concept already, so return
            //if (exxist!=null)
            //return exxist.term();


            //x = i.the(xx).term();
            return xx;
        } else {
            return c;
        }
    }

    private static boolean reflex(@NotNull Term sub0, @NotNull Term sub1) {
        sub0 = sub0.unneg();
        sub1 = sub1.unneg();
        return sub0.equals(sub1) ||
                sub0.containsRecursively(sub1) ||
                sub1.containsRecursively(sub0);
    }


    public Term the(@NotNull Op op, int dt, Collection<Term> sub) {
        int ss = sub.size();
        return the(op, dt, sub.toArray(new Term[ss]));
    }


}
