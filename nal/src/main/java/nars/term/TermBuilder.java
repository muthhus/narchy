package nars.term;

import com.gs.collections.api.set.MutableSet;
import com.gs.collections.impl.set.mutable.UnifiedSet;
import nars.Op;
import nars.nal.Tense;
import nars.nal.meta.match.Ellipsis;
import nars.term.container.TermContainer;
import nars.term.container.TermSet;
import nars.term.container.TermVector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import static java.util.Arrays.copyOfRange;
import static nars.Op.*;
import static nars.nal.Tense.DTERNAL;
import static nars.term.Statement.pred;
import static nars.term.Statement.subj;

/**
 * Created by me on 1/2/16.
 */
public abstract class TermBuilder {

    @Nullable
    public Term the(@NotNull Op op, int relation, int dt, @NotNull TermContainer tt) throws InvalidTerm {

        Term[] u = tt.terms();

        /* special handling */
        switch (op) {
            case NEGATE:
                if (u.length != 1)
                    throw new RuntimeException("invalid negation subterms: " + Arrays.toString(u));
                return negation(u[0]);


            case INSTANCE:
                if (u.length != 2 || dt != DTERNAL) throw new InvalidTerm(INSTANCE);
                return inst(u[0], u[1]);
            case PROPERTY:
                if (u.length != 2 || dt != DTERNAL) throw new InvalidTerm(PROPERTY);
                return prop(u[0], u[1]);
            case INSTANCE_PROPERTY:
                if (u.length != 2 || dt != DTERNAL) throw new InvalidTerm(INSTANCE_PROPERTY);
                return instprop(u[0], u[1]);

            case CONJUNCTION:
                return junction(CONJUNCTION, dt, u);
            case DISJUNCTION:
                return junction(DISJUNCTION, dt, u);

            case IMAGE_INT:
            case IMAGE_EXT:
                //if no relation was specified and it's an Image,
                //it must contain a _ placeholder
                if (hasImdex(u)) {
                    //TODO use result of hasImdex in image construction to avoid repeat iteration to find it
                    return image(op, u);
                } else if ((relation == -1) || (relation > u.length)) {
                    throw new InvalidTerm(op,u);
                } else {
                    return finish(op, relation, DTERNAL, tt);
                }

            case DIFF_EXT:
            case DIFF_INT:
                return newDiff(op, tt);
            case INTERSECT_EXT:
                return newIntersectEXT(u);
            case INTERSECT_INT:
                return newIntersectINT(u);

            case SET_EXT:
            case SET_INT:
                if (u.length == 0)
                    throw new InvalidTerm(op,u);
                    //return null; /* emptyset */

        }

        return op.isStatement() ? statement(op, dt, u) : finish(op, relation, dt, tt);

    }


    static boolean validEquivalenceTerm(@NotNull Term t) {
        return !t.isAnyOf(TermIndex.InvalidEquivalenceTerm);
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


    @Nullable
    public Termed make(Op op, int relation, TermContainer subterms) {
        return make(op, relation, subterms, Tense.DTERNAL);
    }

    @NotNull
    public abstract Termed make(Op op, int relation, TermContainer subterms, int dt);

//    @Nullable
//    public Termed the(@NotNull Termed t) {
//        return t.term();
//    }

//    //    public Term theTerm(Termed t) {
////        return the(t).term();
////    }
//    public Term theTerm(Term t) {
//        return the(t).term();
//    }


//    public Term newTerm(Op op, Term... t) {
//        return newTerm(op, -1, t);
//    }


    @Nullable
    public Term newCompound(@NotNull Op op, @NotNull Collection<Term> t) {
        return newCompound(op, -1, t);
    }

    @Nullable
    public Term newCompound(@NotNull Op op, int relation, @NotNull Collection<Term> t) {
        return newCompound(op, relation, TermContainer.the(op, t));
    }

    @Nullable
    public Term newCompound(@NotNull Op op, Term singleton) {
        return newCompound(op, TermVector.the(singleton));
    }


//    @Nullable
//    public <X extends Compound> X transform(@NotNull Compound src, @NotNull CompoundTransform t) {
//        return transform(src, t);
//    }

//    @Nullable
//    public <X extends Compound> X transformRoot(@NotNull Compound src, @NotNull CompoundTransform t) {
//        if (t.testSuperTerm(src)) {
////            Compound xsrc = transform(src, t);
////            if (xsrc!=null)
////                src = xsrc;
//            return (X) t.apply(src, null, 0);
//        }
//        return (X)src;
//    }



    @Nullable
    public Term newCompound(@NotNull Op op, @NotNull TermContainer subs) {
        return newCompound(op, -1, subs);
    }


    @Nullable
    public Term newCompound(@NotNull Op op, int relation, @NotNull TermContainer tt) {
        return the(op, relation, DTERNAL, tt);
    }


    @Nullable
    public Term newDiff(@NotNull Op op, @NotNull TermContainer tt) {

        //corresponding set type for reduction:
        Op set = op == DIFF_EXT ? SET_EXT : SET_INT;

        Term[] t = tt.terms();
        switch (t.length) {
            case 1: {
                Term t0 = t[0];
                if (ellipsisoid(t0))
                    return finish(op, -1, tt);
                return t0;
            }
            case 2:
                Term t0 = t[0];
                Term et0 = t0, et1 = t[1];
                if ((et0.op() == set && et1.op() == set))
                    return subtractSet(set, (Compound) et0, (Compound) et1);

                if (et0.equals(et1))
                    return Terms.empty(set);

                return finish(op, -1, TermContainer.the(op, t));
            default:
                return null;
        }
    }

    @Nullable
    public Term finish(@NotNull Op op, int relation, @NotNull TermContainer tt) {
        return finish(op, relation, Tense.DTERNAL, tt);
    }

    /**
     * step before calling Make, do not call manually from outside
     */
    @Nullable
    public Term finish(@NotNull Op op, int relation, int dt, @NotNull TermContainer args) {

        //Term[] u = args.terms();
        int currentSize = args.size();

        if (op.minSize > 1 && currentSize == 1) {
            //special case: allow for ellipsis to occupy one item even if minArity>1
            Term u0 = args.term(0);
            if ((u0 instanceof Ellipsis) || (u0 instanceof Ellipsis.EllipsisPrototype))
                currentSize++; //increase to make it seem valid and allow constrct below
            else
                return u0; //reduction
        }

        if (!op.validSize(currentSize)) {
            //throw new RuntimeException(Arrays.toString(t) + " invalid size for " + op);
            //if (Global.DEBUG)
                //throw new InvalidTerm(op, relation, dt, args.terms());
            //else
                return null;
        }

        return make(op, relation, TermContainer.the(op, args), dt).term();
    }


    @Nullable
    public Compound inst(Term subj, Term pred) {
        return (Compound) newCompound(INHERIT, TermVector.the(newCompound(SET_EXT, subj), pred));
    }

    @Nullable
    public Compound prop(Term subj, Term pred) {
        return (Compound) newCompound(INHERIT, TermVector.the(subj, newCompound(SET_INT, pred)));
    }

    @Nullable
    public Compound instprop(@NotNull Term subj, @NotNull Term pred) {
        return (Compound) newCompound(INHERIT, TermVector.the(newCompound(SET_EXT, subj), newCompound(SET_INT, pred)));
    }

    @Nullable
    public Term negation(@NotNull Term t) {
        if (t.op() == NEGATE) {
            // (--,(--,P)) = P
            return ((TermContainer) t).term(0);
        }
        return make(NEGATE, -1, TermVector.the(t)).term();
    }

    @Nullable
    public Term image(@NotNull Op o, @NotNull Term[] res) {

        int index = 0, j = 0;
        for (Term x : res) {
            if (x.equals(Imdex)) {
                index = j;
            }
            j++;
        }

        if (index == -1)
            throw new RuntimeException("invalid image subterms: " + Arrays.toString(res));

        int serN = res.length - 1;
        Term[] ser = new Term[serN];
        System.arraycopy(res, 0, ser, 0, index);
        System.arraycopy(res, index + 1, ser, index, (serN - index));
        res = ser;

        return newCompound(
                o,
                index, TermVector.the(res));
    }

    @NotNull
    public Term junction(@NotNull Op op, int t, @NotNull Term... u) {
//        if (u.length == 1)

        if (u.length == 0) {
            return null;
        }

        if (u.length == 1) {
            Term only = u[0];
            //preserve unitary ellipsis
            return ellipsisoid(only) ?
                    finish(op, -1, t, TermContainer.the(only)) : only;

        }

        if (t != DTERNAL) {
            if (op == DISJUNCTION) {
                throw new RuntimeException("invalid temporal disjunction");
            }

            if (t == 0) {
                //special case: 0
                Compound x = (Compound) junctionFlat(op, 0, u);
                if (x == null)
                    return null;
                if (x.size() == 1) {
                    return x.term(0);
                }
                //if (x.op(op))

                return x.op().isTemporal() ? x.dt(0) : x;

                //return x;
            }



           if (u.length == 2) {
                if (u[0].equals(u[1]))
                    return u[0];
            } else {
                //if (Global.DEBUG)
                    //throw new InvalidTerm(op, -1, t, u);
                //else
                    return null;
            }

            Term x = make(op, -1, TermContainer.the(op, u)).term();
            if (!(x instanceof Compound)) return x;

            Compound cx = (Compound) x;

            boolean reversed = cx.term(0) == u[1];
            return cx.dt(reversed ? -t : t);
        } else {
            return junctionFlat(op, t, u);
        }
    }

    public static boolean ellipsisoid(Term only) {
        return (only instanceof Ellipsis) || (only instanceof Ellipsis.EllipsisPrototype);
    }

    /**
     * flattening junction builder, for multi-arg conjunction and disjunction (dt == 0 ar DTERNAL)
     */
    @NotNull
    public Term junctionFlat(@NotNull Op op, int dt, @NotNull Term[] u) {


        assert(dt ==0 || dt == DTERNAL); //throw new RuntimeException("should only have been called with dt==0 or dt==DTERNAL");


        TreeSet<Term> s = new TreeSet();
        UnifiedSet<Term> negs = new UnifiedSet(0);

        flatten(op, u, dt, s, negs);

        //any commutive terms with both a subterm and its negative are invalid
        if (negs.anySatisfy(s::contains))
            return null; //throw new InvalidTerm(op, u);


        if (negs.size() == s.size()) {
            //all subterms negated; apply DeMorgan's Law
            if (op == CONJUNCTION) op = DISJUNCTION;
            else /* (op == DISJUNCTION) */ op = CONJUNCTION;

            Term nn = finish(op, -1, dt, TermSet.the(negs));
            return newCompound(NEGATE, nn);
        } else {
            return finish(op, -1, dt, TermSet.the(s));
        }


    }

    static void flatten(@NotNull Op op, @NotNull Term[] u, int dt, @NotNull Collection<Term> s, @NotNull Set<Term> unwrappedNegations) {
        for (Term x : u) {
            if ((x.op() == op) && (((Compound) x).dt()==dt)) {
                flatten(op, ((Compound) x).terms(), dt, s, unwrappedNegations); //recurse
            } else {
                if (s.add(x)) { //ordinary term, add
                    if (x.op() == NEGATE)
                        unwrappedNegations.add(((Compound)x).term(0));
                }
            }
        }
    }



    @Nullable
    public Term statement(@NotNull Op op, int t, @NotNull Term[] u) {

        switch (u.length) {
            case 2:
                return statement2(op, t, u);
            //case 1:
                //return u[0];
            default:
                throw new RuntimeException("invalid statement arguments: " + Arrays.toString(u));
        }
    }

    @Nullable
    public Term statement2(@NotNull Op op, int dt, final Term[] u) {
        Term subject = u[0];
        Term predicate = u[1];

        //if (subject.equals(predicate))
        //    return null; //subject;

        if (Terms.equalsAnonymous(subject, predicate))
            return null;

        //special statement filters
        switch (op) {


            case EQUIV:
                if (!validEquivalenceTerm(subject)) return null;
                if (!validEquivalenceTerm(predicate)) return null;
                break;

            case IMPLICATION:
                if (subject.isAnyOf(TermIndex.InvalidEquivalenceTerm)) return null;
                if (predicate.isAnyOf(TermIndex.InvalidImplicationPredicate)) return null;

                if (predicate.op() == IMPLICATION) {
                    Term oldCondition = subj(predicate);
                    if ((oldCondition.op() == CONJUNCTION && oldCondition.containsTerm(subject)))
                        return null;

                    return impl2Conj(dt, subject, predicate, oldCondition);
                }



                //filter (factor out) any common subterms iff equal 'dt'
                if ((subject.op() == CONJUNCTION) && (predicate.op() == CONJUNCTION)) {
                    Compound csub = (Compound) subject;
                    Compound cpred = (Compound) predicate;
                    if(csub.dt() == cpred.dt()) {

                        TermContainer subjs = csub.subterms();
                        TermContainer preds = cpred.subterms();

                        MutableSet<Term> common = TermContainer.intersect(subjs, preds);
                        if (!common.isEmpty()) {
                            subject = theTransformed(csub, TermContainer.except(subjs, common));
                            if (subject == null)
                                return null;
                            predicate = theTransformed(cpred, TermContainer.except(preds, common));
                            if (predicate == null)
                                return null;

                            if (Terms.equalsAnonymous(subject, predicate))
                                return null;
                        }
                    }
                }

                break;

        }



        //already tested equality, so go to invalidStatement2:
        if (!Statement.invalidStatement2(subject, predicate)) {
            TermContainer cc = TermContainer.the(op, subject, predicate);
            Termed xx = make(op, -1, cc);
            if (xx != null) {
                Compound x = (Compound) (xx.term());
                if (dt != DTERNAL) {
                    boolean reversed = cc.term(0) == predicate;
                    x = x.dt(reversed ? -dt : dt);
                }
                return x;
            }
        }

        return null;
    }

    @Nullable
    public Term subtractSet(@NotNull Op setType, @NotNull Compound A, @NotNull Compound B) {
        return TermContainer.difference(this, setType, A, B);
    }

    @Nullable
    public Term impl2Conj(int t, Term subject, @NotNull Term predicate, Term oldCondition) {
        Term s = junction(CONJUNCTION, t, subject, oldCondition);
        return s != null ? newCompound(IMPLICATION, t, TermVector.the(s, pred(predicate))) : null;
    }

    @Nullable
    public Term newIntersectINT(@NotNull Term[] t) {
        return newIntersection(t,
                INTERSECT_INT,
                SET_INT,
                SET_EXT);
    }

    @Nullable
    public Term newIntersectEXT(@NotNull Term[] t) {
        return newIntersection(t,
                INTERSECT_EXT,
                SET_EXT,
                SET_INT);
    }

    @Nullable
    public Term newIntersection(@NotNull Term[] t, @NotNull Op intersection, @NotNull Op setUnion, @NotNull Op setIntersection) {
        switch (t.length) {

            case 1:

                Term single = t[0];
                if (single instanceof Ellipsis) {
                    //allow
                    single = finish(intersection, -1, TermContainer.the(intersection, single));
                }

                return single;

            case 2:
                return newIntersection2(t[0], t[1], intersection, setUnion, setIntersection);
            default:
                //HACK use more efficient way
                Term a = newIntersection2(t[0], t[1], intersection, setUnion, setIntersection);
                if (a == null) return null;
                return newIntersection2(
                        a,
                        newIntersection(copyOfRange(t, 2, t.length), intersection, setUnion, setIntersection),
                        intersection, setUnion, setIntersection
                );
        }

    }

    @Nullable
    @Deprecated
    public Term newIntersection2(@NotNull Term term1, @Nullable Term term2, @NotNull Op intersection, @NotNull Op setUnion, @NotNull Op setIntersection) {

        if (term2 == null) {
            throw new NullPointerException();
        }

        Op o1 = term1.op();
        Op o2 = term2.op();

        if ((o1 == setUnion) && (o2 == setUnion)) {
            //the set type that is united
            return TermContainer.union(this, setUnion, (Compound) term1, (Compound) term2);
        }


        if ((o1 == setIntersection) && (o2 == setIntersection)) {
            //the set type which is intersected
            return TermContainer.intersect(this, setIntersection, (Compound) term1, (Compound) term2);
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

            Term[] suffix = o2 == intersection ? ((TermContainer) term2).terms() : new Term[]{term2};

            return finish(intersection, -1,
                    TermSet.the(Terms.concat(
                            ((TermContainer) term1).terms(), suffix
                    ))
            );
        }

        return finish(intersection, -1, TermSet.the(term1, term2));


    }


    public final Term theTransformed(@NotNull Compound csrc, @NotNull TermContainer subs) {
        if (csrc.subterms().equals(subs))
            return csrc;

        return the(csrc.op(), csrc.relation(), csrc.dt(), subs);
    }

}
