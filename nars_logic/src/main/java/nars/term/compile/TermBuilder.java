package nars.term.compile;

import com.gs.collections.api.set.MutableSet;
import nars.Global;
import nars.Op;
import nars.nal.PremiseAware;
import nars.nal.PremiseMatch;
import nars.nal.nal7.Tense;
import nars.nal.nal8.Operator;
import nars.nal.op.ImmediateTermTransform;
import nars.term.*;
import nars.term.compound.Compound;
import nars.term.compound.GenericCompound;
import nars.term.match.Ellipsis;
import nars.term.match.EllipsisMatch;
import nars.term.transform.CompoundTransform;
import nars.term.transform.Subst;
import nars.term.transform.VariableNormalization;
import nars.term.transform.VariableTransform;
import nars.term.variable.Variable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import static java.util.Arrays.copyOfRange;
import static nars.Op.*;
import static nars.term.Statement.pred;
import static nars.term.Statement.subj;

/**
 * Created by me on 1/2/16.
 */
public interface TermBuilder {



    /** allows using the single variable normalization,
     * which is safe if the term doesnt contain pattern variables */
    static VariableTransform normalizeFast(Compound target) {
        return target.vars() == 1 ? VariableNormalization.singleVariableNormalization : new VariableNormalization();
    }

    static boolean validEquivalenceTerm(Term t) {
        return !t.isAny(TermIndex.InvalidEquivalenceTerm);
//        if ( instanceof Implication) || (subject instanceof Equivalence)
//                || (predicate instanceof Implication) || (predicate instanceof Equivalence) ||
//                (subject instanceof CyclesInterval) || (predicate instanceof CyclesInterval)) {
//            return null;
//        }
    }

    static boolean hasImdex(Term[] r) {
        for (Term x : r) {
            //        if (t instanceof Compound) return false;
//        byte[] n = t.bytes();
//        if (n.length != 1) return false;
            if (x.equals(Op.Imdex)) return true;
        }
        return false;
    }


    Termed make(Op op, int relation, TermContainer subterms);

    default Termed make(Op op, TermContainer subterms) {
        return make(op, -1, subterms);
    }

    /** unifies a term with this; by default it passes through unchanged */
    default Termed the(Term t) {
        return t;
    }

    default Termed the(Termed t) {
        return the(t.term());
    }
    default Term theTerm(Termed t) {
        return the(t).term();
    }
    default Term theTerm(Term t) {
        return the(t).term();
    }

    default Termed normalized(Term t) {
        if (t.isNormalized()) {
            return t;
        }

        Compound x = transform((Compound) t, normalizeFast((Compound) t));
        Termed tx = the(x);
        //if (x != t) {
            //if modified, set normalization flag HACK
            ((GenericCompound)tx.term()).setNormalized();
        //}
        return tx;
    }

//    default Term newTerm(Op op, Term... t) {
//        return newTerm(op, -1, t);
//    }


    default Term newTerm(Op op, Collection<Term> t) {
        return newTerm(op, -1, t);
    }
    default Term newTerm(Op op, int relation, Collection<Term> t) {
        return newTerm(op, relation, TermContainer.the(op, t));
    }
    default Term newTerm(Op op, Term singleton) {
        return newTerm(op, -1, new TermVector(singleton));
    }
    default Term newTerm(Op op, Term... x) {
        return newTerm(op, -1, TermContainer.the(op, x));
    }


    default <X extends Compound> X transform(Compound src, CompoundTransform t) {
        return transform(src, t, true);
    }

    default <X extends Compound> X transform(Compound src, CompoundTransform t, boolean requireEqualityForNewInstance) {
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
    default <T extends Term> int transform(Compound src, CompoundTransform<Compound<T>, T> trans, Term[] target, int level) {
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
                        x = newTerm(cx, TermContainer.the(cx.op(), yy));
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

    default Term newTerm(Compound csrc, TermContainer subs) {
        if (csrc.subterms().equals(subs))
            return csrc;
        return newTerm(csrc.op(), csrc.relation(), subs);
    }
    default Term newTerm(Op op, TermContainer subs) {
        return newTerm(op, -1, subs);
    }



//    /** "clone"  */
//    @Deprecated default Term newTerm(Compound csrc, Term... subs) {
//        if (csrc.subterms().equals(subs))
//            return csrc;
//        return newTerm(csrc.op(), csrc.relation(), subs);
//    }


    default Term newTerm(Op op, int relation, TermContainer tt) {
        return newTerm(op, relation, Tense.ITERNAL, tt);
    }

    default Term newTerm(Op op, int relation, int t, TermContainer tt) {

        if (tt == null)
            return null;

        Term[] u = tt.terms();

        /* special handling */
        switch (op) {
            case NEGATE:
                if (u.length!=1)
                    throw new RuntimeException("invalid negation subterms: " + Arrays.toString(u));
                return negation(u[0]);
            case INSTANCE:
                return inst(u[0], u[1]);
            case PROPERTY:
                return prop(u[0], u[1]);
            case INSTANCE_PROPERTY:
                return instprop(u[0], u[1]);
            case CONJUNCTION:
                return junction(CONJUNCTION, t, tt);
            case DISJUNCTION:
                return junction(DISJUNCTION, t, tt);
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
                //throw new RuntimeException("invalid index relation: " + relation + " for args " + Arrays.toString(t));

                break;
            case DIFF_EXT:
            case DIFF_INT:
                return newDiff(op, tt);
            case INTERSECT_EXT: return newIntersectEXT(u);
            case INTERSECT_INT: return newIntersectINT(u);
        }

        if (op.isStatement()) {

            return statement(op, t, u);

        } else {

            return finish(op, relation, tt);

        }

    }

    default Term newDiff(Op op, TermContainer tt) {

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
                    return null;
                return finish(op, -1, TermContainer.the(op, tt));
            default:
                return null;
        }
    }


    /** step before calling Make, do not call manually from outside */
    default Term finish(Op op, int relation, TermContainer tt) {

        Term[] t = tt.terms();
        int currentSize = t.length;

        boolean valid;
        if (op.minSize > 1 && currentSize == 1) {
            //special case: allow for ellipsis to occupy one item even if minArity>1
            if (t[0] instanceof Ellipsis)
                currentSize++; //increase to make it seem valid and allow constrct below
            else
              return t[0]; //reduction
        }

        if (!op.validSize(currentSize)) {
            //throw new RuntimeException(Arrays.toString(t) + " invalid size for " + op);
            return null;
        }

        return make(op, relation, TermContainer.the(op, tt)).term();

    }



    default Term inst(Term subj, Term pred) {
        return newTerm(Op.INHERIT, newTerm(Op.SET_EXT, subj), pred);
    }
    default Term prop(Term subj, Term pred) {
        return newTerm(Op.INHERIT, subj, newTerm(Op.SET_INT, pred));
    }
    default Term instprop(Term subj, Term pred) {
        return newTerm(Op.INHERIT, newTerm(Op.SET_EXT, subj), newTerm(Op.SET_INT, pred));
    }

    default Term negation(Term t) {
        if (t.op(Op.NEGATE)) {
            // (--,(--,P)) = P
            return ((TermContainer) t).term(0);
        }
        return make(Op.NEGATE, -1, new TermVector(t)).term();
    }

    default Term image(Op o, Term[] res) {

        int index = 0, j = 0;
        for (Term x : res) {
            if (x.equals(Op.Imdex)) {
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

    default Term junction(Op op, int t, Iterable<Term> u) {


        final boolean[] done = {true};

        //TODO use a more efficient flattening that doesnt involve recursion and multiple array creations
        TreeSet<Term> s = new TreeSet();
        u.forEach(x -> {
            if (x.op(op)) {
                for (Term y : ((TermContainer) x).terms()) {
                    if (s.add(y))
                        if (y.op(op))
                            done[0] = false;
                }
            } else {
                s.add(x);
            }
        });

        if (!done[0]) {
            return junction(op, t, s);
        }

        Term x = finish(op, -1, TermSet.the(s));
        if (x instanceof Compound) {
            x = ((Compound)x).t(t);
        }
        return x;
    }

    default Term statement(Op op, int t, Term[] u) {

        switch (u.length) {
            case 1:
                return u[0];

            case 2:

                Term subject = u[0];
                Term predicate = u[1];

                //DEBUG:
                if (subject == null || predicate == null)
                    throw new RuntimeException("invalid statement terms");

                //special statement filters
                switch (op) {
                    case EQUIV:
                        if (!validEquivalenceTerm(subject)) return null;
                        if (!validEquivalenceTerm(predicate)) return null;
                        break;

                    case IMPLICATION:
                        if (subject.isAny(TermIndex.InvalidEquivalenceTerm)) return null;
                        if (predicate.isAny(TermIndex.InvalidImplicationPredicate)) return null;

                        if (predicate.isAny(Op.ImplicationsBits)) {
                            Term oldCondition = subj(predicate);
                            if ((oldCondition.isAny(Op.ConjunctivesBits)) && oldCondition.containsTerm(subject))
                                return null;

                            return impl2Conj(op, subject, predicate, oldCondition);
                        }
                        break;
                    default:
                        break;
                }

                if (u[0].equals(u[1]))
                    return u[0];

                //already tested equality, so go to invalidStatement2:
                if (!Statement.invalidStatement2(u[0], u[1])) {
                    TermContainer cc = TermContainer.the(op, u);
                    Compound x = ((Compound) make(op, -1, cc).term());
                    if(t!=Tense.ITERNAL) {
                        boolean reversed = cc.term(0)==u[1];
                        x = x.t(reversed ? -t : t);
                    }
                    return x;
                }

                return null;
            default:
                return null;
                //throw new RuntimeException("invalid statement arguments: " + Arrays.toString(t));
        }
    }

    default Term subtractSet(Op setType, Compound A, Compound B) {
        if (A.equals(B))
            return null; //empty set
        return newTerm(setType, TermContainer.difference(A, B));
    }

    default Term impl2Conj(Op op, Term subject, Term predicate, Term oldCondition) {
        Op conjOp;
        switch (op) {
            case IMPLICATION:
                conjOp = CONJUNCTION;
                break;
            default:
                throw new RuntimeException("invalid case");
        }

        Term s = newTerm(conjOp, subject, oldCondition);
        if (s == null)
            return null;

        return newTerm(op, s, pred(predicate));
    }

    default Term newIntersectINT(Term[] t) {
        return newIntersection(t,
                Op.INTERSECT_INT,
                Op.SET_INT,
                Op.SET_EXT);
    }

    default Term newIntersectEXT(Term[] t) {
        return newIntersection(t,
                Op.INTERSECT_EXT,
                Op.SET_EXT,
                Op.SET_INT);
    }

    default Term newIntersection(Term[] t, Op intersection, Op setUnion, Op setIntersection) {
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

    @Deprecated default Term newIntersection2(Term term1, Term term2, Op intersection, Op setUnion, Op setIntersection) {

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
            return intersect(setIntersection, (Compound) term1, (Compound) term2);
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
            Term[] suffix;

            suffix = o2 == intersection ? ((TermContainer) term2).terms() : new Term[]{term2};

            return finish(intersection, -1,
                    TermSet.the(Terms.concat(
                        ((TermContainer) term1).terms(), suffix
                    ))
            );
        }

        return finish(intersection, -1, TermSet.the(term1, term2));


    }

    default Term intersect(Op resultOp, Compound a, Compound b) {
        MutableSet<Term> i = TermContainer.intersect(a, b);
        if (i.isEmpty()) return null;
        return newTerm(resultOp, i);
    }


    /** returns the resolved term according to the substitution    */
    default Term transform(Compound src, Subst f, boolean fullMatch) {

//        Term y = f.getXY(src);
//        if (y!=null)
//            return y;

        int len = src.size();

        List<Term> sub = Global.newArrayList(len /* estimate */);

        for (int i = 0; i < len; i++) {
            Term t = src.term(i);
            if (!apply(t, f, sub)) {
                if (fullMatch)
                    return null;
            }
        }

        Term result = newTerm(src, TermContainer.the(src.op(), sub));

        //apply any known immediate transform operators
        //TODO decide if this is evaluated incorrectly somehow in reverse
        if (Op.isOperation(result)) {
            ImmediateTermTransform tf = f.getTransform(Operator.operatorTerm((Compound)result));
            if (tf!=null) {
                return applyImmediateTransform(f, result, tf);
            }
        }

        return result;
    }



    default Term applyImmediateTransform(Subst f, Term result, ImmediateTermTransform tf) {

        //Compound args = (Compound) Operator.opArgs((Compound) result).apply(f);
        Compound args = Operator.opArgs((Compound) result);

        return ((tf instanceof PremiseAware) && (f instanceof PremiseMatch)) ?
                ((PremiseAware) tf).function(args, (PremiseMatch) f) :
                tf.function(args, this);
    }


    default Term apply(Subst f, Term src) {
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
    default boolean apply(Term src, Subst f, Collection<Term> sub) {
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

    default Compound theCompound(Term x) {
        Term y = theTerm(x);
        if (y instanceof Compound) return ((Compound)y);
        return null;
    }

}
