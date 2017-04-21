package nars.term.subst;

import jcog.version.VersionMap;
import jcog.version.Versioned;
import jcog.version.Versioning;
import nars.$;
import nars.Op;
import nars.Param;
import nars.derive.meta.constraint.MatchConstraint;
import nars.derive.meta.constraint.NotEqualConstraint;
import nars.index.term.TermIndex;
import nars.term.Term;
import nars.term.mutate.Termutator;
import nars.term.var.CommonVariable;
import nars.term.var.Variable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import static nars.Param.MaxMatchConstraintsPerVariable;


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
public abstract class Unify implements Termutator, Subst {

    final static Logger logger = LoggerFactory.getLogger(Unify.class);

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

    @NotNull
    public final VersionMap<Term, Term> xy;

    @NotNull
    public final VersionMap<Term, Term> yx;


    public final boolean mutate(List<Termutator> chain, int next) {
        return chain.get(++next).mutate(this, chain, next);
    }


    protected Unify(TermIndex index, @Nullable Op type, Random random, int stackMax, int ttl) {
        this(index, type, random, new Versioning(stackMax, ttl));
    }

    protected Unify(TermIndex index, @Nullable Op type, Random random, @NotNull Versioning versioning) {
        super();

        this.index = index;

        this.random = random;
        this.type = type;

        this.versioning = versioning;

        xy = new VersionMap(versioning, Param.MaxUnificationVariables, Param.MaxUnificationVariableStack) {
            @NotNull
            @Override
            public Versioned newEntry(Object keyIgnoredk) {
                return new ConstrainedVersionedTerm();
            }
        };

        yx = new VersionMap(versioning, Param.MaxUnificationVariables, Param.MaxUnificationVariableStack);
        //reassignerYX = new VersionMap.Reassigner<>(constraintPredicate, yx);
    }


    /**
     * called each time all variables are satisfied in a unique way
     *
     * @return whether to continue on any subsequent matches
     */
    public abstract boolean onMatch();


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

    public final void set(@NotNull Term t) {
        xy.putConstant(t, t);
    }

    /**
     * unifies the next component, which can either be at the start (true, false), middle (false, false), or end (false, true)
     * of a matching context
     * <p>
     * setting finish=false allows matching in pieces before finishing
     */
    public boolean unify(@NotNull Term x, @NotNull Term y, boolean start, boolean finish) {

        boolean result;
        try {
            if (unify(x, y)) {

                if (!finish) {
                    result = true; //return to callee to continue in subsequent operation
                } else {
                    @Nullable List<Termutator> t = termutes;
                    if (t == null) {
                        result = onMatch();
                    } else {

                        //shuffle the ordering of the termutes themselves
                        if (t.size() > 1)
                            Collections.shuffle(t, random);

                        t.add(this); //append call-back at the end

                        result = mutate(this, t, -2);

                    }
                }

            } else {
                result = false;
            }
        } catch (Throwable e) {
            if (Param.DEBUG)
                logger.error("{}", e);
            result = false;
            finish = true;
        }

        if (finish) {
            //revert(s); //else: allows the set constraints to continue
            this.termutes = null;
        }

        return result;

    }


    @Override
    public boolean tryPut(@NotNull Unify m) {
        return m.xy.forEachVersioned(this::replaceXY);
    }

    @Override
    public final boolean mutate(Unify f, List<Termutator> n, int seq) {
        if (!f.versioning.tick())
            return false;

        return (seq == -2) ?
                f.mutate(n, -1) //start combinatorial recurse
                :
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



    class ConstrainedVersionedTerm extends Versioned<Term> {

        /** divide constraints into two classes: fast and slow, fast obviously checked first */
        Versioned<MatchConstraint> constraints = null;
        Versioned<MatchConstraint> fastConstraints = null;

        ConstrainedVersionedTerm() {
            super(versioning,  Param.MaxUnificationVariableStack);
        }

        @Nullable
        @Override
        protected Versioned<Term> set(@Nullable Term current, @NotNull Term next) {

            if (fastConstraints!=null && !valid(next, fastConstraints))
                return null;

            if (constraints!=null && !valid(next, constraints))
                return null;

            return super.set(current, next);
        }

        boolean valid(@NotNull Term next, Versioned<MatchConstraint> c) {
            int s = c.size();
            for (int i = 0; i < s; i++) {
                if (c.get(i).invalid(next, Unify.this ))
                    return false;
            }
            return true;
        }

        public boolean addConstraint(MatchConstraint m) {
            Versioned<MatchConstraint> cc;
            if (m instanceof NotEqualConstraint) {
                cc = fastConstraints!=null ? fastConstraints : (this.fastConstraints = newConstraints());
            } else {
                cc = constraints !=null ? constraints : (this.constraints = newConstraints());
            }
            return cc.set(m)!=null;
        }

        public Versioned newConstraints() {
            return new Versioned(versioning,MaxMatchConstraintsPerVariable);
        }
    }

    public boolean addConstraint(Term x, MatchConstraint m) {


        //final boolean[] valid = {true};
        Versioned<Term> v = xy.getOrCreateIfAbsent(x);
        return ((ConstrainedVersionedTerm)v).addConstraint(m);

//        xy.map.computeIfAbsent(x, (xx) -> return
//            ConstrainedVersionedTerm cv;
////            if (v instanceof ConstrainedVersionedTerm) {
////                cv = (ConstrainedVersionedTerm)v;
////            } else if (v!=null) {
////                Term vv = v.get();
////                assert(vv==null);
//////                if (vv!=null && m.invalid(vv, Unify.this)) {
//////                     //tried to put constraint on a variable with an existing invalid value
//////                    valid[0] = false;
//////                    return v;
//////                }
//////
//////                //it will be valid; promote this to non-constrained versioned entry's values
//////                cv = new ConstrainedVersionedTerm(v);
////                cv = new ConstrainedVersionedTerm();
//
//            if (v == null) {
//                v = new ConstrainedVersionedTerm();
//            } else {
//                System.out.println("what is it: " + v);
//            }
//
//            return v;
//        });
//
//        return valid[0];
    }


    /**
     * returns true if the assignment was allowed, false otherwise
     */
    public final boolean putXY(@NotNull Term xVar /* usually a Variable */, @NotNull Term y /* value */) {
        return xy.tryPut(xVar, y);
    }

    public final boolean replaceXY(Term x /* usually a Variable */, @NotNull Term y) {
        return xy.tryPut(x, y);
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


}


