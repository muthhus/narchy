package nars.term.subst;

import jcog.list.FasterList;
import jcog.version.VersionMap;
import jcog.version.Versioned;
import jcog.version.Versioning;
import nars.$;
import nars.Op;
import nars.Param;
import nars.derive.meta.constraint.MatchConstraint;
import nars.index.term.TermIndex;
import nars.term.Term;
import nars.term.mutate.Termutator;
import nars.term.var.CommonVariable;
import nars.term.var.Variable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;


/* recurses a pair of compound term tree's subterms
across a hierarchy of sequential and permutative fanouts
where valid matches are discovered, backtracked,
and collected until power is depleted.



https://github.com/rupertlssmith/lojix/blob/master/lojix/prolog/src/main/com/thesett/aima/logic/fol/prolog/PrologUnifier.java
https://github.com/rupertlssmith/lojix/blob/master/lojix/wam_prolog/src/main/com/thesett/aima/logic/fol/wam/compiler/WAMInstruction.java
see this code for a clear explanation of what a prolog unifier does.
this code does some additional things but shares a general structure with the lojix code which i just found now
So it can be useful for a more easy to understand rewrite of this class TODO


*/
public abstract class Unify extends Termutator implements Subst {

    @NotNull
    public final Random random;

    @Nullable
    public final Op type;

    @Nullable
    private List<Termutator> termutes;

    @NotNull
    public final Versioning versioning;

    @NotNull
    public final TermIndex index;

    /**
     * variables whose contents are disallowed to equal each other
     */
    @NotNull
    public final Constraints constraints;
    @NotNull
    public final Reassigner<Term, Term> reassignerXY;//, reassignerYX;

    @NotNull
    public final VersionMap<Term, Term> xy;
    @NotNull
    public final VersionMap<Term, Term> yx;



    public final void mutate(List<Termutator> chain, int next) {
        chain.get(++next).mutate(this, chain, next);
    }

    public final class Constraints extends VersionMap<Term,List<MatchConstraint>> implements BiPredicate<Term, Term> {

        public Constraints(@NotNull Versioning context, int maxConstr) {
            super(context, maxConstr);
        }

        public boolean add(Term x, MatchConstraint m) {

//            //check that constraint isnt violated by existing conditions:
//            Term y = xy.get(x);
//            if (y!=null) {
//                if (m.invalid(x, y, Unify.this))
//                    return false;
//            }

            List<MatchConstraint> ccc = get(x);
            if (ccc == null)
                ccc = $.newArrayList(1);
            else {
                int s = ccc.size();
                FasterList ddd = new FasterList(s + 1); //clone
                ddd.addAll(ccc);
                ccc = ddd;
            }
            ccc.add(m);
            return tryPut(x, ccc);
        }

        @Override
        public boolean test(@NotNull Term x, @NotNull Term y) {
            List<MatchConstraint> ccc = get(x);
            if (ccc==null)
                return true;

            int s = ccc.size();
            for (int i = 0; i < s; i++) {
                if (ccc.get(i).invalid(x, y, Unify.this))
                    return false;
            }
            return true;
        }
    }

    protected Unify(TermIndex index, @Nullable Op type, Random random, int stackMax) {
        this(index, type, random, new Versioning(stackMax));
    }

    protected Unify(TermIndex index, @Nullable Op type, Random random, @NotNull Versioning versioning) {
        super(Unify.class);

        this.index = index;

        this.random = random;
        this.type = type;

        this.versioning = versioning;

        int constraintsLimit = 16;
        constraints = new Constraints(versioning, constraintsLimit);

        xy = new VersionMap(versioning, 32);
        reassignerXY = new Reassigner(constraints, xy);

        yx = new VersionMap(versioning, 16);
        //reassignerYX = new VersionMap.Reassigner<>(constraintPredicate, yx);
    }


    /**
     * called each time all variables are satisfied in a unique way
     */
    public abstract void onMatch();


    @Override
    public final void clear() {
        versioning.clear();
    }

    @Override
    public int getEstimatedPermutations() {
        throw new UnsupportedOperationException();
    }

    @Nullable
    @Override
    public final Term xy(@NotNull Term t) {
        return xy.get(t);
    }

    public final Unify unifyAll(@NotNull Term x, @NotNull Term y) {
        unify(x, y, false, true);
        return this;
    }


    final static Logger logger = LoggerFactory.getLogger(Unify.class);

    /**
     * unifies the next component, which can either be at the start (true, false), middle (false, false), or end (false, true)
     * of a matching context
     * <p>
     * setting finish=false allows matching in pieces before finishing
     */
    public void unify(@NotNull Term x, @NotNull Term y, boolean start, boolean finish) {

        //int s = now();
        boolean result;
        try {
            if (unify(x, y)) {

                if (!finish) {
                    result = true; //return to callee to continue in subsequent operation
                } else {
                    @Nullable List<Termutator> t = termutes;
                    if (t != null) {

                        //shuffle the ordering of the termutes themselves
                        Collections.shuffle(t, random);

                        t.add(this); //call-back
                        mutate(this, t, -2);

                    } else {
                        onMatch();
                    }
                }

            } else {
                result = false;
            }
        } catch (Exception e) {
            if (Param.DEBUG_EXTRA)
                logger.error("{}", e);
            result = false;
            finish = true;
        }

        if (finish) {
            //revert(s); //else: allows the set constraints to continue
            this.termutes = null;
        }


    }

    @Override
    public final void mutate(Unify f, List<Termutator> n, int seq) {
        if (seq==-2)
            f.mutate(n, -1); //start combinatorial recurse
        else
            f.onMatch(); //end combinatorial recurse
    }



    public final boolean unify(@NotNull Term x, @NotNull Term y) {

        return x.equals(y)
                ||
                x.unify(y, this)
                ||
                (/*y instanceof AbstractVariable && */matchType(y) && matchVarY(x, y))
                ;
    }

    public final boolean matchType(@NotNull Term y) {
        return matchType(y.op());
    }

    public final boolean matchType(@NotNull Op oy) {
        Op t = this.type;
        return t == null ? oy.var : oy == t;
    }

    /**
     * x's and y's ops already determined inequal
     */
    public final boolean matchVarX(@NotNull Term /* var */ x, @NotNull Term y) {
        Term x2 = xy(x);
        return (x2 != null) ?
                unify(x2, y) :
                putVarX(/* (Variable) */ x, y);

    }


    /**
     * x's and y's ops already determined inequal
     */
    public final boolean matchVarY(@NotNull Term x, @NotNull Term /* var */ y) {

        Term y2 = yx.get(y);
        if (y2 != null) {
            return unify(x, y2);
        } else {

            //return putYX(x, y);
            if (putYX((Variable) y, x)) {
                if (y instanceof CommonVariable) {
                    if (!putXY((Variable) y, x)) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;

    }

    @Override
    public final boolean isEmpty() {
        return xy.isEmpty();
    }


    @Nullable
    public final Term resolve(@NotNull Term x) {
        return transform(x, this);
    }


    @Nullable
    public final Term transform(@NotNull Term t, @NotNull Subst subst) {
        return index.transform(t, subst);
    }


    public boolean addTermutator(@NotNull Termutator x) {
        List<Termutator> t = this.termutes;
        if (t == null) {
            this.termutes = t = $.newArrayList(4);
        } else if (t.contains(x)) {
            return true;
        }

        return t.add(x);

        /*
        int s = t.size();

        for (int i = 0; i < s; i++) {
            Termutator y = t.get(i);
            if (x.equals(y)) {
                return true; //TODO maybe bifurcate a termutator tree with an OR branch?
            }
        }

        return t.add(x);
        */
    }


    public boolean putVarX(@NotNull Term /* var */ x, @NotNull Term y) {

        if (putXY(x, y)) {
            if (x instanceof CommonVariable) {
                if (!putYX(x, y)) {
                    return false;
                }
            }
            return true;
        }

        return false;
    }


    public boolean putCommon(@NotNull Variable/* var */ x, @NotNull Variable y) {
        @NotNull Term common = CommonVariable.common((Variable) x, (Variable) y);
        if (putXY(x, common)) {
            if (!putYX(y, common)) {
                return false;
            }
            return true;
        } else {
            return false;
        }
    }


    /**
     * returns true if the assignment was allowed, false otherwise
     */
    final boolean putYX(@NotNull Term x /* usually a Variable */, @NotNull Term y) {
        return yx.tryPut(y, x);
    }

    /**
     * returns true if the assignment was allowed, false otherwise
     */
    public final boolean putXY(@NotNull Term x /* usually a Variable */, @NotNull Term y) {
        return reassignerXY.compute(x, y);
    }

    public final boolean replaceXY(Term x /* usually a Variable */, @NotNull Term y) {
        return xy.tryPut(x, y);
    }

    public final void setXY(Term x /* usually a Variable */, @NotNull Term y) {
        xy.putConstant(x, y);
    }

    public final int now() {
        return versioning.size();
    }

    public final void revert(int then) {
        versioning.revert(then);
    }

//    public final void pop(int count) {
//        versioning.pop(count);
//    }

    @NotNull
    public final Term yxResolve(@NotNull Term y) {
        Term y1 = yx.get(y);
        return (y1 == null) ? y : y1;
    }


    private static class Reassigner<X,Y> implements BiFunction<X, Versioned<Y>, Versioned<Y>> {

        protected Y y;
        protected final VersionMap<X,Y> map;
        private final BiPredicate<X,Y> assigner;

        Reassigner(@NotNull BiPredicate<X,Y> assigner, @NotNull final VersionMap<X,Y> map) {
            this.map = map;
            this.assigner = assigner;
        }

        @Override
        public Versioned<Y> apply(X x, @Nullable Versioned<Y> vy) {
            final Y y = this.y;

            if (vy == null) {
                return assigner.test(x, y) ?  map.newEntry(x).set(y) : null;
            } else {
                Y yy = vy.get();
                if (yy == null) {
                    if (!assigner.test(x, y) || (vy.set(y)==null))
                        return null;
                } else if (!Objects.equals(yy, y)) {
                    return null; //conflict
                }
                return vy;
            }
        }

        /** should not be used by multiple threads at once! */
        public final boolean compute(@NotNull X x, @NotNull Y y) {
            this.y = y;
            return map.computeAssignable(x, this);
        }

    }
}


