package nars.term.subst;

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

import java.util.Collections;
import java.util.List;
import java.util.Random;

import static nars.Op.VAR_PATTERN;
import static nars.Param.UnificationConstraintsMax;


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

    public final static Logger logger = LoggerFactory.getLogger(Unify.class);

    @NotNull
    public Random random;

    @NotNull
    public final Op type;

    @NotNull
    protected final List<Termutator> termutes = $.newArrayList();

    @NotNull
    public final Versioning versioning;

    @NotNull
    public TermIndex terms;

    @NotNull
    public final VersionMap<Term, Term> xy;



    protected Unify(TermIndex terms, @Nullable Op type, Random random, int stackMax, int ttl) {
        this(terms, type, random, new Versioning(stackMax, ttl));
    }

    /**
     *
     * @param terms
     * @param type if null, unifies any variable type.  if non-null, only unifies that type
     * @param random
     * @param versioning
     */
    protected Unify(TermIndex terms, @Nullable Op type, Random random, @NotNull Versioning versioning) {
        super();

        this.terms = terms;

        this.random = random;
        this.type = type;

        this.versioning = versioning;

        xy = new ConstrainedVersionMap(versioning, Param.UnificationVariableStackMax);

    }


    /**
     * called each time all variables are satisfied in a unique way
     *
     * @return whether to continue on any subsequent matches
     */
    public abstract boolean onMatch();

    public final boolean mutate(List<Termutator> chain, int next) {
        return chain.get(++next).mutate(this, chain, next);
    }

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
    public Term xy(@NotNull Term t) {
        return xy.get(t);
    }

    public final Unify unifyAll(@NotNull Term x, @NotNull Term y) {
        unify(x, y, false, true);
        return this;
    }

    protected final void set(@NotNull Term t) {
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
                    List<Termutator> t = termutes;
                    if (t.isEmpty()) {
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
            this.termutes.clear();
        }

        return result;

    }


    @Override
    public boolean put(@NotNull Unify m) {
        return m.xy.forEachVersioned(this::putXY);
    }

    @Override
    public final boolean mutate(Unify f, List<Termutator> n, int seq) {
        return (seq == -2) ?
                f.mutate(n, -1) //start combinatorial recurse
                :
                f.onMatch(); //end combinatorial recurse
    }


    public final boolean unify(@NotNull Term x, @NotNull Term y) {

        return x.equals(y)
                ||
                x.unify(y, this)
                ;
    }

    public final boolean matchType(@NotNull Term y) {
        return matchType(y.op());
    }

    public final boolean matchType(@NotNull Op oy) {
        Op t = this.type;
        return t == null ?
                oy.var : //any variable
                oy == t; //the specified type
    }






    @Nullable
    public Term resolve(@NotNull Term x) {
        return transform(x, terms);
    }


    public final boolean addTermutator(@NotNull Termutator x) {
        return this.termutes.add(x);
    }


    public boolean putCommon(@NotNull Variable/* var */ x, @NotNull Variable y) {
        @NotNull Term common = CommonVariable.common((Variable) x, (Variable) y);
        return (putXY(x, common) && putXY(y, common)
                //&& putYX(y, common) //&& putYX(x,common)
        );
    }

    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException("slow");
    }




    /**
     * returns true if the assignment was allowed, false otherwise
     */
    public final boolean putXY(@NotNull Term x /* usually a Variable */, @NotNull Term y /* value */) {
        Term x2 = xy(x);
        if (x2 != null) {
            return unify(x2, y);
        } else {
            return xy.tryPut(x, y);
        }
    }

    public final int now() {
        return versioning.size();
    }

    public final boolean revert(int then) {
        return versioning.revert(then);
    }


    public final boolean live() {
        return versioning.live();
    }


    private class ConstrainedVersionMap extends VersionMap {
        public ConstrainedVersionMap(@NotNull Versioning versioning, int maxVars) {
            super(versioning, maxVars);
        }

        @NotNull
        @Override
        public Versioned newEntry(Object keyIgnored) {
            return new ConstrainedVersionedTerm();
        }
    }

    final class ConstrainedVersionedTerm extends Versioned<Term> {


        final Versioned<MatchConstraint> constraints = new Versioned(versioning, UnificationConstraintsMax);

//        /**
//         * divide constraints into two classes: fast and slow,
//         * fast ieally are checked first
//         */
//        Versioned<MatchConstraint> fastConstraints = null;

        ConstrainedVersionedTerm() {
            super(versioning, Param.UnificationVariableStackMax);
        }

        @Nullable
        @Override
        public Versioned<Term> set(@NotNull Term next) {
            return valid(next) ? super.set(next) : null;
        }

        private boolean valid(@NotNull Term x) {
            int s = constraints.size();
            for (int i = 0; i < s; i++) {
                MatchConstraint cc = constraints.get(i);
                if (cc == null) /* why null? */
                    throw new NullPointerException();
                    //break;
                if (cc.invalid(x, Unify.this))
                    return false;
            }
            return true;
        }

//
//        boolean isFast(MatchConstraint m) {
//            return !(m instanceof CommonalityConstraint);
//        }

//        Versioned newConstraints() {
//            return new Versioned(versioning, MaxMatchConstraintsPerVariable);
//        }
    }


    public boolean addConstraint(MatchConstraint... cc) {
        for (MatchConstraint m : cc) {
            Versioned<Term> v = xy.getOrCreateIfAbsent(m.target);
            if (((ConstrainedVersionedTerm) v).constraints.set(m) == null)
                return false;
        }
        return true;
    }

}


