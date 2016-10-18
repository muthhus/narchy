package nars.nal;

import nars.$;
import nars.Op;
import nars.nal.meta.match.Ellipsislike;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Terms;
import nars.term.compound.GenericCompound;
import nars.term.container.TermContainer;
import nars.term.container.TermSet;
import nars.term.container.TermVector;
import nars.term.transform.TermTransform;
import nars.term.util.InvalidTermException;
import nars.term.var.Variable;
import org.eclipse.collections.api.set.MutableSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import static java.util.Arrays.copyOfRange;
import static nars.Op.*;
import static nars.term.Term.False;
import static nars.term.Term.True;
import static nars.term.compound.Statement.pred;
import static nars.term.compound.Statement.subj;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

/**
 * Created by me on 1/2/16.
 */
public abstract class TermBuilder {


    private static final Term[] TrueArray = {True};
    private static final Term[] FalseArray = {False};
    public static final TermContainer InvalidSubterms = TermVector.the(False);


    private static final int InvalidEquivalenceTerm = or(IMPL, EQUI);

    private static final int InvalidImplicationSubject = or(EQUI, IMPL);
    private static final int InvalidImplicationPredicate = or(EQUI);


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

        if (a.equals(b))
            return False; //empty set

        //quick test: intersect the mask: if nothing in common, then it's entirely the first term
        if ((a.structure() & b.structure()) == 0) {
            return a;
        }

        int retained = 0, size = a.size();
        List<Term> terms = $.newArrayList(size);

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
            return the(o, terms.toArray(new Term[retained]));
        }

    }


    /** main entry point for compound construction - creates an immutable result  */
    @NotNull public Term the(@NotNull Op op, int dt, @NotNull Term[] u) throws InvalidTermException {

        if (transformImmediates())
            productNormalizeSubterms(u);

        int arity = u.length;
        switch (op) {
//            case INT:
//            case INTRANGE:
//                System.out.println(op + " " + dt + " " + Arrays.toString(u));
//                break;

            case NEG:
                if (arity != 1)
                    throw new InvalidTermException(op, dt, u, "negation requires 1 subterm");

                return neg(u[0]);

//            case INTRANGE:
//                System.err.println("intRange: " + Arrays.toString(u));
//                break;

            case INSTANCE:
                if (arity != 2 || dt != DTERNAL) throw new InvalidTermException(INSTANCE, dt, u, "needs 2 arg");
                return inst(u[0], u[1]);
            case PROPERTY:
                if (arity != 2 || dt != DTERNAL) throw new InvalidTermException(PROPERTY, dt, u, "needs 2 arg");
                return prop(u[0], u[1]);
            case INSTANCE_PROPERTY:
                if (arity != 2 || dt != DTERNAL)
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

                if ((arity < 1) || (dt > arity))
                    throw new InvalidTermException(op, dt, u, "image requires size=2 excluding _ imdex");

                if (hasImdex(u)) {
                    return image(op, u);
                }

                if ((dt < 0) && !(u[0].varPattern()>0 || u[1].varPattern()>0))
                    throw new InvalidTermException(op, dt, u, "Invalid Image");


                break; //construct below


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
                if (arity != 2) {
                    throw new InvalidTermException(op, dt, u, "Statement without exactly 2 arguments");
                }
                return statement(op, dt, u[0], u[1]);

            case PROD:
                if (arity == 0)
                    return Terms.ZeroProduct;
                break;

        }

        return finish(op, dt, u);
    }

    private void productNormalizeSubterms(Term[] u) {
        for (int i = 0, uLength = u.length; i < uLength; i++) {
            u[i] = productNormalize(u[i]);
        }
    }

    public Term productNormalize(Term t) {
        boolean neg = t.op() == NEG;
        if (neg)
            t = t.unneg();

        if (t instanceof Compound && (t.op() == INH) && (t.varPattern()==0) && t.hasAny(Op.IMGbits))  {
            Term s = (((Compound) t).term(0));
            Term p = (((Compound) t).term(1));
            Op so = s.op();
            Op po = p.op();
            if (so == Op.IMGi && !po.image) {
                Compound ii  = (Compound)s;
                t = $.inh(ii.term(0), imageUnwrapToProd(p, ii));
            } else if (po == Op.IMGe && !so.image) {
                Compound ii  = (Compound)p;
                t = $.inh(imageUnwrapToProd(s, ii), ii.term(0));
            }
        }

        return !neg ? t : neg(t);
    }

    @NotNull
    private static Term imageUnwrapToProd(Term p, Compound ii) {
        return $.p(imageUnwrap(ii, p));
    }

    public static Term[] imageUnwrap(Compound image, Term other) {
        int l = image.size();
        Term[] t = new Term[l];
        int r = image.dt();
        @NotNull Term[] imageTerms = image.terms();
        for (int i = 0 /* skip the first element of the image */, j = 0; j < l; ) {
            t[j++] = ((j) == r) ? other : imageTerms[++i];
        }
        return t;
    }

    /** collection implementation of the conjunction true/false filter */
    private static TreeSet<Term> conjTrueFalseFilter(TreeSet<Term> terms) {
        Iterator<Term> ii = terms.iterator();
        while (ii.hasNext()) {
            Term n = ii.next();
            if (isTrue(n))
                ii.remove();
            else if (isFalse(n))
                return null;
        }
        return terms;
    }


    /** array implementation of the conjunction true/false filter */
    @NotNull private static Term[] conjTrueFalseFilter(@NotNull Term[] u) {
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

        assert(j == y.length);

        return y;
    }


    private static boolean validEquivalenceTerm(@NotNull Term t) {
        return !t.isAny(InvalidEquivalenceTerm);
//        if ( instanceof Implication) || (subject instanceof Equivalence)
//                || (predicate instanceof Implication) || (predicate instanceof Equivalence) ||
//                (subject instanceof CyclesInterval) || (predicate instanceof CyclesInterval)) {
//            return null;
//        }
    }

    private static boolean hasImdex(@NotNull Term[] r) {
        for (Term x : r) {
            //        if (t instanceof Compound) return false;
//        byte[] n = t.bytes();
//        if (n.length != 1) return false;
            if (x.equals(Imdex)) return true;
        }
        return false;
    }


    @NotNull
    protected Term newCompound(@NotNull Op op, int dt, @NotNull TermContainer subterms) {
        return new GenericCompound(op, dt, subterms);
    }

    @NotNull
    public final Term the(@NotNull Op op, @NotNull Term... tt) {
        return the(op, DTERNAL, tt);
    }


    @NotNull
    private Term newDiff(@NotNull Op op, @NotNull Term[] t) {

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
    private final Term finish(@NotNull Op op, @NotNull Term... args) {
        return finish(op, DTERNAL, args);
    }

    @NotNull
    private Term finish(@NotNull Op op, @NotNull TermContainer args) {
        return finish(op, DTERNAL, args);
    }

    @NotNull
    private Term finish(@NotNull Op op, int dt, @NotNull Term... args) {
        return finish(op, dt, TermContainer.the(op, args));
    }

    public static boolean isTrueOrFalse(@NotNull Term x) {
        return isTrue(x) || isFalse(x);
    }


    private static boolean isTrue(@NotNull Term x) {
        return x == True;
    }

    private static boolean isFalse(@NotNull Term x) {
        return x == False;
    }


    /**
     * final step in compound construction
     */
    @NotNull
    private Term finish(@NotNull Op op, int dt, @NotNull TermContainer args) {

        //if (Param.DEBUG ) {
        //check for any imdex terms that may have not been removed
        for (Term x : args.terms()) {
            if (isTrueOrFalse(x)) {
                if ((op == NEG) || (op == CONJ) || (op == IMPL) || (op == EQUI))
                   throw new RuntimeException("appearance of True/False in " + op + " should have been filtered prior to this");

                //any other term causes it to be invalid/meaningless
                return False;
            }
        }

        args = ArithmeticInduction.compress(op, dt, args);

        int s = args.size();
        if (s == 0) {
            throw new RuntimeException("should not have zero args here");
        }
        if (s == 1 && op.minSize > 1) {
            //special case: allow for ellipsis to occupy one item even if minArity>1
            Term a0 = args.term(0);
            if (!(a0 instanceof Ellipsislike)) {
                //return null;
                //throw new RuntimeException("invalid size " + s + " for " + op);
                return a0; //reduction
            }
        }

        return newCompound(op, dt, args);
    }




    @Nullable
    public Term inst(Term subj, Term pred) {
        return the(INH, the(SETe, subj), pred);
    }

    @Nullable
    public Term prop(Term subj, Term pred) {
        return the(INH, subj, the(SETi, pred));
    }

    @Nullable
    public Term instprop(@NotNull Term subj, @NotNull Term pred) {
        return the(INH, the(SETe, subj), the(SETi, pred));
    }

    @Nullable
    private Term[] neg(@NotNull Term[] modified) {
        int l = modified.length;
        Term[] u = new Term[l];
        for (int i = 0; i < l; i++) {
            u[i] = neg(modified[i]);
        }
        return u;
    }

    @NotNull
    public final Term neg(@NotNull Term t) {
        //HACK testing for equality like this is not a complete solution. for that we need a new special term type

        if ((t instanceof Compound) || (t instanceof Variable)) {
            if (t.op() == NEG) {
                // (--,(--,P)) = P
                return t.unneg();
            } else {
                return //newCompound(NEG, DTERNAL, TermVector.the(t)); //newCompound bypasses some checks that finish involves
                       finish(NEG, t);
            }
        } else {
            if (isFalse(t)) return True;
            if (isTrue(t)) return False;
            return t;
        }
    }


    @Nullable
    private Term image(@NotNull Op o, @NotNull Term[] res) {

        int index = DTERNAL, j = 0;
        boolean hasPatternVar = false;
        for (Term x : res) {
            if (x.equals(Imdex)) {
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
                throw new InvalidTermException(o, DTERNAL, res, "image missing '_' (Imdex)");

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

        int n = u.length;
        if (n == 0)
            return False;

        if (n == 1) {
            Term only = u[0];

            //preserve unitary ellipsis for patterns etc
            return only instanceof Ellipsislike ? finish(CONJ, dt, only) : only;

        }


        if (dt == XTERNAL) {
            if (n!=2)
                throw new InvalidTermException(CONJ, XTERNAL, u, "XTERNAL only applies to 2 subterms, as dt placeholder");

            //preserve grouping (don't flatten) but use normal commutive ordering as dternal &&
            return finish(CONJ, XTERNAL, u);
        }

        if (commutive(dt)) {
            return junctionFlat(CONJ, dt, u);
        }

        if (n == 2) {

            Term a = u[0];
            Term b = u[1];
            if (a.equals(b))
                return a;

//            //if dternal or parallel, dont allow the subterms to be conegative:
//            if (commutive(dt) &&
//                    (((a.op() == NEG) && ($.unneg(a).equals(b))) ||
//                            ((b.op() == NEG) && ($.unneg(b).equals(a))))) {
//                return False;
//            }

            return finish(CONJ,
                    (u[0].compareTo(u[1]) > 0) ? -dt : dt, //it will be reversed in commutative sorting, so invert dt if sort order swapped
                    u);
        } else {
            throw new InvalidTermException(CONJ, dt, u, "temporal conjunction requires exactly 2 arguments");
        }

    }

    private static boolean commutive(int dt) {
        return (dt == DTERNAL) || (dt == 0);
    }




    /**
     * flattening junction builder, for (commutive) multi-arg conjunction and disjunction (dt == 0 ar DTERNAL)
     */
    @NotNull
    private Term junctionFlat(@NotNull Op op, int dt, @NotNull Term[] u) {

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
                @Nullable TreeSet<Term> cs = junctionGroupNonDTSubterms(s, dt);
                if (cs == null)
                    return False;
                TreeSet<Term> ts = conjTrueFalseFilter(cs);
                if (ts == null || ts.isEmpty())
                    return False;
                return finish(op, dt, TermSet.the(ts));
        }

    }



    /**
     * this is necessary to keep term structure consistent for intermpolation.
     * by grouping all non-sequence subterms into its own subterm, future
     * flattening and intermpolation is prevented from destroying temporal
     * measurements.
     */
    @Nullable
    private TreeSet<Term> junctionGroupNonDTSubterms(@NotNull TreeSet<Term> s, int innerDT) {
        TreeSet<Term> outer = new TreeSet();
        Iterator<Term> ss = s.iterator();
        while (ss.hasNext()) {
            Term x = ss.next();
            if (isTrue(x)) {
                ss.remove();
            } else if (isFalse(x)) {
                return null;
            } else if (x.op() == CONJ /* dt will be something other than 'innerDT' having just been flattened */) {
                outer.add(x);
                ss.remove();
            }
        }
        if (outer.isEmpty()) {
            return s; //no change
        }

        Term next = null;
        switch (s.size()) {
            case 0:
                return outer;
            case 1:
                next = s.iterator().next();
                break;
            default:
                next = finish(CONJ, innerDT, TermSet.the(s));
                break;
        }

        outer.add(next);

        return outer;
    }

    /**
     * for commutive conjunction (0 or DTERNAL)
     */
    private void flatten(@NotNull Op op, @NotNull Term[] u, int dt, @NotNull Collection<Term> s) {

        for (Term x : u) {

            if ((x.op() == op) && (((Compound) x).dt() == dt)) {
                flatten(op, ((Compound) x).terms(), dt, s); //recurse
            } else {
                if (x instanceof Compound) {
                    //cancel co-negations TODO optimize this?
                    if (!s.isEmpty()) {
                        if (s.remove(neg(x))) {
                            continue;
                        }
                    }
                }
                s.add(x);
            }
        }
    }


    @NotNull
    protected Term statement(@NotNull Op op, int dt, @NotNull Term subject, @NotNull Term predicate) {
        statement:
        while (true) {


            while (true) {

                //special statement filters
                switch (op) {

                    case INH:
                        if (predicate instanceof TermTransform && transformImmediates() && subject.op() == PROD) {
                            return ((TermTransform) predicate).function((Compound) subject);
                        }
                        break;


                    case EQUI:

                        if (!validEquivalenceTerm(subject))
                            throw new InvalidTermException(op, dt, new Term[]{subject, predicate}, "Invalid equivalence subject");
                        if (!validEquivalenceTerm(predicate))
                            throw new InvalidTermException(op, dt, new Term[]{subject, predicate}, "Invalid equivalence predicate");

                        boolean subjNeg = subject.op() == NEG;
                        boolean predNeg = predicate.op() == NEG;
                        if (subjNeg && predNeg) {
                            subject = subject.unneg();
                            predicate = predicate.unneg();
                            continue statement;
                        } else if (!subjNeg && predNeg) {
                            return neg(statement(op, dt, subject, predicate.unneg()));
                        } else if (subjNeg && !predNeg) {
                            return neg(statement(op, dt, subject.unneg(), predicate));
                        }

                        break;


                    case IMPL:


                        if (isTrue(subject)) {
                            return predicate;
                        } else if (isFalse(subject) || isTrueOrFalse(predicate)) {
                            return False;
                            //throw new InvalidTermException(op, dt, new Term[] { subject, predicate }, "Implication predicate is singular FALSE");
                            //return negation(predicate); /??
                        }


                        //filter (factor out) any common subterms iff commutive
                        if ((subject.op() == CONJ) && (predicate.op() == CONJ)) {
                            Compound csub = (Compound) subject;
                            Compound cpred = (Compound) predicate;
                            if (commutive(dt) || dt == XTERNAL /* if XTERNAL somehow happens here, just consider it as commutive */) {

                                TermContainer subjs = csub.subterms();
                                TermContainer preds = cpred.subterms();

                                MutableSet<Term> common = TermContainer.intersect(subjs, preds);
                                if (!common.isEmpty()) {
                                    subject = the(csub, TermContainer.exceptToSet(subjs, common));
                                    predicate = the(cpred, TermContainer.exceptToSet(preds, common));
                                    continue;
                                }
                            }
                        }

                        // (C ==> (A ==> B))   <<==>>  ((&&,A,C) ==> B)
                        if (predicate.op() == IMPL) {
                            Term oldCondition = subj(predicate);
                            if ((oldCondition.op() == CONJ && oldCondition.containsTerm(subject))) {
                                //throw new InvalidTermException(op, dt, new Term[]{subject, predicate}, "Implication circularity");
                                return True; //infinite loop
                            } else {
                                if (commutive(dt)  /* if XTERNAL somehow happens here, just consider it as commutive */) {
                                    subject = conj(dt, subject, oldCondition);
                                    predicate = pred(predicate);
                                }
                            }
                        }


                        if (subject.isAny(InvalidImplicationSubject))
                            throw new InvalidTermException(op, dt, new Term[]{subject, predicate}, "Invalid implication subject");
                        if (predicate.isAny(InvalidImplicationPredicate))
                            throw new InvalidTermException(op, dt, new Term[]{subject, predicate}, "Invalid implication predicate");

                        if (predicate.op() == NEG) {
                            Term unNegatedPred = $.impl(subject, dt, predicate.unneg());
                            return //negation
                                    neg( //to be safe use the full negation but likely it can be the local negation pipeline
                                            unNegatedPred
                                    );
                        }
                        break;

                }

                //if either the subject or pred are True/False by this point, fail
                if (isTrueOrFalse(subject) || isTrueOrFalse(predicate)) {
                    return subject == predicate ? True : False;
                }


                Term ss = subject.unneg();
                Term pp = predicate.unneg();

                if (Terms.equalAtemporally(ss,pp)) {
                    return subject==ss ^ predicate==pp ? False : True;  //handle root-level negation comparison
                }

                //with exceptions for pattern variable containing terms
                if ((ss.varPattern()==0 && ss.containsTermRecursivelyAtemporally(pp)) ||
                        (pp.varPattern()==0 && pp.containsTermRecursivelyAtemporally(ss))) {
                    return False; //self-reference
                }


//                //compare unneg'd if it's not temporal or eternal/parallel
//                boolean preventInverse = !op.temporal || (commutive(dt) || dt == XTERNAL);
//                Term sRoot = (subject instanceof Compound && preventInverse) ? $.unneg(subject) : subject;
//                Term pRoot = (predicate instanceof Compound && preventInverse) ? $.unneg(predicate) : predicate;
//                //        if (as == bs) {
////            return true;
////        } else if (as instanceof Compound && bs instanceof Compound) {
////            return equalsAnonymous((Compound) as, (Compound) bs);
////        } else {
////            return as.equals(bs);
////        }
//                if (Terms.equalAtemporally(sRoot, pRoot))
//                    return subject.op() == predicate.op() ? True : False; //True if same, False if negated
//
//
//                //TODO its possible to disqualify invalid statement if there is no structural overlap here??
////
//                @NotNull Op sop = sRoot.op();
//                if (sop == CONJ && (sRoot.containsTerm(pRoot) || (pRoot instanceof Compound && (preventInverse && sRoot.containsTerm($.neg(pRoot)))))) { //non-recursive
//                    //throw new InvalidTermException(op, new Term[]{subject, predicate}, "subject conjunction contains predicate");
//                    return True;
//                }
//
//                @NotNull Op pop = pRoot.op();
//                if (pop == CONJ && pRoot.containsTerm(sRoot) || (sRoot instanceof Compound && (preventInverse && pRoot.containsTerm($.neg(sRoot))))) {
//                    //throw new InvalidTermException(op, new Term[]{subject, predicate}, "predicate conjunction contains subject");
//                    return True;
//                }

//            if (sop.statement && pop.statement) {
//                Compound csroot = (Compound) sRoot;
//                Compound cproot = (Compound) pRoot;
//                if ((csroot.term(0).equals(cproot.term(1))) ||
//                        (csroot.term(1).equals(cproot.term(0))))
//                    throw new InvalidTermException(op, new Term[]{subject, predicate}, "inner subject cross-linked with predicate");
//
//            }


                if (op.commutative) {

                    boolean crossesTime = dt != DTERNAL && dt != XTERNAL && dt != 0;

                    //System.out.println("\t" + subject + " " + predicate + " " + subject.compareTo(predicate) + " " + predicate.compareTo(subject));

                    //normalize co-negation
                    boolean sn = subject.op() == NEG;
                    boolean pn = predicate.op() == NEG;

                    if (sn ^ pn) {
                        //this block should only happen for similarity, since the either-negated case for equivalence is factored out above
//                    Term ss, pp;
//                    if (sn) {
//                        //subject negated;
//                        ss = $.unneg(subject);
//                        pp = predicate;
//                    } else {
//                        //predicate negated, which is the normal form
//                        ss = subject;
//                        pp = $.unneg(predicate);
//                    }
//                    //TODO re-use the original terms rather than re-neg the un-neg
//                    if (ss.compareTo(pp) > 0) {
//                        subject = pp;
//                        predicate = $.neg(ss);
//                        if (crossesTime)
//                            dt = -dt;
//                    } else {
//                        subject = ss;
//                        predicate = $.neg(pp);
//                    }
                    } else {
//                        if (sn && pn) {
////                        //both negative: unnegate both but use the original sort ordering in case negation causes it to change, this will keep it stable
////                        subject = $.unneg(subject);
////                        predicate = $.unneg(predicate);
//                        } else {
//                            //both postiive
//                        }

                        if (subject.compareTo(predicate) > 0) {
                            Term x = predicate;
                            predicate = subject;
                            subject = x;
                            if (crossesTime)
                                dt = -dt;
                        }
                    }

                    //System.out.println( "\t" + subject + " " + predicate + " " + subject.compareTo(predicate) + " " + predicate.compareTo(subject));


                }

                return finish(op, dt, TermVector.the(subject, predicate)); //use the calculated ordering, not the TermContainer default for commutives

            }
        }
    }

    /**
     * whether to apply immediate transforms during compound building
     */
    protected boolean transformImmediates() {
        return true;
    }


//    @Nullable
//    public Term subtractSet(@NotNull Op setType, @NotNull Compound A, @NotNull Compound B) {
//        return difference(setType, A, B);
//    }

    @Nullable
    private Term newIntersectINT(@NotNull Term[] t) {
        return newIntersection(t,
                SECTi,
                SETi,
                SETe);
    }

    @NotNull
    private Term newIntersectEXT(@NotNull Term[] t) {
        return newIntersection(t,
                SECTe,
                SETe,
                SETi);
    }

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
            } else if (t.length - trues == 1){
                //find the element which is not true and return it
                for (Term y : t) {
                    if (!isTrue(y))
                        return y;
                }
            } else {
                //filter the True statements from t
                Term[] t2 = new Term[t.length - trues];
                int yy = 0;
                for (Term y : t) {
                    if (!isTrue(y))
                        t2[yy++] = y;
                }
                t = t2;
            }
        }

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

    @NotNull public Term the(@NotNull Compound csrc, @NotNull Term[] newSubs) {
        return the(csrc.op(), csrc.dt(), newSubs);
    }
    @NotNull public Term the(@NotNull Compound csrc, @NotNull TermContainer newSubs) {
        return the(csrc.op(), csrc.dt(), newSubs.terms());
    }
    @NotNull public Term the(@NotNull Op op, int dt, @NotNull TermContainer newSubs) {
        return the(op, dt, newSubs.terms());
    }
    @NotNull public Term the(@NotNull Compound csrc, @NotNull Collection<Term> newSubs) {
        return the(csrc.op(), csrc.dt(), newSubs.toArray(new Term[newSubs.size()]));
    }

    public final Term disjunction(@NotNull Term[] u) {
        return neg(conj(DTERNAL, neg(u)));
    }



}
