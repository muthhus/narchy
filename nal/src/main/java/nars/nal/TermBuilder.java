package nars.nal;

import nars.$;
import nars.Op;
import nars.Param;
import nars.nal.meta.match.Ellipsislike;
import nars.nal.op.TermTransform;
import nars.term.Compound;
import nars.term.InvalidTermException;
import nars.term.Term;
import nars.term.Terms;
import nars.term.atom.Atom;
import nars.term.compound.GenericCompound;
import nars.term.container.TermContainer;
import nars.term.container.TermSet;
import nars.term.container.TermVector;
import org.eclipse.collections.api.set.MutableSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static java.util.Arrays.copyOfRange;
import static nars.Op.*;
import static nars.nal.Tense.DTERNAL;
import static nars.nal.Tense.XTERNAL;
import static nars.term.compound.Statement.pred;
import static nars.term.compound.Statement.subj;

/**
 * Created by me on 1/2/16.
 */
public abstract class TermBuilder {


    /**
     * truth singularity subterms
     */
    public static final Atom True = $.the("†");
    public static final Atom False = $.the("Ø");
    private static final Term[] TrueArray = new Term[]{True};
    public static final Compound FalseProduct = new GenericCompound(Op.PROD, TermVector.the(False));
    /**
     * implications, equivalences, and interval
     */
    public static int InvalidEquivalenceTerm = or(IMPL, EQUI);
    /**
     * equivalences and intervals (not implications, they are allowed
     */
    public static int InvalidImplicationSubject = or(EQUI, IMPL);
    public static int InvalidImplicationPredicate = or(EQUI);


    @NotNull
    public static Term empty(@NotNull Op op) {
        switch (op) {

            case PROD:
                return Terms.ZeroProduct;
            default:
                return False;
        }
    }

    @NotNull
    public Term difference(@NotNull Op o, @NotNull Compound a, @NotNull TermContainer b) {

        //quick test: intersect the mask: if nothing in common, then it's entirely the first term
        if ((a.structure() & b.structure()) == 0) {
            return a;
        }

        Term[] aa = a.terms();

        List<Term> terms = $.newArrayList(aa.length);

        int retained = 0, size = a.size();
        for (int i = 0; i < size; i++) {
            Term x = a.term(i);
            if (!b.containsTerm(x)) {
                terms.add(x);
                retained++;
            }
        }

        if (retained == size) { //same as 'a'
            return a;
        } else if (retained == 0) {
            return False; //empty set
        } else {
            return build(o, terms.toArray(new Term[retained]));
        }

    }

    @NotNull
    public final Term build(@NotNull Op op, int dt, @NotNull Term[] u) throws InvalidTermException {

        /* special handling */
        switch (op) {
            case NEG:
                if (u.length != 1)
                    throw new RuntimeException("invalid negation subterms: " + Arrays.toString(u));

                return negation(u[0]);


            case INSTANCE:
                if (u.length != 2 || dt != DTERNAL) throw new InvalidTermException(INSTANCE, dt, u, "needs 2 arg");
                return inst(u[0], u[1]);
            case PROPERTY:
                if (u.length != 2 || dt != DTERNAL) throw new InvalidTermException(PROPERTY, dt, u, "needs 2 arg");
                return prop(u[0], u[1]);
            case INSTANCE_PROPERTY:
                if (u.length != 2 || dt != DTERNAL)
                    throw new InvalidTermException(INSTANCE_PROPERTY, dt, u, "needs 2 arg");
                return instprop(u[0], u[1]);


            case DISJ:
                if (dt != DTERNAL)
                    throw new InvalidTermException(op, dt, u, "Disjunction must be DTERNAL");
                return disjunction(u);
            case CONJ:
                return conj(dt, u);

            case IMGi:
            case IMGe:
                //if no relation was specified and it's an Image,
                //it must contain a _ placeholder
                if (hasImdex(u)) {
                    //TODO use result of hasImdex in image construction to avoid repeat iteration to find it
                    return image(op, u);
                } else if ((dt < 0) || (dt > u.length)) {
                    throw new InvalidTermException(op, dt, u, "Invalid Image");
                }
                break;


            case DIFFe:
            case DIFFi:
                return newDiff(op, u);
            case SECTe:
                return newIntersectEXT(u);
            case SECTi:
                return newIntersectINT(u);

            case INH:
            case SIM:
            case EQUI:
            case IMPL:
                if (u.length != 2) {//throw new RuntimeException("invalid statement: args=" + Arrays.toString(u));
                    throw new InvalidTermException(op, dt, u, "Statement without exactly 2 arguments");
                }
                return statement(op, dt, u[0], u[1]);

            case PROD:
                if (u.length == 0)
                    return Terms.ZeroProduct;
                break;

        }

        return finish(op, dt, u);
    }

    @NotNull
    private static Term[] conjTrueFalseFilter(@NotNull Term[] u) {
        int trues = 0; //# of True subterms that can be eliminated
        for (Term x : u) {
            if (x.equals(True)) {
                trues++;
            } else if (x.equals(False)) {

                //false subterm in conjunction makes the entire condition false
                //this will eventually reduce diectly to false in this method's only callee HACK
                return new Term[]{False};

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
            if (!uu.equals(True)) // && (!uu.equals(False)))
                y[j++] = uu;
        }
        if (j != y.length)
            throw new RuntimeException("invalid conjunction after filtration");

        return y;
    }


    static boolean validEquivalenceTerm(@NotNull Term t) {
        return !t.isAny(InvalidEquivalenceTerm);
//        if ( instanceof Implication) || (subject instanceof Equivalence)
//                || (predicate instanceof Implication) || (predicate instanceof Equivalence) ||
//                (subject instanceof CyclesInterval) || (predicate instanceof CyclesInterval)) {
//            return null;
//        }
    }

    static boolean hasImdex(@NotNull Term[] r) {
        for (Term x : r) {
            //        if (t instanceof Compound) return false;
//        byte[] n = t.bytes();
//        if (n.length != 1) return false;
            if (x.equals(Imdex)) return true;
        }
        return false;
    }


    @NotNull
    public abstract Term newCompound(@NotNull Op op, int dt, @NotNull TermContainer subterms);


    @NotNull
    public Term build(@NotNull Op op, @NotNull Term... tt) {
        return build(op, DTERNAL, tt);
    }


    @NotNull
    public Term newDiff(@NotNull Op op, @NotNull Term[] t) {

        //corresponding set type for reduction:
        Op set = op == DIFFe ? SETe : SETi;

        switch (t.length) {
            case 1:
                Term t0 = t[0];
                return t0 instanceof Ellipsislike ? finish(op, t0) : t0;
            case 2:
                Term et0 = t[0], et1 = t[1];
                if ((et0.op() == set && et1.op() == set))
                    return difference(set, (Compound) et0, (Compound) et1);
                else
                    return et0.equals(et1) ? False : finish(op, t);
            default:
                throw new InvalidTermException(op, t, "diff requires 2 terms");
        }
    }


    @Nullable
    public final Term finish(@NotNull Op op, @NotNull Term... args) {
        return finish(op, DTERNAL, args);
    }

    @NotNull
    public final Term finish(@NotNull Op op, @NotNull TermContainer args) {
        return finish(op, DTERNAL, args);
    }

    @NotNull
    public final Term finish(@NotNull Op op, int dt, @NotNull Term... args) {
        return finish(op, dt, TermContainer.the(op, args));
    }

    public static boolean isTrueOrFalse(@NotNull Term x) {
        return isTrue(x) || isFalse(x);
    }

    public static boolean isTrue(@NotNull Term x) {
        return x.equals(True);
    }

    public static boolean isFalse(@NotNull Term x) {
        return x.equals(False);
    }


    /**
     * step before calling Make, do not call manually from outside
     */
    @NotNull
    protected final Term finish(@NotNull Op op, int dt, @NotNull TermContainer args) {

        int s = args.size();

        if (s == 1 && op.minSize > 1) {
            //special case: allow for ellipsis to occupy one item even if minArity>1
            Term a0 = args.term(0);
            if (!(a0 instanceof Ellipsislike)) {
                //return null;
                //throw new RuntimeException("invalid size " + s + " for " + op);
                return a0; //reduction
            }
        }

        //if (Param.DEBUG ) {
        //check for any imdex terms that may have not been removed
        for (Term x : args.terms()) {
            if (isTrueOrFalse(x)) {
                //return null;
                // throw new RuntimeException(op + " term with imdex in subterms: " + args);
                return False;
            }
        }
        //}

        return newCompound(op, dt, args);
    }


    @Nullable
    public Compound inst(Term subj, Term pred) {
        return (Compound) build(INH, build(SETe, subj), pred);
    }

    @Nullable
    public Compound prop(Term subj, Term pred) {
        return (Compound) build(INH, subj, build(SETi, pred));
    }

    @Nullable
    public Compound instprop(@NotNull Term subj, @NotNull Term pred) {
        return (Compound) build(INH, build(SETe, subj), build(SETi, pred));
    }

    @Nullable
    public final Term[] negation(@NotNull Term[] t) {
        int l = t.length;
        Term[] u = new Term[l];
        for (int i = 0; i < l; i++) {
            u[i] = negation(t[i]);
        }
        return u;
    }

    @NotNull
    public final Term negation(@NotNull Term t) {

        //HACK testing for equality like this is not a complete solution. for that we need a new special term type

        if (isTrue(t)) return False;
        if (isFalse(t)) return True;

        if (t.op() == NEG) {
            // (--,(--,P)) = P
            t = ((TermContainer) t).term(0);

            if (isTrue(t)) return False;
            if (isFalse(t)) return True;

            return t;

        } else {
            return (t instanceof Compound) || (t.op().var) ? finish(NEG, t) : False;
        }
    }


    @Nullable
    final Term image(@NotNull Op o, @NotNull Term[] res) {

        int index = DTERNAL, j = 0;
        for (Term x : res) {
            if (x.equals(Imdex)) {
                index = j;
            }
            j++;
        }

        if (index == DTERNAL)
            throw new RuntimeException("invalid image subterms: " + Arrays.toString(res));

        int serN = res.length - 1;
        Term[] ser = new Term[serN];
        System.arraycopy(res, 0, ser, 0, index);
        System.arraycopy(res, index + 1, ser, index, (serN - index));

        return finish(o, index, ser);
    }

    @NotNull
    public Term conj(int dt, final @NotNull Term... uu) {

        Term[] u = conjTrueFalseFilter(uu);

        int ul = u.length;
        if (ul == 0)
            return False;

        if (ul == 1) {
            Term only = u[0];

            if (only instanceof Ellipsislike) {
                //preserve unitary ellipsis for patterns etc
                return finish(CONJ, dt, only);
            } else return only;

        }


        if ((dt == DTERNAL) || (dt == 0) || (dt == XTERNAL)) {
            return junctionFlat(CONJ, dt, u);
        } else {

            if (ul == 2) {

                Term a = u[0];
                Term b = u[1];
                if (a.equals(b))
                    return a;

                //if dternal or parallel, dont allow the subterms to be conegative:
                if (((dt == DTERNAL) || (dt == 0)) &&
                        (((a.op() == NEG) && ($.unneg(a).equals(b))) ||
                                ((b.op() == NEG) && ($.unneg(b).equals(a))))) {
                    return False;
                }

                return finish(CONJ,
                        (u[0].compareTo(u[1]) > 0) ? -dt : dt, //it will be reversed in commutative sorting, so invert dt if sort order swapped
                        u);
            } else {
                throw new InvalidTermException(CONJ, dt, u, "temporal conjunction requires exactly 2 arguments");
            }

        }
    }


    /**
     * flattening junction builder, for (commutive) multi-arg conjunction and disjunction (dt == 0 ar DTERNAL)
     */
    @NotNull
    public Term junctionFlat(@NotNull Op op, int dt, @NotNull Term[] u) {

        if (u.length == 0)
            return False;

        assert (dt == 0 || dt == DTERNAL); //throw new RuntimeException("should only have been called with dt==0 or dt==DTERNAL");


        TreeSet<Term> s = new TreeSet();
        flatten(op, u, dt, s);

        //boolean negate = false;
        int n = s.size();
        switch (n) {
            case 0:
                return False;
            case 1:
                return s.iterator().next();
            default:
                s = junctionGroupNonDTSubterms(s, dt);
                if (s.isEmpty())
                    return False; //wtf
                return finish(op, dt, TermSet.the(s));
        }


        //Co-Negated Subterms - any commutive terms with both a subterm and its negative are invalid
        //if (unwrappedNegs!=null) {
//            if (op == DISJ && unwrappedNegs.anySatisfy(s::contains))
//                return null; //throw new InvalidTerm(op, u);
//            //for conjunction, this is handled by the Task normalization process to allow the co-negations for naming concepts
//            if (s.removeAll(unwrappedNegs)) {
//                //remove their negative counterparts
//                s.removeIf(x -> {
//                    return (x.op()==NEG) && unwrappedNegs.contains(((Compound)x).term(0));
//                });
//
//                n = s.size();
//                if (n == 0)
//                    return null;
//                if (n == 1)
//                    return s.iterator().next();
//
//            } else {
//                //if all subterms negated; apply DeMorgan's Law
//                if ((dt == DTERNAL) && (unwrappedNegs.size() == n)) {
//                    op = (op == CONJ) ? DISJ : CONJ;
//                    negate = true;
//                }
//            }
        //}

//        if (negate) {
//            return negation( finish(op, dt, unwrappedNegs.toArray(new Term[n])) );
//        } else {
        //if (dt == 0) {

        //}
    }

    /**
     * this is necessary to keep term structure consistent for intermpolation.
     * by grouping all non-sequence subterms into its own subterm, future
     * flattening and intermpolation is prevented from destroying temporal
     * measurements.
     */
    @NotNull
    protected TreeSet<Term> junctionGroupNonDTSubterms(@NotNull TreeSet<Term> s, int innerDT) {
        TreeSet<Term> outer = new TreeSet();
        Iterator<Term> ss = s.iterator();
        while (ss.hasNext()) {
            Term x = ss.next();
            if (x.op() == CONJ /* dt will be something other than 'innerDT' having just been flattened */) {
                outer.add(x);
                ss.remove();
            }
        }
        if (outer.isEmpty()) {
            return s; //no change
        }

        switch (s.size()) {
            case 0:
                return outer;
            case 1:
                outer.add(s.iterator().next());
                return outer;
            default:
                outer.add(finish(CONJ, innerDT, TermSet.the(s)));
                return outer;
        }

    }

    /**
     * returns # of terms negated
     */
    static void flatten(@NotNull Op op, @NotNull Term[] u, int dt, @NotNull Collection<Term> s) {

        for (Term x : u) {

            if ((x.op() == op) && (((Compound) x).dt() == dt) /* 0 or DTERNAL */) {
                flatten(op, ((Compound) x).terms(), dt, s); //recurse
            } else {
                if (x instanceof Compound) {
                    //cancel co-negations TODO optimize this?
                    if (!s.isEmpty()) {
                        Term negX = $.neg(x);
                        if (s.remove(negX)) {
                            continue;
                        }
                    }
                }
                s.add(x);
            }
        }
    }


    @NotNull
    public Term statement(@NotNull Op op, int dt, @NotNull Term subject, @NotNull Term predicate) {


        while (true) {

            //special statement filters
            switch (op) {

                case INH:
                    if (predicate instanceof TermTransform && transformImmediates() && subject.op() == PROD) {
                        return ((TermTransform) predicate).function((Compound<?>) subject);
                    }
                    break;


                case EQUI:
                    if (!Param.ALLOW_RECURSIVE_IMPLICATIONS) {
                        if (!validEquivalenceTerm(subject))
                            throw new InvalidTermException(op, dt, new Term[]{subject, predicate}, "Invalid equivalence subject");
                        if (!validEquivalenceTerm(predicate))
                            throw new InvalidTermException(op, dt, new Term[]{subject, predicate}, "Invalid equivalence predicate");
                    }
                    break;

                case IMPL:
                    if (!Param.ALLOW_RECURSIVE_IMPLICATIONS) {
                        if (subject.isAny(InvalidImplicationSubject))
                            throw new InvalidTermException(op, dt, new Term[]{subject, predicate}, "Invalid implication subject");
                        if (predicate.isAny(InvalidImplicationPredicate))
                            throw new InvalidTermException(op, dt, new Term[]{subject, predicate}, "Invalid implication predicate");
                    }


                    if (subject.equals(True)) {
                        return predicate;
                    } else if (subject.equals(False)) {
                        return False;
                        //throw new InvalidTermException(op, dt, new Term[] { subject, predicate }, "Implication predicate is singular FALSE");
                        //return negation(predicate); /??
                    }

                    // (C ==> (A ==> B))   <<==>>  ((&&,A,C) ==> B)
                    if (predicate.op() == IMPL) {
                        Term oldCondition = subj(predicate);
                        if (!Param.ALLOW_RECURSIVE_IMPLICATIONS && (oldCondition.op() == CONJ && oldCondition.containsTerm(subject)))
                            throw new InvalidTermException(op, dt, new Term[]{subject, predicate}, "Implication circularity");
                        else {
                            if ((dt == 0) || (dt == DTERNAL))
                                return impl2Conj(dt, subject, predicate, oldCondition);
                        }
                    }


                    //filter (factor out) any common subterms iff commutive
                    if ((subject.op() == CONJ) && (predicate.op() == CONJ)) {
                        Compound csub = (Compound) subject;
                        Compound cpred = (Compound) predicate;
                        if ((dt == DTERNAL || dt == 0)) {

                            TermContainer subjs = csub.subterms();
                            TermContainer preds = cpred.subterms();

                            MutableSet<Term> common = TermContainer.intersect(subjs, preds);
                            if (!common.isEmpty()) {
                                Term newSubject = build(csub, TermContainer.except(subjs, common));
                                Term newPredicate = build(cpred, TermContainer.except(preds, common));
                                subject = newSubject;
                                predicate = newPredicate;
                                continue;
                            }
                        }
                    }

                    break;

            }


            //compare unneg'd if it's not temporal or eternal/parallel
            boolean preventInverse = !op.temporal || (dt == DTERNAL || dt == 0);
            Term sRoot = (subject instanceof Compound && preventInverse) ? $.unneg(subject).term() : subject;
            Term pRoot = (predicate instanceof Compound && preventInverse) ? $.unneg(predicate).term() : predicate;
            if (Terms.equalsAnonymous(sRoot, pRoot))
                return subject.op() == predicate.op() ? True : False; //True if same, False if negated


            //TODO its possible to disqualify invalid statement if there is no structural overlap here??
//
            if (sRoot.op() == CONJ && (sRoot.containsTerm(pRoot) || (pRoot instanceof Compound && (preventInverse && sRoot.containsTerm($.neg(pRoot)))))) { //non-recursive
                //throw new InvalidTermException(op, new Term[]{subject, predicate}, "subject conjunction contains predicate");
                return True;
            }


            if (pRoot.op() == CONJ && pRoot.containsTerm(sRoot) || (sRoot instanceof Compound && (preventInverse && pRoot.containsTerm($.neg(sRoot))))) {
                //throw new InvalidTermException(op, new Term[]{subject, predicate}, "predicate conjunction contains subject");
                return True;
            }

            if (sRoot.op().statement && pRoot.op().statement) {
                if ((((Compound) sRoot).term(0).equals(((Compound) pRoot).term(1))) ||
                        (((Compound) sRoot).term(1).equals(((Compound) pRoot).term(0))))
                    throw new InvalidTermException(op, new Term[]{subject, predicate}, "inner subject cross-linked with predicate");

            }


            if (op.commutative) {


                //normalize co-negation
                boolean sn = subject.op() == NEG;
                boolean pn = predicate.op() == NEG;
                if (sn && pn) {
                    //unnegate both
                    subject = $.unneg(subject).term();
                    predicate = $.unneg(predicate).term();
                } else if (sn && !pn) {
                    //swap negation so that subject is un-negated
                    subject = $.unneg(subject).term();
                    predicate = $.neg(predicate);
                }

                boolean reversed = subject.compareTo(predicate) > 0;
                if (reversed) {
                    Term x = predicate;
                    predicate = subject;
                    subject = x;
                }

                if (reversed && (dt != DTERNAL && dt!=XTERNAL && dt != 0) ) {
                    dt = -dt;
                }


            }
            return finish(op, dt, subject, predicate);

        }
    }

    /**
     * whether this builder applies immediate transforms
     */
    protected abstract boolean transformImmediates();


//    @Nullable
//    public Term subtractSet(@NotNull Op setType, @NotNull Compound A, @NotNull Compound B) {
//        return difference(setType, A, B);
//    }

    @NotNull
    public Term impl2Conj(int t, Term subject, @NotNull Term predicate, Term oldCondition) {
        return build(IMPL, conj(t, subject, oldCondition), pred(predicate));
    }

    @Nullable
    public Term newIntersectINT(@NotNull Term[] t) {
        return newIntersection(t,
                SECTi,
                SETi,
                SETe);
    }

    @NotNull
    public Term newIntersectEXT(@NotNull Term[] t) {
        return newIntersection(t,
                SECTe,
                SETe,
                SETi);
    }

    @NotNull
    public Term newIntersection(@NotNull Term[] t, @NotNull Op intersection, @NotNull Op setUnion, @NotNull Op setIntersection) {
        switch (t.length) {

            case 1:

                Term single = t[0];
                return single instanceof Ellipsislike ? finish(intersection, single) : single;

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
    public Term newIntersection2(@NotNull Term term1, @NotNull Term term2, @NotNull Op intersection, @NotNull Op setUnion, @NotNull Op setIntersection) {

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

        if (o1 == intersection) {
            return finish(intersection, TermSet.concat(
                    ((TermContainer) term1).terms(),
                    o2 == intersection ? ((TermContainer) term2).terms() : new Term[]{term2}
                    )
            );
        }

        return finish(intersection, term1, term2);
    }


    @NotNull
    public Term intersect(@NotNull Op o, @NotNull Compound a, @NotNull Compound b) {
        if (a.equals(b))
            return a;

        MutableSet<Term> s = TermContainer.intersect(
                /*(TermContainer)*/ a, /*(TermContainer)*/ b
        );
        return s.isEmpty() ? empty(o) : (Compound) finish(o, TermContainer.the(o, s));
    }


    @NotNull
    public Compound union(@NotNull Op o, @NotNull Compound term1, @NotNull Compound term2) {
        TermContainer u = TermContainer.union(term1, term2);
        if (u == term1)
            return term1;
        else if (u == term2)
            return term2;
        else
            return (Compound) finish(o, u);
    }

    @Nullable
    public final Term build(@NotNull Compound csrc, @NotNull Term[] newSubs) {
        return build(csrc.op(), csrc.dt(), newSubs);
    }

    @NotNull
    public final Term build(@NotNull Compound csrc, @NotNull TermContainer newSubs) {
        return csrc.subterms().equals(newSubs) ? csrc : build(csrc.op(), csrc.dt(), newSubs.terms());
    }

    public final Term disjunction(@NotNull Term[] u) {
        return negation(conj(DTERNAL, negation(u)));
    }

}
