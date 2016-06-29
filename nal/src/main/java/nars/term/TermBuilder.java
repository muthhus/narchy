package nars.term;

import com.gs.collections.api.set.MutableSet;
import com.gs.collections.impl.set.mutable.UnifiedSet;
import nars.Op;
import nars.index.TermIndex;
import nars.nal.meta.match.Ellipsis;
import nars.nal.meta.match.Ellipsislike;
import nars.term.compound.Statement;
import nars.term.container.TermContainer;
import nars.term.container.TermSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.TreeSet;

import static java.util.Arrays.copyOfRange;
import static nars.Op.*;
import static nars.nal.Tense.DTERNAL;
import static nars.term.Terms.empty;
import static nars.term.compound.Statement.pred;
import static nars.term.compound.Statement.subj;

/**
 * Created by me on 1/2/16.
 */
public abstract class TermBuilder {



    @Nullable
    public Term build(@NotNull Op op, int dt, @NotNull Term[] u) throws InvalidTerm {

        /* special handling */
        switch (op) {
            case NEG:
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

            case CONJ:
            case DISJ:
                return junction(op, dt, u);

            case IMGi:
            case IMGe:
                //if no relation was specified and it's an Image,
                //it must contain a _ placeholder
                if (hasImdex(u)) {
                    //TODO use result of hasImdex in image construction to avoid repeat iteration to find it
                    return image(op, u);
                } else if ((dt < 0) || (dt > u.length)) {
                    throw new InvalidTerm(op,u);
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
                return statement(op, dt, u);

            case PROD:
                if (u.length == 0)
                    return Terms.ZeroProduct;
                break;

        }

        return finish(op, dt, u);
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


    public abstract Term newCompound(Op op, int dt, TermContainer subterms);


    @Nullable
    public Term build(@NotNull Op op, @NotNull Term... tt) {
        return build(op, DTERNAL, tt);
    }


    @Nullable
    public Term newDiff(@NotNull Op op, @NotNull Term[] t) {

        //corresponding set type for reduction:
        Op set = op == DIFFe ? SETe : SETi;

        switch (t.length) {
            case 1:
                Term t0 = t[0];
                if (t0 instanceof Ellipsislike)
                    return finish(op, t0);
                else
                    return t0;
            case 2:
                Term et0 = t[0], et1 = t[1];
                if ((et0.op() == set && et1.op() == set))
                    return subtractSet(set, (Compound) et0, (Compound) et1);
                else
                    return et0.equals(et1) ?
                            empty(set) :
                            finish(op, t);
            default:
                return null;
        }
    }



    @Nullable
    public final Term finish(@NotNull Op op, @NotNull Term... args) {
        return finish(op, DTERNAL, args);
    }

    @Nullable
    public final Term finish(@NotNull Op op, int dt, @NotNull Term... args) {
        return finish(op, dt, TermContainer.the(op, args));
    }


    /**
     * step before calling Make, do not call manually from outside
     */
    @Nullable
    final Term finish(@NotNull Op op, int dt, @NotNull TermContainer args) {

        int s = args.size();

        if (s == 1 && op.minSize > 1) {
            //special case: allow for ellipsis to occupy one item even if minArity>1
            Term a0 = args.term(0);
            if (!(a0 instanceof Ellipsislike)) {
                throw new RuntimeException("invalid size " + s + " for " + op);
                //return u0; //reduction
            }
        }

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
    public final Term negation(@NotNull Term t) {
        if (t.op() == NEG) {
            // (--,(--,P)) = P
            return ((TermContainer) t).term(0);
        } else {
            return finish(NEG, t);
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

        return finish( o, index, ser);
    }

    @Nullable
    public Term junction(@NotNull Op op, int dt, final @NotNull Term... u) {

        int ul = u.length;

        if (ul == 0) {
            return null;
        }

        if (ul == 1) {
            Term only = u[0];
            //preserve unitary ellipsis for patterns etc
            if (only instanceof Ellipsislike)
                return finish(op, dt, only);
            else
                return only;

        }

        if (dt == DTERNAL) {
            return junctionFlat(op, dt, u);
        } else {
            if (op == DISJ) {
                throw new RuntimeException("invalid temporal disjunction");
            }

            if (dt == 0) {
                //special case: 0
                Compound x = (Compound) junctionFlat(op, 0, u);
                if (x == null) {
                    return null;
                } else if (x.size() == 1) {
                    return x.term(0);
                } else {
                    return x.op().temporal ? x.dt(0) : x;
                }
            } else {


                if (ul != 2) {
                    //if (Global.DEBUG)
                    //throw new InvalidTerm(op, DTERNAL, t, u);
                    //else
                    return null;
                }

                if (u[0].equals(u[1]))
                    return u[0];

                if (u[0].compareTo(u[1]) == +1) {
                    //it will be reversed in commutative sorting, so invert dt
                    dt = -dt;
                }

                return finish(op, dt, u);
            }
        }
    }


    /**
     * flattening junction builder, for multi-arg conjunction and disjunction (dt == 0 ar DTERNAL)
     */
    @Nullable
    public Term junctionFlat(@NotNull Op op, int dt, @NotNull Term[] u) {


        assert(dt ==0 || dt == DTERNAL); //throw new RuntimeException("should only have been called with dt==0 or dt==DTERNAL");


        TreeSet<Term> s = new TreeSet();
        UnifiedSet<Term> negs = flatten(op, u, dt, s, null);

        boolean negate = false;
        int n = s.size();
        if (n == 1) {
            return s.iterator().next();
        }

        //Co-Negated Subterms - any commutive terms with both a subterm and its negative are invalid
        if (negs!=null) {
            if (op == DISJ && negs.anySatisfy(s::contains))
                return null; //throw new InvalidTerm(op, u);
            //for conjunction, this is handled by the Task normalization process to allow the co-negations for naming concepts


            //if all subterms negated; apply DeMorgan's Law
            if ((dt == DTERNAL) && (negs.size() == n)) {
                op = (op == CONJ) ? DISJ : CONJ;
                negate = true;
            }
        }

        if (negate) {
            return negation( finish(op, dt, negs.toArray(new Term[n])) );
        } else {
            return finish(op, dt, s.toArray(new Term[n]));
        }
    }

    static UnifiedSet<Term> flatten(@NotNull Op op, @NotNull Term[] u, int dt, @NotNull Collection<Term> s, @NotNull UnifiedSet<Term> unwrappedNegations) {
        for (Term x : u) {
            if ((x.op() == op) && (((Compound) x).dt()==dt)) {
                unwrappedNegations = flatten(op, ((Compound) x).terms(), dt, s, unwrappedNegations); //recurse
            } else {
                if (s.add(x)) { //ordinary term, add
                    if (x.op() == NEG) {
                        if (unwrappedNegations == null)
                            unwrappedNegations = new UnifiedSet<>(1);
                        unwrappedNegations.add(((Compound) x).term(0));
                    }
                }
            }
        }
        return unwrappedNegations;
    }



    @Nullable
    public Term statement(@NotNull Op op, int t, @NotNull Term[] u) {

        switch (u.length) {
            case 2:
                return statement2(op, t, u);
            //case 1:
                //return u[0];
            default:
                //throw new RuntimeException("invalid statement: args=" + Arrays.toString(u));
                return null;
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


            case EQUI:
                if (!validEquivalenceTerm(subject) || !validEquivalenceTerm(predicate))
                    return null;
                break;

            case IMPL:
                if (subject.isAnyOf(TermIndex.InvalidEquivalenceTerm) ||
                    predicate.isAnyOf(TermIndex.InvalidImplicationPredicate))
                    return null;

                if (predicate.op() == IMPL) {
                    Term oldCondition = subj(predicate);
                    if ((oldCondition.op() == CONJ && oldCondition.containsTerm(subject)))
                        return null;
                    else
                        return impl2Conj(dt, subject, predicate, oldCondition);
                }



                //filter (factor out) any common subterms iff equal 'dt'
                if ((subject.op() == CONJ) && (predicate.op() == CONJ)) {
                    Compound csub = (Compound) subject;
                    Compound cpred = (Compound) predicate;
                    if(csub.dt() == cpred.dt()) {

                        TermContainer subjs = csub.subterms();
                        TermContainer preds = cpred.subterms();

                        MutableSet<Term> common = TermContainer.intersect(subjs, preds);
                        if (!common.isEmpty()) {
                            subject = build(csub, TermContainer.except(subjs, common));
                            if (subject == null)
                                return null;
                            predicate = build(cpred, TermContainer.except(preds, common));
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
            Termed xx = finish(op, subject, predicate);
            if (xx != null) {
                Compound x = (Compound) (xx.term());
                if (dt != DTERNAL) {
                    boolean reversed = (x.term(0) == predicate);
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
        Term s = junction(CONJ, t, subject, oldCondition);
        return s != null ? build(IMPL, s, pred(predicate)) : null;
    }

    @Nullable
    public Term newIntersectINT(@NotNull Term[] t) {
        return newIntersection(t,
                SECTi,
                SETi,
                SETe);
    }

    @Nullable
    public Term newIntersectEXT(@NotNull Term[] t) {
        return newIntersection(t,
                SECTe,
                SETe,
                SETi);
    }

    @Nullable
    public Term newIntersection(@NotNull Term[] t, @NotNull Op intersection, @NotNull Op setUnion, @NotNull Op setIntersection) {
        switch (t.length) {

            case 1:

                Term single = t[0];
                if (single instanceof Ellipsislike) {
                    //allow
                    return finish(intersection, single);
                } else {
                    return single;
                }

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
            return finish(intersection, DTERNAL,
                    TermSet.concat(
                        ((TermContainer) term1).terms(),
                        o2 == intersection ? ((TermContainer) term2).terms() : new Term[]{term2}
                    )
            );
        }

        return finish(intersection, term1, term2);
    }


    @Nullable
    public Compound intersect(@NotNull Op o, @NotNull Compound a, @NotNull Compound b) {
        if (a.equals(b))
            return a;

        MutableSet<Term> s = TermContainer.intersect(
                /*(TermContainer)*/ a, /*(TermContainer)*/ b
        );
        return s.isEmpty() ? null : (Compound) build(o, s.toArray(new Term[s.size()]));
    }

    @Nullable
    public Compound union(@NotNull Op o, @NotNull Compound term1, @NotNull Compound term2) {
        TermContainer u = TermContainer.union(term1, term2);
        if (u == term1)
            return term1;
        else if (u == term2)
            return term2;
        else
            return (Compound)finish(o, DTERNAL, u);
    }

    @Nullable
    public final Term build(@NotNull Compound csrc, @NotNull Term[] newSubs) {
        if (csrc.subterms().equivalent(newSubs))
            return csrc;
        else
            return build(csrc.op(), csrc.dt(), newSubs);
    }

    public final Term build(@NotNull Compound csrc, TermContainer newSubs) {
        if (csrc.subterms().equals(newSubs))
            return csrc;
        else
            return build(csrc.op(), csrc.dt(), newSubs.terms());
    }

}
