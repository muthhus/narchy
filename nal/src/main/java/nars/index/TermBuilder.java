package nars.index;

import nars.$;
import nars.Op;
import nars.derive.meta.match.Ellipsislike;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Terms;
import nars.term.atom.Atomic;
import nars.term.atom.AtomicSingleton;
import nars.term.compound.GenericCompound;
import nars.term.container.TermContainer;
import nars.term.container.TermVector;
import nars.term.util.InvalidTermException;
import nars.term.var.Variable;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.primitive.ObjectBytePair;
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


    private static final TermContainer InvalidSubterms = TermVector.the(False);

    private static final int InvalidEquivalenceTerm = or(IMPL, EQUI);
    private static final int InvalidImplicationSubj = or(EQUI, IMPL);
    private static final int InvalidImplicationPred = or(EQUI);


    /**
     * main entry point for compound construction - creates an immutable result
     */
    @NotNull
    public Term the(@NotNull Op op, int dt, @NotNull Term... u) throws InvalidTermException {


        int arity = u.length;
        switch (op) {
//            case INT:
//            case INTRANGE:
//                System.out.println(op + " " + dt + " " + Arrays.toString(u));
//                break;

            case NEG:
                if (arity != 1)
                    throw new InvalidTermException(op, dt, "negation requires 1 subterm", u);

                return neg(u[0]);

//            case INTRANGE:
//                System.err.println("intRange: " + Arrays.toString(u));
//                break;

            case INSTANCE:
                if (arity != 2 || dt != DTERNAL) throw new InvalidTermException(INSTANCE, dt, "needs 2 arg", u);
                return inst(u[0], u[1]);
            case PROPERTY:
                if (arity != 2 || dt != DTERNAL) throw new InvalidTermException(PROPERTY, dt, "needs 2 arg", u);
                return prop(u[0], u[1]);
            case INSTANCE_PROPERTY:
                if (arity != 2 || dt != DTERNAL)
                    throw new InvalidTermException(INSTANCE_PROPERTY, dt, "needs 2 arg", u);
                return instprop(u[0], u[1]);


            case DISJ:
                if (dt != DTERNAL)
                    throw new InvalidTermException(op, dt, "Disjunction must be DTERNAL", u);
                return disjunction(u);
            case CONJ:
                return conj(dt(dt), u);

            case IMGi:
            case IMGe:
                //if no relation was specified and it's an Image,
                //it must contain a _ placeholder

                if ((arity < 1) || (dt > arity))
                    throw new InvalidTermException(op, dt, "image requires size=2 excluding _ imdex", u);

                if (hasImdex(u)) {
                    return image(op, u);
                }

                if ((dt < 0) && !(u[0].varPattern() > 0 || u[1].varPattern() > 0))
                    throw new InvalidTermException(op, dt, "Invalid Image", u);


                break; //construct below


            case DIFFe:
            case DIFFi:
                return newDiff(op, u);
            case SECTe:
                return newIntersection(u,
                        SECTe,
                        SETe,
                        SETi);
            case SECTi:
                return newIntersection(u,
                        SECTi,
                        SETi,
                        SETe);

            case EQUI:
            case IMPL:
                dt = dt(dt);
                //fall-through:
            case INH:
            case SIM:
//                if (arity == 1)
//                    return True;
                if (arity != 2)
                    throw new InvalidTermException(op, dt, "Statement without exactly 2 arguments", u);
                return statement(op, dt, u[0], u[1]);

            case PROD:
                return (arity != 0) ? compound(op, dt, u) : Terms.ZeroProduct;

        }


        return finish(op, dt, u);
    }


    /**
     * dt pre-filter
     */
    protected int dt(int dt) {
        return dt;
    }

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

        return !neg ? t : neg(t);
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


    /**
     * array implementation of the conjunction true/false filter
     */
    @NotNull
    private static Term[] conjTrueFalseFilter(@NotNull Term... u) {
        int trues = 0; //# of True subterms that can be eliminated
        for (Term x : u) {
            if (isTrue(x)) {
                trues++;
            } else if (isFalse(x)) {

                //false subterm in conjunction makes the entire condition false
                //this will eventually reduce diectly to false in this method's only callee HACK
                return FalseArray;
            }
        }

        if (trues == 0)
            return u;

        int ul = u.length;
        if (ul == trues)
            return TrueArray; //reduces to an Imdex itself

        Term[] y = new Term[ul - trues];
        int j = 0;
        for (int i = 0; j < y.length; i++) {
            Term uu = u[i];
            if (!isTrue(uu)) // && (!uu.equals(False)))
                y[j++] = uu;
        }

        assert (j == y.length);

        return y;
    }


    private static boolean validEquivalenceTerm(@NotNull Term t) {
        //return !t.opUnneg().in(InvalidEquivalenceTerm);
        return !t.hasAny(InvalidEquivalenceTerm);
//        if ( instanceof Implication) || (subject instanceof Equivalence)
//                || (predicate instanceof Implication) || (predicate instanceof Equivalence) ||
//                (subject instanceof CyclesInterval) || (predicate instanceof CyclesInterval)) {
//            return null;
//        }
    }

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
    public TermContainer intern(@NotNull Term[] s) {
        return TermVector.the(s);
    }

    protected Compound newCompound(@NotNull Op op, int dt, Term[] subterms) {
//        if (subterms.length==1 && subterms[0].vars() == 0) {
//            return new UnitCompound1(op, subterms[0]); //HACK avoid creating the TermContainer if possible
//        }
        return newCompound(op, dt, intern(subterms));
    }

    /**
     * directly constructs a new instance, applied at the end.
     */
    protected Compound newCompound(@NotNull Op op, int dt, TermContainer subterms) {
        return new GenericCompound(op, dt, subterms);
    }


    @NotNull
    public final Term the(@NotNull Op op, @NotNull Term... tt) {
        return the(op, DTERNAL, tt);
    }


    @NotNull
    private Term newDiff(@NotNull Op op, @NotNull Term... t) {

        //corresponding set type for reduction:
        Op set = op == DIFFe ? SETe : SETi;

        switch (t.length) {
            case 1:
                Term t0 = t[0];
                return t0 instanceof Ellipsislike ?
                        finish(op, t0) :
                        False;
            case 2:
                Term et0 = t[0], et1 = t[1];
                if (et0.equals(et1) || et0.containsRecursively(et1) || et1.containsRecursively(et0))
                    return False;
                else if ((et0.op() == set && et1.op() == set))
                    return difference(set, (Compound) et0, (Compound) et1);
                else
                    return finish(op, t);
            default:
                throw new InvalidTermException(op, t, "diff requires 2 terms");
        }
    }


    @NotNull
    private Term finish(@NotNull Op op, @NotNull Term... args) {
        return finish(op, DTERNAL, args);
    }

    @NotNull
    private Term finish(@NotNull Op op, int dt, @NotNull Term... args) {
        return finish(TermContainer.mustSortAndUniquify(op, dt, args.length), op, dt, args);
    }

    @NotNull
    private Term finish(boolean sort, @NotNull Op op, int dt, @NotNull Term... args) {
        if (sort) {
            args = Terms.sorted(args);
        }
        return compound(op, dt, args);
    }

    @NotNull
    private Term finalize(@NotNull Op op, int dt, @NotNull Set<Term> args) {
        return compound(op, dt, Terms.sorted(args));
    }

    @NotNull
    private Term finalize(@NotNull Op op, @NotNull Set<Term> args) {
        return finalize(op, DTERNAL, args);
    }


    @NotNull
    private Term finalize(@NotNull Op op, @NotNull Term... args) {
        return compound(op, DTERNAL, args);
    }

    /**
     * NOTE: terms must be sorted, if they need to be, before calling.
     */
    @NotNull
    protected Term compound(@NotNull Op op, int dt, @NotNull Term... args) {

        int s = args.length;
        assert (s != 0);


//        if (s < op.minSize) {
//            throw new RuntimeException("invalid size " + s + " for " + op);
//        }
//        //assert(s >= op.minSize);

        for (int i = 0; i < s; i++) {
            Term x = args[i];

            x = productNormalize(x);

            if (isTrueOrFalse(x))
                return False; //may have become False through eval()

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

        return newCompound(op, dt, args);
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
    private Term[] neg(@NotNull Term... modified) {
        int l = modified.length;
        Term[] u = new Term[l];
        for (int i = 0; i < l; i++) {
            u[i] = neg(modified[i]);
        }
        return u;
    }

    @NotNull
    public final Term neg(@NotNull Term x) {

        if (x instanceof Compound) {
            // (--,(--,P)) = P
            if (x.op() == NEG)
                return x.unneg();
        } else if (x instanceof AtomicSingleton) {
            if (isFalse(x)) return True;
            if (isTrue(x)) return False;
        }

        Term y = compound(NEG, DTERNAL, x);
        if (y instanceof Compound && x.isNormalized()) {
            ((Compound) y).setNormalized(); //share normalization state
        }
        return y;
    }


    @NotNull
    private Term image(@NotNull Op o, @NotNull Term... res) {

        int index = DTERNAL, j = 0;
        boolean hasPatternVar = false;
        for (Term x : res) {
            if (x.equals(Imdex)) {
                assert (index == DTERNAL);
                index = j;
            } else if (!hasPatternVar && x.varPattern() > 0) {
                hasPatternVar = true;
            }
            j++;
        }

        Term[] ser;
        if (hasPatternVar && index == DTERNAL) {
            ser = res;
        } else {

            if (index == DTERNAL)
                throw new InvalidTermException(o, DTERNAL, "image missing '_' (Imdex)", res);

            int serN = res.length - 1;
            ser = new Term[serN];
            System.arraycopy(res, 0, ser, 0, index);
            System.arraycopy(res, index + 1, ser, index, (serN - index));
        }

        return finish(o, index, ser);
    }

    @NotNull
    private Term conj(int dt, final @NotNull Term... uu) {

        Term[] u = conjTrueFalseFilter(uu);

        final int n = u.length;
        if (n == 0)
            return False;

        if (n == 1) {
            Term only = u[0];

            //preserve unitary ellipsis for patterns etc
            return only instanceof Ellipsislike ?
                    finish(CONJ, dt, only) :
                    only;

        }

        if (dt == XTERNAL) {
            if (n != 2)
                throw new InvalidTermException(CONJ, XTERNAL, "XTERNAL only applies to 2 subterms, as dt placeholder", u);

            if (u[0].equals(u[1]))
                return u[0];

            //preserve grouping (don't flatten) but use normal commutive ordering as dternal &&
            return finish(CONJ, XTERNAL, u);
        }

        boolean commutive = concurrent(dt);
        if (commutive) {

            return junctionFlat(dt, u);

        } else {
            //NON-COMMUTIVE

            assert (n == 2);

            Term a = u[0];
            Term b = u[1];
            boolean equal = a.equals(b);
            if (equal) {
                if (dt < 0) {
                    //make dt positive to avoid creating both (x &&+1 x) and (x &&-1 x)
                    dt = -dt;
                }
            } else {
                if (a.compareTo(b) > 0) {
                    //ensure lexicographic ordering

                    Term x = u[0];
                    u[0] = u[1];
                    u[1] = x; //swap
                    dt = -dt; //and invert time
                }
            }

            return finish(false /* already sorted */, CONJ, dt, u);

        }

    }


    /**
     * flattening conjunction builder, for (commutive) multi-arg conjunction and disjunction (dt == 0 ar DTERNAL)
     * see: https://en.wikipedia.org/wiki/Boolean_algebra#Monotone_laws
     */
    @NotNull
    private Term junctionFlat(int dt, @NotNull Term... u) {

        //TODO if there are no negations in u then an accelerated construction is possible

        assert (u.length > 0 && dt == 0 || dt == DTERNAL); //throw new RuntimeException("should only have been called with dt==0 or dt==DTERNAL");

        ObjectByteHashMap<Term> s = new ObjectByteHashMap<>(u.length * 2);

        if (flatten(CONJ, u, dt, s) && !s.isEmpty()) {
            Set<Term> cs = junctionGroupNonDTSubterms(s, dt);
            if (!cs.isEmpty()) {


                //annihilate common terms inside and outside of disjunction
                //      ex:
                //          -X &&  ( X ||  Y)
                //          -X && -(-X && -Y)  |-   -X && Y
                Iterator<Term> csi = cs.iterator();
                List<Term> csa = null;
                while (csi.hasNext()) {
                    Term x = csi.next();

                    if (x.op() == NEG && x.subIs(0, CONJ)) { //DISJUNCTION
                        Compound disj = (Compound) x.unneg();
                        Set<Term> disjSubs = disj.toSet();
                        //factor out occurrences of the disj's contents outside the disjunction, so remove from inside it
                        if (disjSubs.removeAll(cs)) {
                            //reconstruct disj if changed
                            csi.remove();

                            if (!disjSubs.isEmpty()) {
                                Term y = neg(the(CONJ, disj.dt(), disjSubs));
                                if (csa == null)
                                    csa = $.newArrayList(1);
                                csa.add(y);
                            }
                        }
                    }
                }
                if (csa != null)
                    cs.addAll(csa);

                return finalize(CONJ, dt, cs);
            }
        }

        return False;
    }


    /**
     * this is necessary to keep term structure consistent for intermpolation.
     * by grouping all non-sequence subterms into its own subterm, future
     * flattening and intermpolation is prevented from destroying temporal
     * measurements.
     *
     * @param innerDT will either 0 or DTERNAL (commutive relation)
     */
    private @NotNull Set<Term> junctionGroupNonDTSubterms(@NotNull ObjectByteHashMap<Term> s, int innerDT) {

        Set<Term> outer = new HashSet(s.size());

        for (ObjectBytePair<Term> xn : s.keyValuesView()) {
            Term x = xn.getOne();
            outer.add((xn.getTwo() < 0) ? neg(x) : x);
        }
        return outer;

//            if (isTrue(x)) {
//                ss.remove();
//            } else if (isFalse(x)) {
//                return Collections.emptySet();
//            } else {
//                switch (x.op()) {
//                    case CONJ:
//                        // dt will be something other than 'innerDT' having just been flattened
//                        toOuter = x;
//                        ss.remove();
//                        break;
//                    case NEG:
//                        Compound n = (Compound) x;
//                        Term nn = n.sub(0);
//                        if (nn.op() == CONJ) {
//                            Compound cnn = ((Compound) nn);
//                            int dt = cnn.dt();
//                            if (dt == innerDT) {
//                                //negating unwrap each subterm of the negated conjunction to the outer level of compatible 'dt'
//                                int cnns = cnn.size();
//                                for (int i = 0; i < cnns; i++) {
//                                    Term cnt = cnn.sub(i);
//                                    if (s.contains(cnt)) {
//                                        //co-negation detected
//                                        return Collections.emptySet();
//                                    }
//                                    toOuter = neg(cnt);
//                                }
//                                ss.remove();
//                            }
//                        }
//                        break;
//                }
//            }
//            if (toOuter != null) {
//                if (outer == null) outer = new UnifiedSet(2);
//                outer.add(x);
//            }
//
//        if (outer != null) { //something changed
//            int sts = s.size();
//            if (sts > 0) {
//                Term[] sa = s.toArray(new Term[sts]);
//
//                Term next = (sa.length == 1) ? sa[0] : the(CONJ, innerDT, sa);
//                if (next == null)
//                    return Collections.emptySet();
//
//                outer.add(next);
//            }
//            return outer;
//        } else {
//            return s;
//        }

    }

    /**
     * for commutive conjunction
     *
     * @param dt will be either 0 or DTERNAL (commutive relation)
     */
    private static boolean flatten(@NotNull Op op, @NotNull Term[] u, int dt, ObjectByteHashMap<Term> s) {
        for (Term x : u) {
            if (!flatten(op, dt, x, s))
                return false;
        }
        return true;
    }

    private static boolean flatten(@NotNull Op op, @NotNull TermContainer u, int dt, ObjectByteHashMap<Term> s) {
        int l = u.size();
        for (int i = 0; i < l; i++) {
            if (!flatten(op, dt, u.sub(i), s))
                return false;
        }
        return true;
    }

    private static boolean flattenMatchDT(int candidate, int target) {
        if (candidate == target) return true;
        if (target == 0 && candidate == DTERNAL)
            return true; //promote to parallel
        return false;
    }

    private static boolean flatten(@NotNull Op op, int dt, Term x, ObjectByteHashMap<Term> s) {
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


    @NotNull
    private Term statement(@NotNull Op op, int dt, @NotNull Term subject, @NotNull Term predicate) {


        switch (op) {

            case SIM:

                if (subject.equals(predicate))
                    return True;
                if (isTrue(subject) || isFalse(subject) || isTrue(predicate) || isFalse(predicate))
                    return False;
                break;

            case INH:

                if (subject.equals(predicate)) //equal test first to allow, ex: False<->False to result in True
                    return True;
                if (isTrueOrFalse(subject) || isTrueOrFalse(predicate))
                    return False;

                boolean sNeg = subject.op() == NEG;
                boolean pNeg = predicate.op() == NEG;
                if (sNeg && pNeg) {
                    subject = subject.unneg();
                    predicate = predicate.unneg();
                } else if (sNeg && !pNeg) {
                    return neg(statement(op, dt, subject.unneg(), predicate)); //TODO loop and not recurse, needs negation flag to be applied at the end before returning
                } else if (pNeg && !sNeg) {
                    return neg(statement(op, dt, subject, predicate.unneg()));
                }

                break;


            case EQUI:

                //if (isTrue(subject)) return predicate;
                //if (isTrue(predicate)) return subject;
                //if (isFalse(subject)) return neg(predicate);
                //if (isFalse(predicate)) return neg(subject);
                if (isTrue(subject) || isFalse(subject) || isTrue(predicate) || isFalse(predicate))
                    return False;

                if (!validEquivalenceTerm(subject))
                    throw new InvalidTermException(op, dt, "Invalid equivalence subject", subject, predicate);
                if (!validEquivalenceTerm(predicate))
                    throw new InvalidTermException(op, dt, "Invalid equivalence predicate", subject, predicate);

                boolean subjNeg = subject.op() == NEG;
                boolean predNeg = predicate.op() == NEG;
                if (subjNeg && predNeg) {
                    subject = subject.unneg();
                    predicate = predicate.unneg();
                } else if (!subjNeg && predNeg) {
                    //factor out (--, ...)
                    return neg(statement(op, dt, subject, predicate.unneg()));
                } else if (subjNeg && !predNeg) {
                    //factor out (--, ...)
                    return neg(statement(op, dt, subject.unneg(), predicate));
                }

                if (dt == XTERNAL) {
                    //create as-is
                    return compound(op, XTERNAL, subject, predicate);
                } else {
                    boolean equal = subject.equals(predicate);
                    if (concurrent(dt)) {
                        if (equal) {
                            return True;
                        }
                    } else {
                        if (dt < 0 && equal) {
                            dt = -dt; //use only the forward direction on a repeat
                        }
                    }
                }

                break;

            case IMPL:

                //special case for implications: reduce to --predicate if the subject is False
                if (isTrue(subject /* antecedent */)) {
                    if (concurrent(dt) || dt == XTERNAL)
                        return predicate; //special case for implications: reduce to predicate if the subject is True
                    else {
                        return False; //no temporal basis
                    }
                }
                if (isFalse(subject))
                    return False;
                if (isTrueOrFalse(predicate /* consequence */))
                    return False;
                if (subject.hasAny(InvalidImplicationSubj))
                    return False; //throw new InvalidTermException(op, dt, "Invalid equivalence subject", subject, predicate);
                if (predicate.hasAny(InvalidImplicationPred))
                    return False; //throw new InvalidTermException(op, dt, "Invalid equivalence predicate", subject, predicate);


                if (predicate.op() == NEG) {
                    //negated predicate gets unwrapped to outside
                    return neg(the(op, dt, subject, predicate.unneg()));
                }

                if (dt == XTERNAL) {
                    //create as-is
                    return compound(op, XTERNAL, subject, predicate);
                } else {
                    if (concurrent(dt)) {
                        if (subject.equals(predicate))
                            return True;
                    } //else: allow repeat
                }


                // (C ==>+- (A ==>+- B))   <<==>>  ((C &&+- A) ==>+- B)
                if (dt != XTERNAL && predicate.op() == IMPL) {
                    Compound cpr = (Compound) predicate;
                    int cprDT = cpr.dt();
                    if (cprDT != XTERNAL) {
                        Term a = cpr.sub(0);

                        subject = conj(dt, subject, a);
                        predicate = cpr.sub(1);
                        return statement(IMPL, cprDT, subject, predicate);
                    }
                }


                break;
        }


        //factor out any common subterms iff concurrent
        if (concurrent(dt)) {

//            if (subject.contains(predicate) || predicate.contains(subject)) //first layer only, not recursively
//                return False; //cyclic

            if (subject.varPattern() == 0 && predicate.varPattern() == 0 &&
                    !(subject instanceof Variable) && !(predicate instanceof Variable) &&
                    (subject.containsRecursively(predicate) || predicate.containsRecursively(subject))) //first layer only, not recursively
                return False; //cyclic

            if ((op == IMPL || op == EQUI)) { //TODO verify this works as it should


                boolean subjConj = subject.op() == CONJ && concurrent(((Compound) subject).dt());
                boolean predConj = predicate.op() == CONJ && concurrent(((Compound) predicate).dt());
                if (subjConj && !predConj) {
                    final Compound csub = (Compound) subject;
                    TermContainer subjs = csub.subterms();
                    if (csub.contains(predicate)) {
                        Term finalPredicate = predicate;
                        subject = the(CONJ, csub.dt(), subjs.asFiltered(z -> z.equals(finalPredicate)).toArray());
                        predicate = False;
                        return statement(op, dt, subject, predicate);
                    }
                } else if (predConj && !subjConj) {
                    final Compound cpred = (Compound) predicate;
                    TermContainer preds = cpred.subterms();
                    if (cpred.contains(subject)) {
                        Term finalSubject = subject;
                        predicate = the(CONJ, cpred.dt(), preds.asFiltered(z -> z.equals(finalSubject)).toArray());
                        subject = False;
                        return statement(op, dt, subject, predicate);
                    }

                } else if (subjConj && predConj) {
                    final Compound csub = (Compound) subject;
                    TermContainer subjs = csub.subterms();
                    final Compound cpred = (Compound) predicate;
                    TermContainer preds = cpred.subterms();

                    MutableSet<Term> common = TermContainer.intersect(subjs, preds);
                    if (common != null && !common.isEmpty()) {

                        @NotNull Set<Term> sss = subjs.toSet();
                        if (sss.removeAll(common)) {
                            int s0 = sss.size();
                            switch (s0) {
                                case 0:
                                    subject = False;
                                    break;
                                case 1:
                                    subject = sss.iterator().next();
                                    break;
                                default:
                                    subject = the(CONJ, csub.dt(), sss);
                                    break;
                            }
                        }

                        @NotNull Set<Term> ppp = preds.toSet();
                        if (ppp.removeAll(common)) {
                            int s0 = ppp.size();
                            switch (s0) {
                                case 0:
                                    predicate = False;
                                    break;
                                case 1:
                                    predicate = ppp.iterator().next();
                                    break;
                                default:
                                    predicate = the(CONJ, cpred.dt(), ppp);
                                    break;
                            }
                        }

                        return statement(op, dt, subject, predicate);
                    }
                }
            }
        }

//            if (op == INH || op == SIM || dt == 0 || dt == DTERNAL) {
//                if ((subject instanceof Compound && subject.varPattern() == 0 && subject.containsRecursively(predicate)) ||
//                        (predicate instanceof Compound && predicate.varPattern() == 0 && predicate.containsRecursively(subject))) {
//                    return False; //self-reference
//                }
//            }

        if (op.commutative) {

            //normalize co-negation
            boolean sn = subject.op() == NEG;
            boolean pn = predicate.op() == NEG;

            if ((sn == pn) && (subject.compareTo(predicate) > 0)) {
                Term x = predicate;
                predicate = subject;
                subject = x;
                if (!concurrent(dt))
                    dt = -dt;
            }

            //System.out.println( "\t" + subject + " " + predicate + " " + subject.compareTo(predicate) + " " + predicate.compareTo(subject));

        }

        return compound(op, dt, subject, predicate); //use the calculated ordering, not the TermContainer default for commutives


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
    private Term newIntersection(@NotNull Term[] t, @NotNull Op intersection, @NotNull Op setUnion, @NotNull Op setIntersection) {

        int trues = 0;
        for (Term x : t) {
            if (isTrue(x)) {
                //everything intersects with the "all", so remove this TRUE below
                trues++;
            } else if (isFalse(x)) {
                return False;
            }
        }
        if (trues > 0) {
            if (trues == t.length) {
                return True; //all were true
            } else if (t.length - trues == 1) {
                //find the element which is not true and return it
                for (Term x : t) {
                    if (!isTrue(x))
                        return x;
                }
            } else {
                //filter the True statements from t
                Term[] t2 = new Term[t.length - trues];
                int yy = 0;
                for (Term x : t) {
                    if (!isTrue(x))
                        t2[yy++] = x;
                }
                t = t2;
            }
        }

        switch (t.length) {

            case 1:

                Term single = t[0];
                return single instanceof Ellipsislike ?
                        finish(intersection, single) :
                        single;

            case 2:
                return newIntersection2(t[0], t[1], intersection, setUnion, setIntersection);
            default:
                //HACK use more efficient way
                Term a = newIntersection2(t[0], t[1], intersection, setUnion, setIntersection);

                Term b = newIntersection(copyOfRange(t, 2, t.length), intersection, setUnion, setIntersection);

                return newIntersection2(a, b,
                        intersection, setUnion, setIntersection
                );
        }

    }

    @NotNull
    @Deprecated
    private Term newIntersection2(@NotNull Term term1, @NotNull Term term2, @NotNull Op intersection, @NotNull Op setUnion, @NotNull Op setIntersection) {

        if (term1.equals(term2))
            return term1;

        Op o1 = term1.op();
        Op o2 = term2.op();

        if ((o1 == setUnion) && (o2 == setUnion)) {
            //the set type that is united
            return union(setUnion, (Compound) term1, (Compound) term2);
        }


        if ((o1 == setIntersection) && (o2 == setIntersection)) {
            //the set type which is intersected
            return intersect(setIntersection, (Compound) term1, (Compound) term2);
        }

        if (o2 == intersection && o1 != intersection) {
            //put them in the right order so everything fits in the switch:
            Term x = term1;
            term1 = term2;
            term2 = x;
            o2 = o1;
            o1 = intersection;
        }

        //reduction between one or both of the intersection type

        Term[] args;
        if (o1 == intersection) {
            args = ArrayUtils.addAll(
                    ((TermContainer) term1).toArray(),
                    o2 == intersection ? ((TermContainer) term2).toArray() : new Term[]{term2}
            );
        } else {
            args = new Term[]{term1, term2};
        }

        return finalize(intersection, Terms.sorted(args));
    }


    @NotNull
    public Term intersect(@NotNull Op o, @NotNull Compound a, @NotNull Compound b) {
        if (a.equals(b))
            return a;

        MutableSet<Term> s = TermContainer.intersect(
                /*(TermContainer)*/ a, /*(TermContainer)*/ b
        );
        return s == null || s.isEmpty() ? False : (Compound) finalize(o, s);
    }


    @NotNull
    public Compound union(@NotNull Op o, @NotNull Compound a, @NotNull Compound b) {
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
        return (Compound) finalize(o, t);
    }

    @NotNull
    public Term the(@NotNull Compound csrc, @NotNull Term... newSubs) {
        return the(csrc.op(), csrc.dt(), newSubs);
    }

    @NotNull
    public Term the(@NotNull Op op, int dt, @NotNull TermContainer newSubs) {
        return the(op, dt, newSubs.toArray());
    }

    @NotNull
    private Term the(@NotNull Compound csrc, @NotNull Collection<Term> newSubs) {
        return the(csrc.op(), csrc.dt(), newSubs.toArray(new Term[newSubs.size()]));
    }

    public final Term disjunction(@NotNull Term... u) {
        return neg(conj(DTERNAL, neg(u)));
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

    public Atomic the(Number o) {
        if (o instanceof Byte) return the(o.intValue());
        if (o instanceof Short) return the(o.intValue());
        if (o instanceof Integer) return the(o.intValue());

        if (o instanceof Long) {
            if (((int) o) == o.longValue())
                return the(o.intValue()); //use the integer form since it will be IntTerm
            else
                return Atomic.the(Long.toString((long) o));
        }

        if ((o instanceof Float) || (o instanceof Double))
            return the(o.floatValue());

        return Atomic.the(o.toString());
    }


    @NotNull
    public Compound atemporalize(final @NotNull Compound c) {

        if (!c.isTemporal())
            return c;

        TermContainer st = c.subterms();
        Term[] oldSubs = st.toArray();
        Term[] newSubs = oldSubs;

        Op o = c.op();
        int pdt = c.dt();
        if (st.hasAny(Op.TemporalBits)) {

            boolean subsChanged = false;
            int cs = oldSubs.length;
            Term[] maybeNewSubs = new Term[cs];
            for (int i = 0; i < cs; i++) {

                Term x = oldSubs[i], y;
                if (x instanceof Compound) {
                    subsChanged |= (x != (y = atemporalize((Compound) x)));
                } else {
                    y = x;
                }

                maybeNewSubs[i] = y;

            }

            if (subsChanged)
                newSubs = maybeNewSubs;
        }

        //resolve XTERNAL temporals to lexical order
        if (pdt == XTERNAL /*&& cs == 2*/) {
            if (newSubs[0].compareTo(newSubs[1]) > 0) {
                newSubs = (newSubs == oldSubs) ? newSubs.clone() : newSubs;
                Term x = newSubs[0];
                newSubs[0] = newSubs[1];
                newSubs[1] = x;
            }
        }


        boolean dtChanged = (pdt != DTERNAL && o.temporal);
        boolean subsChanged = (newSubs != oldSubs);

        if (subsChanged || dtChanged) {

            if (subsChanged && o.temporal && newSubs.length == 1) {
                //it was a repeat which collapsed, so use XTERNAL and repeat the subterm

                if (pdt != DTERNAL)
                    pdt = XTERNAL;

                Term s = newSubs[0];
                newSubs = new Term[]{s, s};
            } else {
                if (o.temporal)
                    pdt = DTERNAL;
            }
//            if (o.temporal && newSubs!=null && newSubs.size() == 1) {
//                System.out.println("?");
//            }

            Compound xx = compoundOrNull(
                    compound(o,
                            pdt,
                            subsChanged ? newSubs : oldSubs)
            );
            if (xx == null)
                throw new InvalidTermException("unable to atemporalize", c);


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

    @NotNull
    public Term difference(@NotNull Op o, @NotNull Compound a, @NotNull TermContainer b) {

        if (a.equals(b))
            return False; //empty set

        //quick test: intersect the mask: if nothing in common, then it's entirely the first term
        if ((a.structure() & b.structure()) == 0) {
            return a;
        }

        int size = a.size();
        List<Term> terms = $.newArrayList(size);

        for (int i = 0; i < size; i++) {
            Term x = a.sub(i);
            if (!b.contains(x)) {
                terms.add(x);
            }
        }

        int retained = terms.size();
        if (retained == size) { //same as 'a'
            return a;
        } else if (retained == 0) {
            return False; //empty set
        } else {
            return the(o, terms.toArray(new Term[retained]));
        }

    }

    public Term the(@NotNull Op op, int dt, Collection<Term> sub) {
        int ss = sub.size();
        return the(op, dt, sub.toArray(new Term[ss]));
    }


}
