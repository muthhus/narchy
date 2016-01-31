package nars.term;

import com.google.common.collect.Lists;
import com.gs.collections.api.set.MutableSet;
import nars.$;
import nars.Global;
import nars.Op;
import nars.nal.Tense;
import nars.nal.meta.PremiseAware;
import nars.nal.meta.PremiseMatch;
import nars.nal.meta.match.Ellipsis;
import nars.nal.meta.match.EllipsisMatch;
import nars.nal.nal8.Operator;
import nars.nal.op.ImmediateTermTransform;
import nars.term.compound.GenericCompound;
import nars.term.container.TermContainer;
import nars.term.container.TermSet;
import nars.term.container.TermVector;
import nars.term.transform.CompoundTransform;
import nars.term.transform.VariableNormalization;
import nars.term.transform.VariableTransform;
import nars.term.transform.subst.Subst;
import nars.term.variable.Variable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import static java.util.Arrays.copyOfRange;
import static nars.Op.*;
import static nars.nal.Tense.ITERNAL;
import static nars.term.Statement.pred;
import static nars.term.Statement.subj;

/**
 * Created by me on 1/2/16.
 */
public interface TermBuilder {



    /** allows using the single variable normalization,
     * which is safe if the term doesnt contain pattern variables */
    @NotNull
    static VariableTransform normalizeFast(@NotNull Compound target) {
        return target.vars() == 1 ? VariableNormalization.singleVariableNormalization : new VariableNormalization();
    }

    static boolean validEquivalenceTerm(@NotNull Term t) {
        return !t.isAny(TermIndex.InvalidEquivalenceTerm);
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
    default Termed make(Op op, int relation, TermContainer subterms) {
        return make(op, relation, subterms, Tense.ITERNAL);
    }

    @NotNull
    Termed make(Op op, int relation, TermContainer subterms, int dt);

    /** unifies a term with this; by default it passes through unchanged */
    @Nullable
    default Termed the(Term t) {
        return t;
    }

    @Nullable
    default Termed the(@NotNull Termed t) {
        return the(t.term());
    }
//    default Term theTerm(Termed t) {
//        return the(t).term();
//    }
    default Term theTerm(Term t) {
        return the(t).term();
    }

    @Nullable
    default Termed normalized(@NotNull Term t) {
        if (t.isNormalized()) {
            return t;
        }

        Compound tx = transform((Compound) t, normalizeFast((Compound) t));
        if (tx != null)
            ((GenericCompound)tx.term()).setNormalized();

        return tx;
    }

//    default Term newTerm(Op op, Term... t) {
//        return newTerm(op, -1, t);
//    }


    @Nullable
    default Term newTerm(@NotNull Op op, @NotNull Collection<Term> t) {
        return newTerm(op, -1, t);
    }
    @Nullable
    default Term newTerm(@NotNull Op op, int relation, @NotNull Collection<Term> t) {
        return newTerm(op, relation, TermContainer.the(op, t));
    }
    @Nullable
    default Term newTerm(@NotNull Op op, Term singleton) {
        return newTerm(op, -1, new TermVector(singleton));
    }
    @Nullable
    default Term newTerm(@NotNull Op op, Term... x) {
        return newTerm(op, -1, TermContainer.the(op, x));
    }


    @Nullable
    default <X extends Compound> X transform(@NotNull Compound src, @NotNull CompoundTransform t) {
        return transform(src, t, true);
    }

//    @Nullable
//    default <X extends Compound> X transformRoot(@NotNull Compound src, @NotNull CompoundTransform t) {
//        if (t.testSuperTerm(src)) {
////            Compound xsrc = transform(src, t);
////            if (xsrc!=null)
////                src = xsrc;
//            return (X) t.apply(src, null, 0);
//        }
//        return (X)src;
//    }

    @Nullable
    default <X extends Compound> X transform(@NotNull Compound src, @NotNull CompoundTransform t, boolean requireEqualityForNewInstance) {
        if (!t.testSuperTerm(src)) {
            return (X) src; //nothing changed
        }

        Term[] newSubterms = new Term[src.size()];

        int mods = transform(src, t, newSubterms, 0);

        if (mods == -1) {
            return null;
        } else if (!requireEqualityForNewInstance || (mods > 0)) {
            return (X) newTerm(src, TermContainer.the(src.op(), newSubterms));
        }
        return (X) src; //nothing changed
    }


    /** returns how many subterms were modified, or -1 if failure (ex: results in invalid term) */
    default <T extends Term> int transform(@NotNull Compound src, @NotNull CompoundTransform<Compound<T>, T> trans, Term[] target, int level) {
        int n = src.size();

        int modifications = 0;

        for (int i = 0; i < n; i++) {
            Term x = src.term(i);
            if (x == null)
                throw new RuntimeException("null subterm");

            if (trans.test(x)) {

                Term y = trans.apply( (Compound<T>)src, (T) x, level);
                if (y == null)
                    return -1;

                if (x!=y) {
                    modifications++;
                    x = y;
                }

            } else if (x instanceof Compound) {
                //recurse
                Compound cx = (Compound) x;
                if (trans.testSuperTerm(cx)) {

                    Term[] yy = new Term[cx.size()];
                    int submods = transform(cx, trans, yy, level + 1);

                    if (submods == -1) return -1;
                    if (submods > 0) {

                        //method 1: using termindex
//                        x = newTerm(cx.op(), cx.relation(), cx.t(),
//                            TermContainer.the(cx.op(), yy)
//                        );

                        //method 2: on heap
                        Op op = cx.op();
                        x = $.the(op, cx.relation(), cx.t(),
                            TermContainer.the(op, yy)
                        );

                        if (x == null)
                            return -1;
                        modifications+= (cx!=x) ? 1 : 0;
                    }
                }
            }
            target[i] = x;
        }

        return modifications;
    }

    @Nullable
    default Term newTerm(@NotNull Compound csrc, TermContainer subs) {
        if (csrc.subterms().equals(subs))
            return csrc;
        return newTerm(csrc.op(), csrc.relation(), csrc.t(), subs);
    }

    @Nullable
    default Term newTerm(@NotNull Op op, TermContainer subs) {
        return newTerm(op, -1, subs);
    }



    @Nullable
    default Term newTerm(@NotNull Op op, int relation, TermContainer tt) {
        return newTerm(op, relation, ITERNAL, tt);
    }

    @Nullable
    default Term newTerm(@NotNull Op op, int relation, int t, @Nullable TermContainer tt) {

//        if (tt == null)
//            return null;

        Term[] u = tt.terms();

        /* special handling */
        switch (op) {
            case NEGATE:
                if (u.length!=1)
                    throw new RuntimeException("invalid negation subterms: " + Arrays.toString(u));
                return negation(u[0]);


            case INSTANCE:
                if (u.length!=2 || t!= ITERNAL) throw new RuntimeException("invalid inst");
                return inst(u[0], u[1]);
            case PROPERTY:
                if (u.length!=2 || t!= ITERNAL) throw new RuntimeException("invalid prop");
                return prop(u[0], u[1]);
            case INSTANCE_PROPERTY:
                if (u.length!=2 || t!= ITERNAL) throw new RuntimeException("invalid instprop");
                return instprop(u[0], u[1]);

            case CONJUNCTION:
                return junction(CONJUNCTION, t, u);
            case DISJUNCTION:
                return junction(DISJUNCTION, t, u);

            case IMAGE_INT:
            case IMAGE_EXT:
                //if no relation was specified and it's an Image,
                //it must contain a _ placeholder
                if (hasImdex(u)) {
                    //TODO use result of hasImdex in image construction to avoid repeat iteration to find it
                    return image(op, u);
                }
                if ((relation == -1) || (relation > u.length))
                    return null;

                break; //continued below

            case DIFF_EXT:
            case DIFF_INT:
                return newDiff(op, tt);
            case INTERSECT_EXT:
                return newIntersectEXT(u);
            case INTERSECT_INT:
                return newIntersectINT(u);
        }

        if (op.isStatement()) {

            return statement(op, t, u);

        } else {

            return finish(op, relation, t, tt);

        }

    }

    @Nullable
    default Term newDiff(@NotNull Op op, @NotNull TermContainer tt) {

        //corresponding set type for reduction:
        Op set = op == DIFF_EXT ? SET_EXT : SET_INT;

        Term[] t = tt.terms();
        switch (t.length) {
            case 1:
                if (t[0] instanceof Ellipsis)
                    return finish(op, -1, tt);
                return t[0];
            case 2:
                Term et0 = t[0], et1 = t[1];
                if ((et0.op(set) && et1.op(set) ))
                    return subtractSet(set, (Compound)et0, (Compound)et1);

                if (et0.equals(et1))
                    return Terms.empty(set);

                return finish(op, -1, TermContainer.the(op, tt));
            default:
                return null;
        }
    }

    @Nullable
    default Term finish(@NotNull Op op, int relation, @NotNull TermContainer tt) {
        return finish(op, relation, Tense.ITERNAL, tt);
    }

    /** step before calling Make, do not call manually from outside */
    @Nullable
    default Term finish(@NotNull Op op, int relation, int dt, @NotNull TermContainer args) {

        Term[] u = args.terms();
        int currentSize = u.length;

        if (op.minSize > 1 && currentSize == 1) {
            //special case: allow for ellipsis to occupy one item even if minArity>1
            if (u[0] instanceof Ellipsis)
                currentSize++; //increase to make it seem valid and allow constrct below
            else
              return u[0]; //reduction
        }

        if (!op.validSize(currentSize)) {
            //throw new RuntimeException(Arrays.toString(t) + " invalid size for " + op);
            throw new InvalidTermConstruction(op, relation, dt, args.terms());
            //return null;
        }

        return make(op, relation, TermContainer.the(op, args), dt).term();

    }

    public static final class InvalidTermConstruction extends RuntimeException {
        private final Op op;
        private final int rel;
        private final int dt;
        private final Term[] args;

        public InvalidTermConstruction(Op op, int rel, int dt, Term[] args) {

            this.op = op;
            this.rel = rel;
            this.dt = dt;
            this.args = args;
        }

        @Override
        public String getMessage() {
            return toString();
        }

        @Override
        public String toString() {
            return "InvalidTermConstruction{" +
                    "op=" + op +
                    ", rel=" + rel +
                    ", dt=" + dt +
                    ", args=" + Arrays.toString(args) +
                    '}';
        }
    }


    @Nullable
    default Term inst(Term subj, Term pred) {
        return newTerm(INHERIT, newTerm(SET_EXT, subj), pred);
    }
    @Nullable
    default Term prop(Term subj, Term pred) {
        return newTerm(INHERIT, subj, newTerm(SET_INT, pred));
    }
    @Nullable
    default Term instprop(Term subj, Term pred) {
        return newTerm(INHERIT, newTerm(SET_EXT, subj), newTerm(SET_INT, pred));
    }

    default Term negation(@NotNull Term t) {
        if (t.op(NEGATE)) {
            // (--,(--,P)) = P
            return ((TermContainer) t).term(0);
        }
        return make(NEGATE, -1, new TermVector(t)).term();
    }

    @Nullable
    default Term image(@NotNull Op o, @NotNull Term[] res) {

        int index = 0, j = 0;
        for (Term x : res) {
            if (x.equals(Imdex)) {
                index = j;
            }
            j++;
        }

        if (index == -1) {
            throw new RuntimeException("invalid image subterms: " + Arrays.toString(res));
        } else {
            int serN = res.length - 1;
            Term[] ser = new Term[serN];
            System.arraycopy(res, 0, ser, 0, index);
            System.arraycopy(res, index + 1, ser, index, (serN - index));
            res = ser;
        }

        return newTerm(
                o,
                index, new TermVector(res));
    }

    @Nullable
    default Term junction(@NotNull Op op, int t, @NotNull Term... u) {
//        if (u.length == 1)
//            return u[0];


        if (t!=ITERNAL) {
            if (op == DISJUNCTION) {
                throw new RuntimeException("invalid temporal disjunction");
            }

            if (t == 0) {
                //special case: 0
                Term x = junction(op, 0, TermSet.the(u));
                if (x.op(op))
                    return ((Compound)x).t(0);
                return x;
            }

            if (u.length!=2) {
                throw new RuntimeException
                ("invalid temporal conjunction: " + op + " " + t + " "+ Arrays.toString(u));
                //return null;
            }

            if (u[0].equals(u[1])) return u[0];


            Term x = make(op, -1, TermContainer.the(op, u)).term();
            if (!x.isCompound()) return x;

            Compound cx = (Compound)x;

            boolean reversed = cx.term(0)==u[1];
            return cx.t(reversed ? -t : t);
        } else {
            return junction(op, t, Lists.newArrayList(u));
        }
    }

    /** flattening junction builder, don't use with temporal relation */
    @Nullable
    default Term junction(@NotNull Op op, int dt, @NotNull Iterable<Term> u) {

//        if (t!=ITERNAL) {
//            return junction(op, t, Iterables.toArray(u, Term.class));
//        }

        final boolean[] done = {true};

        //TODO use a more efficient flattening that doesnt involve recursion and multiple array creations
        TreeSet<Term> s = new TreeSet();
        u.forEach(x -> {
            if (x.op(op) && (((Compound)x).t()==dt) ) {
                for (Term y : ((TermContainer) x).terms()) {
                    if (s.add(y))
                        if (y.op(op))
                            done[0] = false;
                }
            } else {
                s.add(x);
            }
        });

//        if (s.size() == 1) {
//            return s.iterator().next();
//        }
//        if (s.size() == 2 && t!=ITERNAL) {
//            Iterator<Term> ii = s.iterator();
//            return junction(op, t, ii.next(), ii.next());
//        }

        return !done[0] ? junction(op, dt, s) :
                finish(op, -1, dt, TermSet.the(s));
    }

    @Nullable
    default Term statement(@NotNull Op op, int t, @NotNull Term[] u) {

        switch (u.length) {
            case 2: return statement2(op, t, u);
            case 1: return u[0];
            default:
                throw new RuntimeException("invalid statement arguments: " + Arrays.toString(u));
        }
    }

    @Nullable
    default Term statement2(@NotNull Op op, int t, Term[] u) {
        Term subject = u[0];
        Term predicate = u[1];

        //DEBUG:
        //if (subject == null || predicate == null)
        //    throw new RuntimeException("invalid statement terms");

        //special statement filters
        switch (op) {


            case EQUIV:
                if (!validEquivalenceTerm(subject)) return null;
                if (!validEquivalenceTerm(predicate)) return null;
                break;

            case IMPLICATION:
                if (subject.isAny(TermIndex.InvalidEquivalenceTerm)) return null;
                if (predicate.isAny(TermIndex.InvalidImplicationPredicate)) return null;

                if (predicate.isAny(ImplicationsBits)) {
                    Term oldCondition = subj(predicate);
                    if ((oldCondition.isAny(ConjunctivesBits)) && oldCondition.containsTerm(subject))
                        return null;

                    return impl2Conj(t, subject, predicate, oldCondition);
                }
                break;

        }

        if (u[0].equals(u[1]))
            return u[0];

        //already tested equality, so go to invalidStatement2:
        if (!Statement.invalidStatement2(u[0], u[1])) {
            TermContainer cc = TermContainer.the(op, u);
            Compound x = ((Compound) make(op, -1, cc).term());
            if(t!= ITERNAL) {
                boolean reversed = cc.term(0)==u[1];
                x = x.t(reversed ? -t : t);
            }
            return x;
        }

        return null;
    }

    @Nullable
    default Term subtractSet(@NotNull Op setType, @NotNull Compound A, @NotNull Compound B) {
        return TermContainer.difference(this, setType, A, B);
    }

    @Nullable
    default Term impl2Conj(int t, Term subject, @NotNull Term predicate, Term oldCondition) {
        Term s = junction(CONJUNCTION, t, subject, oldCondition);
        if (s == null)
            return null;

        return newTerm(IMPLICATION, t, new TermVector(s, pred(predicate)));
    }

    @Nullable
    default Term newIntersectINT(@NotNull Term[] t) {
        return newIntersection(t,
                INTERSECT_INT,
                SET_INT,
                SET_EXT);
    }

    @Nullable
    default Term newIntersectEXT(@NotNull Term[] t) {
        return newIntersection(t,
                INTERSECT_EXT,
                SET_EXT,
                SET_INT);
    }

    @Nullable
    default Term newIntersection(@NotNull Term[] t, @NotNull Op intersection, @NotNull Op setUnion, @NotNull Op setIntersection) {
        switch (t.length) {
            case 0:
                return t[0];
            case 1:

                Term single = t[0];
                if (single instanceof Ellipsis) {
                    //allow
                    return finish(intersection, -1, TermContainer.the(intersection, single));
                }

                return single;

            case 2:
                return newIntersection2(t[0], t[1], intersection, setUnion, setIntersection);
            default:
                //HACK use more efficient way
                return newIntersection2(
                    newIntersection2(t[0], t[1], intersection, setUnion, setIntersection),
                    newIntersection(copyOfRange(t, 2, t.length), intersection, setUnion, setIntersection),
                    intersection, setUnion, setIntersection
                );
        }

        //return newCompound(intersection, t, -1, true);
    }

    @Nullable
    @Deprecated default Term newIntersection2(@NotNull Term term1, @Nullable Term term2, @NotNull Op intersection, @NotNull Op setUnion, @NotNull Op setIntersection) {

        if(term2 == null) {
            throw new NullPointerException();
        }

        Op o1 = term1.op();
        Op o2 = term2.op();

        if ((o1 == setUnion) && (o2 == setUnion)) {
            //the set type that is united
            return newTerm(setUnion, TermSet.union((Compound) term1, (Compound) term2));
        }


        if ((o1 == setIntersection) && (o2 == setIntersection)) {
            //the set type which is intersected
            MutableSet<Term> i = TermContainer.intersect((Compound) term1, (Compound) term2);
            return newTerm(setIntersection, i);
        }

        if (o2 == intersection && o1!=intersection) {
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


    /** returns the resolved term according to the substitution    */
    @Nullable
    default Term transform(@NotNull Compound src, @NotNull Subst f, boolean fullMatch) {

        if (f.isEmpty()) {
            return fullMatch ? null : src;
        }

        Term y = f.getXY(src);
        if (y!=null)
            return y;

        int len = src.size();

        List<Term> sub = Global.newArrayList(len /* estimate */);

        for (int i = 0; i < len; i++) {
            Term t = src.term(i);
            if (!apply(t, f, sub)) {
                if (fullMatch)
                    return null;
            }
        }

        TermContainer cc = TermContainer.the(src.op(), sub);
        Term result = newTerm(src, cc);


        //apply any known immediate transform operators
        //TODO decide if this is evaluated incorrectly somehow in reverse
        if (result!=null && isOperation(result)) {
            ImmediateTermTransform tf = f.getTransform(Operator.operatorTerm((Compound)result));
            if (tf!=null) {
                result = applyImmediateTransform(f, result, tf);
            }
        }

        if (result == null && !fullMatch)
            result = src;

        return result;
    }



    @Nullable
    default Term applyImmediateTransform(Subst f, Term result, ImmediateTermTransform tf) {

        //Compound args = (Compound) Operator.opArgs((Compound) result).apply(f);
        Compound args = Operator.opArgs((Compound) result);

        return ((tf instanceof PremiseAware) && (f instanceof PremiseMatch)) ?
                ((PremiseAware) tf).function(args, (PremiseMatch) f) :
                tf.function(args, this);
    }


    @Nullable
    default Term apply(@NotNull Subst f, Term src) {

        if (f.isEmpty()) {
            //return fullMatch ? null : src;
            return src;
        }

        if (src instanceof Compound) {
            return transform((Compound)src, f, false);
        } else if (src instanceof Variable) {
            Term x = f.getXY(src);
            if (x != null)
                return x;
        }

        return src;

    }


    /** resolve the this term according to subst by appending to sub.
     * return false if this term fails the substitution */
    default boolean apply(Term src, @NotNull Subst f, @NotNull Collection<Term> sub) {
        Term u = apply(f, src);
        if (u == null) {
            u = src;
        }
        /*else
            changed |= (u!=this);*/

        if (u instanceof EllipsisMatch) {
            EllipsisMatch m = (EllipsisMatch)u;
            m.apply(sub);
        } else {
            sub.add(u);
        }

        return true;
    }


}
