package nars.term.subst;

import jcog.version.VersionMap;
import jcog.version.Versioned;
import jcog.version.Versioning;
import nars.$;
import nars.Op;
import nars.derive.meta.constraint.MatchConstraint;
import nars.index.term.TermIndex;
import nars.term.Term;
import nars.term.Termlike;
import nars.term.container.TermContainer;
import nars.term.mutate.CommutivePermutations;
import nars.term.mutate.Termunator;
import nars.term.mutate.Termutator;
import nars.term.var.CommonVariable;
import nars.term.var.Variable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;
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
public abstract class Unify extends Termunator implements Subst {

    @NotNull public final Random random;

    @Nullable public final Op type;

    @NotNull
    private final List<Termutator> termutes;

    @NotNull
    public final Versioning versioning;

    @NotNull
    public final TermIndex index;

    /**
     * variables whose contents are disallowed to equal each other
     */
    @NotNull public final Constraints constraints;
    @NotNull public final VersionMap.Reassigner<Term, Term> reassignerXY;//, reassignerYX;

    @NotNull
    public final VersionMap<Term, Term> xy;
    @NotNull
    public final VersionMap<Term, Term> yx;

    protected Unify(TermIndex index, @Nullable Op type, Random random, int stackMax) {
        this(index, type, random, new Versioning(stackMax));
    }

    /** call this to invoke the next termutator in the chain */
    public final boolean chain(Termutator[] chain, int next) {

        //increment the version counter by one and detect if the limit exceeded.
        // this is to prevent infinite recursions in which no version incrementing
        // occurrs that would otherwise trigger overflow to interrupt it.
        return versioning.nextChange() && chain[++next].run(this, chain, next);

    }

    protected final class Constraints extends Versioned<MatchConstraint> implements BiPredicate<Term,Term> {

        public Constraints(@NotNull Versioning context, int maxConstr) {
            super(context, new MatchConstraint[maxConstr]);
        }

        @Override
        public boolean test(@NotNull Term x, @NotNull Term y) {
            int s = size;
            if (s > 0) {
                MatchConstraint[] ccc = items;
                for (; s > 0; ) {
                    if (ccc[--s].invalid(x, y, Unify.this))
                        return false;
                }
            }
            return true;
        }
    }

    protected Unify(TermIndex index, @Nullable Op type, Random random, @NotNull Versioning versioning) {
        super();

        this.termutes = $.newArrayList();

        this.index = index;

        this.random = random;
        this.type = type;

        this.versioning = versioning;

        int constraintsLimit = 6;
        constraints = new Constraints(versioning, constraintsLimit);

        xy = new VersionMap(versioning, 16);
        reassignerXY = new VersionMap.Reassigner(constraints, xy);

        yx = new VersionMap(versioning, 8);
        //reassignerYX = new VersionMap.Reassigner<>(constraintPredicate, yx);
    }


    /**
     * called each time all variables are satisfied in a unique way
     */
    public abstract boolean onMatch();


    @Override
    public final void clear() {
        versioning.clear();
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



    /**
     * unifies the next component, which can either be at the start (true, false), middle (false, false), or end (false, true)
     * of a matching context
     *
     * setting finish=false allows matching in pieces before finishing
     */
    public boolean unify(@NotNull Term x, @NotNull Term y, boolean start, boolean finish) {

        if (start) {
            termutes.clear();
        }

        if (unify(x, y)) {

            if (finish) {
                return run(this, null, -1);
            }

            return true;
        }

        return false;
    }

    @Override
    public final boolean run(@NotNull Unify f, Termutator[] ignored, int ignoredAlwaysNegativeOne) {
        Termutator[] n = next();
        if (n != null) {
            return chain(n, -1); //combinatorial recurse starts here
        } else {
            return f.onMatch(); //ends here when termutes exhausted
        }
    }

    @Nullable
    private final Termutator[] next() {
        List<Termutator> t = termutes;
        int n = t.size();
        if (n == 0) {
            return null;
        } else {
            Termutator[] tt = t.toArray(new Termutator[n + 1]);
            t.clear();

            tt[tt.length - 1] = this; //add this as the final termutator (termunator)

            return tt;
        }
    }


    public final boolean unify(@NotNull Term x, @NotNull Term y) {

        return  x.equals(y)
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

    public final boolean matchPossible(@NotNull Termlike x) {
        Op t = this.type;
        return (t == Op.VAR_PATTERN) ?
                    (x.varPattern() > 0) :
                    x.hasAny(t == null ? Op.VariableBits : t.bit);
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
        int s = t.size();

        for (int i = 0; i < s; i++) {
            Termutator y = t.get(i);
            if (x.equals(y)) {
                return true; //TODO maybe bifurcate a termutator tree with an OR branch?
            }
        }

        return t.add(x);
    }

    public final boolean matchPermute(@NotNull TermContainer x, @NotNull TermContainer y) {
        //if there are no variables of the matching type, then it seems CommutivePermutations wouldnt match anyway
        return matchPossible(x) && addTermutator(new CommutivePermutations(this, x, y));
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
        @NotNull Term common = CommonVariable.make((Variable) x, (Variable) y);
        return putXY(x, common) && putYX(y, common);
    }

    /**
     * a branch for comparing a particular permutation, called from the main next()
     */
    public final boolean matchLinear(@NotNull TermContainer X, @NotNull TermContainer Y) {
        int s = X.size();
        switch (s) {
            case 0:
                return true; //shouldnt ever happen
            case 1:
                return matchSub(X, Y, 0);
            case 2:
                //match the target variable first, if exists:
                return matchLinear2(X, Y, matchType(X.term(0)) ? 0 : 1);
                //return matchLinear2(X, Y, 0); //<- fails for certain image transformation rules
            default:
                return matchLinearN(X, Y, s);
        }
    }

    final boolean matchLinearN(@NotNull TermContainer X, @NotNull TermContainer Y, int s) {
        for (int i = 0; i < s; i++) {
            if (!matchSub(X, Y, i)) return false;
        }
        return true;
    }

    final boolean matchSub(@NotNull TermContainer X, @NotNull TermContainer Y, int i) {
        return unify(X.term(i), Y.term(i));
    }

    /**
     * special match for size=2 compounds, with order reversal ability
     */
    final boolean matchLinear2(@NotNull TermContainer X, @NotNull TermContainer Y, int first) {
        return matchSub(X, Y, first) && matchSub(X, Y, 1 - first);
    }


    /**
     * returns true if the assignment was allowed, false otherwise
     */
    final boolean putYX(@NotNull Term x /* usually a Variable */, @NotNull Term y) {
        return yx.tryPut(y,x);
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

    @NotNull public final Term yxResolve(@NotNull Term y) {
        Term y1 = yx.get(y);
        return (y1 == null) ? y : y1;
    }


}


