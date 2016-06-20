package nars.term.subst;

import com.gs.collections.api.set.MutableSet;
import nars.Global;
import nars.Op;
import nars.index.TermIndex;
import nars.nal.meta.constraint.MatchConstraint;
import nars.nal.meta.match.Ellipsis;
import nars.nal.meta.match.EllipsisMatch;
import nars.nal.meta.match.EllipsisTransform;
import nars.nal.meta.match.ImageMatch;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termlike;
import nars.term.container.TermContainer;
import nars.term.subst.choice.*;
import nars.term.variable.CommonVariable;
import nars.term.variable.Variable;
import nars.util.data.list.FasterList;
import nars.util.data.list.LimitedFasterList;
import nars.util.version.HeapVersioning;
import nars.util.version.VersionMap;
import nars.util.version.Versioned;
import nars.util.version.Versioning;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Supplier;


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
public abstract class FindSubst implements Subst, Supplier<Versioned<Term>> {


    public final Random random;
    public Op type; //TODO make final again

    @NotNull
    public final Versioning versioning;

    @Nullable
    public final TermIndex index;


    /**
     * variables whose contents are disallowed to equal each other
     */
    @NotNull public final Versioned<MatchConstraint> constraints;
    @NotNull public final VersionMap.Reassigner<Term, Term> reassignerXY, reassignerYX;

    /*
    @NotNull public final Matcher matcherXY, matcherYX;
    */

    @NotNull
    public final VersionMap<Term, Term> xy;
    @NotNull
    public final VersionMap<Term, Term> yx;


//    @NotNull
//    public final Versioned<Term> term;

    /**
     * parent, if in subterms
     */
    @NotNull
    public final Versioned<Compound> parent;


    public final List<Termutator> termutes = new LimitedFasterList(Global.UnificationTermutesMax);


    @NotNull
    @Override
    public String toString() {
        return "subst:{" +
                "now:" + versioning.now() +
                ", type:" + type +
                //", term:" + term +
                ", parent:" + parent +
                //"random:" + random +
                ", xy:" + xy +
                ", yx:" + yx +
                '}';
    }


    protected FindSubst(TermIndex index, Op type, Random random) {
        this(index, type, random,
                new HeapVersioning(Global.UnificationStackMax, 4)
                //new PooledVersioning(Global.UnificationStackMax, 4)
        );
    }

    protected FindSubst(TermIndex index, Op type, Random random, @NotNull Versioning versioning) {
        //super(Global.UnificationStackMax, 8);

        this.index = index;

        this.random = random;
        this.type = type;

        this.versioning = versioning;
        xy = new VersionMap(versioning, 12);
        yx = new VersionMap(versioning, 4);
        reassignerXY = new VersionMap.Reassigner<>(this::assignable, xy);
        reassignerYX = new VersionMap.Reassigner<>(this::assignable, yx);
        parent = new Versioned(versioning);

        int constraintsLimit = 4;
        constraints = new Versioned(versioning, new int[constraintsLimit], new FasterList(0, new MatchConstraint[constraintsLimit]));

        //matcherXY = new Matcher(this::assignable, xy);
        //matcherYX = new Matcher(this::assignable, yx);

    }

    @Override
    public Versioned<Term> get() {
        return new Versioned(versioning);
    }

    /**
     * called each time all variables are satisfied in a unique way
     */
    public abstract boolean onMatch();
//    /**
//     * called each time a match is not fully successful
//     */
//    public void onPartial() {
//
//    }

    @Override
    public final void clear() {
        versioning.clear();
    }


    @Nullable
    @Override
    public final Term term(@NotNull Term t) {
        return xy.get(t);
    }

    public final void matchAll(@NotNull Term x, @NotNull Term y) {
        matchAll(x, y, true);
    }


    private final Termunator termunator = new Termunator(this);

    /**
     * setting finish=false allows matching in pieces before finishing
     */
    public void matchAll(@NotNull Term x, @NotNull Term y, boolean finish) {

        if (match(x, y) && finish) {
            if (!termutes.isEmpty())
                termunator.run(this, null, -1);
            else
                onMatch();
        }

    }


//    private void print(String prefix, @Nullable Term a, Term b) {
//        System.out.print(prefix);
//        if (a != null)
//            System.out.println(" " + a + " ||| " + b);
//        else
//            System.out.println();
//        System.out.println("     " + this);
//    }


    /**
     * recurses into the next sublevel of the term
     */
    public final boolean match(@NotNull Term x, @NotNull Term y) {

        if (x.equals(y)) {
            return true;
        } else {

            final Op xOp = x.op();

            switch (xOp) {
                case VAR_DEP:
                case VAR_INDEP:
                case VAR_QUERY:
                case VAR_PATTERN:
                    //Var
                    if (xOp == y.op())
                        return putCommon(x, y);
                    else if (xOp == type)
                        return matchVarX(x, y);
                    else
                        break;
                case OPER:
                case ATOM:
                    //Atomic
                    return false;
                default:
                    //Compound
                    if (y instanceof Compound)
                        return ((Compound) x).match((Compound) y, this);
                    else
                        break;
            }

            return (y.op() == type) && matchVarY(x, y);
        }

//            else if (x instanceof Compound) {
//                return ((Compound) x).match((Compound) y, this);
//            } else if (x instanceof Variable) {
//                return putCommon( x, y );
//            }
//
//        } else {
//
//            Op t = type;
//
//            if (xOp == t) {
//                //if both are variables of the target type, but different; they need to be common variable
//                return matchVarX(x, y);
//            } else if (yOp == t) {
//                return matchVarY(x, y);
//            }
//
//
//        }
//
//        return false;
    }

//    private static boolean hasAnyVar(Compound x) {
//        return x.complexity()<x.volume() || x.firstEllipsis()!=null;
//    }
//
//    private final boolean matchVarCommon(@NotNull Term /* var */ xVar, @NotNull Term /* var */ y) {
//        return (equalOp) ?
//                putCommon((Variable)xVar, (Variable)y) :
//                putXY(xVar, y);
//
//        //            if ((y.op() == Op.VAR_QUERY && xVar.op() != Op.VAR_QUERY) ||
//        //                    (y.op() != Op.VAR_QUERY && xVar.op() == Op.VAR_QUERY)) {
//        //                return false;
//        //            }
//
//    }


    /**
     * x's and y's ops already determined inequal
     */
    private final boolean matchVarX(@NotNull Term /* var */ x, @NotNull Term y) {
        Term x2 = term(x);
        return (x2 != null) ?
                match(x2, y) :
                putXY(/* (Variable) */ x, y);

        //return matcherXY.computeMatch(x, y);
    }
    /**
     * x's and y's ops already determined inequal
     */
    private final boolean matchVarY(@NotNull Term x, @NotNull Term /* var */ y) {

        Term y2 = yx.get(y);
        return (y2 != null) ?
                match(x, y2) :
                putYX(/*(Variable)*/ x, y);  // && putXY(y, /*(Variable)*/ x));

        //return matcherYX.computeMatch(y, x);

    }

//    public class Matcher extends VersionMap.Reassigner<Term,Term> {
//
//        public Matcher(BiPredicate<Term, Term> assigner, VersionMap map) {
//            super(assigner, map);
//        }
//
//        @Override
//        public Versioned<Term> apply(Term x, Versioned<Term> vy) {
//            Term t = vy!=null ? vy.get() : null;
//            if (t!=null) {
//                return match(t, y) ? vy : null;
//            } else {
//                return super.apply(x, vy);
//            }
//        }
//
//        public final boolean computeMatch(@NotNull Term x, @NotNull Term y) {
//            this.y = y;
//            return map.computeAssignable(x, this);
//        }
//    }






//    private static void printComparison(int power, Compound cx, Compound cy) {
//        System.out.println(cx.structureString() + " " + cx.volume() + "\t" + cx);
//        System.out.println(cy.structureString() + " " + cy.volume() + "\t" + cy);
//        System.out.println(!cx.impossibleToMatch(cy) + "|" + !cy.impossibleToMatch(cx) + " ---> " + (power >= 0) + " " + power);
//        System.out.println();
//    }


    @Override
    public final boolean isEmpty() {
//        if (xy.isEmpty() && !yx.isEmpty()) {
//            System.err.println("YX: " + yx);
//        }
        return xy.isEmpty();
    }

//    public final boolean matchCompoundWithEllipsis(@NotNull Compound X, @NotNull Compound Y) {
//
//
////        final int numNonpatternVars;
////        int ellipsisToMatch = Ellipsis.numUnmatchedEllipsis(X, this);
////        if (ellipsisToMatch == 0) {
////
////            int ellipsisTotal = Ellipsis.numEllipsis(X);
////            if (ellipsisTotal > 0) {
////                //compute a virtual set of subterms based on an existing Ellipsis match
////                Term XX = X.substituted(this);
////                return (match(XX, Y));
////            }
////
////            /** NORMAL: match subterms but do not collect for ellipsis */
////            if (xsize != Y.size()) {
////                return false;
////            }
////            numNonpatternVars = xsize;
////        } else {
////            numNonpatternVars = Ellipsis.countNumNonEllipsis(X);
////        }
//
//        //TODO see if there is a volume or structural constraint that can terminate early here
//
//
//        Ellipsis e = Ellipsis.firstEllipsis(X);
//
//
////        if (!e.valid(numNonpatternVars, ysize)) {
////            return false;
////        }
//
//
//        if (X.isCommutative()) {
//            return matchEllipsedCommutative(
//                    X, e, Y
//            );
//        } else {
//            return matchCompoundWithEllipsisLinear(X, Y, e);
//        }
//
//    }

    @Nullable
    public final Term resolve(@NotNull Term t) {
        //TODO make a half resolve that only does xy?
        return resolve(t, this);
    }

    @Nullable
    public final Term resolveNormalized(@NotNull Term t) {
        //TODO make a half resolve that only does xy?
        t = resolve(t);
        if (t instanceof Compound)
            t = this.index.normalized((Compound) t).term();
        return t;
    }

    @Nullable
    public final Term resolve(@NotNull Term t, @NotNull Subst subst) {
        return index.resolve(t, subst);
    }


    //    private boolean matchEllipsisImage(Compound x, Ellipsis e, Compound y) {
//        /*  ex:
//           (M --> (A..B=_..+))
//        */
//        putXY(e, new ShadowProduct(x.terms()));
//        return false;
//    }

    public boolean addTermutator(@NotNull Termutator x) {

        //resolve termutator interferences that the addition may cause
        Termlike xKey = x.key;
        Class xKeyClass = xKey.getClass();

        List<Termutator> t = this.termutes;
        int s = t.size();

        for (int i = 0; i < s; i++) {
            Termutator y = t.get(i);
            Termlike yKey = y.key;
            if (xKey.equals(yKey)) {
                //TODO maybe bifurcate a termutator tree with an OR branch?

                //if exact same conditions, dont add duplicate
                if (xKeyClass == yKey.getClass() &&
                        y.toStringCached().equals(x.toStringCached()))
                    return true;
                else
                    continue;
            }
            if (xKey.containsTerm((Termlike) yKey)) {
                //insert b before a since it is more specific
                t.add(i, x);
                return true;
            } /*else if (b.containsTerm((Term) a)) {
                //a contained by b; append to end (after a)
                continue;
            } */

        }

        return t.add(x);
    }

    public final boolean matchPermute(@NotNull TermContainer x, @NotNull TermContainer y) {
        //if there are no variables of the matching type, then it seems CommutivePermutations wouldnt match anyway
        return x.hasAny(type) && addTermutator(new CommutivePermutations(this, x, y));
    }


//    private boolean matchChoose2(Term[] x, MutableSet<Term> y) {
//        int prePermute = now();
//        MutableSet<Term> yCopy = y.clone(); //because matchChoose1 will remove on match
//
//        //initial shuffle
//        if (random.nextBoolean()) {
//            Term p = x[0];
//            x[0] = x[1];
//            x[1] = p;
//        }
//
//        int startDivisor = powerDivisor;
//        if (!powerDividable(2))
//            return false;
//
//        boolean matched = false;
//        for (int i = 0; i < 2; i++) {
//
//            boolean modified = false;
//            if (matchChoose1(x[0], y)) {
//                modified = true;
//                if (matchChoose1(x[1], y)) {
//                    matched = true;
//                    break;
//                }
//            }
//
//            if (modified) {
//                y.addAll(yCopy); //restore the original set if any where removed during an incomplete match
//            }
//
//            revert(prePermute);
//
//            /* swap */
//            Term p = x[0];
//            x[0] = x[1];
//            x[1] = p;
//        }
//
//        powerDivisor = startDivisor;
//        return matched;
//    }


////    /**
////     * elimination
////     */
//    private boolean putVarX(@NotNull Term /* var */ x, @NotNull Term y) {
//        //if (!(x instanceof GenericNormalizedVariable))
//          //  throw new RuntimeException();
//
//        return putXY(x, y);// && (x.op() != type || putYX(x, y));
//    }


    private boolean putCommon(@NotNull Term /* var */ x, @NotNull Term y) {
        Variable commonVar = CommonVariable.make((Variable) x, (Variable) y);
        int s = now();
        if (putXY(x, commonVar)) {
            if (putYX(y, commonVar)) {
                return true;
            } else {
                //restore changed values if putYX fails but putXY succeeded
                revert(s);
            }
        }
        return false;
    }

    /**
     * a branch for comparing a particular permutation, called from the main next()
     */
    public final boolean matchLinear(@NotNull TermContainer X, @NotNull TermContainer Y) {
        int s = X.size();
        switch (s) {
            case 0:
                return true;
            case 1:
                return matchSub(X, Y, 0);
            case 2:
                //match the target variable first, if exists:
                return matchLinear2(X, Y, X.isTerm(0, type) ? 0 : 1);
                //return matchLinear2(X, Y, 0);
            default:
                return matchLinearN(X, Y);
        }
    }


    public final boolean matchLinearN(@NotNull TermContainer X, @NotNull TermContainer Y) {
        final int s = X.size();
        for (int i = 0; i < s; i++) {
            if (!matchSub(X, Y, i)) return false;
        }
        return true;
    }

    public final boolean matchSub(@NotNull TermContainer X, @NotNull TermContainer Y, int i) {
        return match(X.term(i), Y.term(i));
    }

    /**
     * special match for size=2 compounds, with order reversal ability
     */
    public final boolean matchLinear2(@NotNull TermContainer X, @NotNull TermContainer Y, int first) {
        int other = 1 - first;
        return match(X.term(first), Y.term(first)) && match(X.term(other), Y.term(other));
    }


    /**
     * returns true if the assignment was allowed, false otherwise
     */
    public final boolean putYX(@NotNull Term x /* usually a Variable */, @NotNull Term y) {
        return reassignerYX.compute(y, x);
    }

    /**
     * returns true if the assignment was allowed, false otherwise
     */
    public final boolean putXY(@NotNull Term x /* usually a Variable */, @NotNull Term y) {
        return reassignerXY.compute(x, y);
    }


    public final boolean replaceXY(Term x /* usually a Variable */, @NotNull Versioned<Term> y) {
        return replaceXY(x, y.get());
    }

    public final boolean replaceXY(Term x /* usually a Variable */, @NotNull Term y) {
        //assert (y != null);
        xy.put(x, y);
        return true;
    }

    /**
     * true if the match assignment is allowed by constraints
     * TODO find a way to efficiently eliminate redundant rules shared between versions
     */
    public final boolean assignable(@NotNull Term x, @NotNull Term y) {
        Versioned<MatchConstraint> cc = this.constraints;
        int s = cc.size();
        if (s > 0) {
            MatchConstraint[] ccc = cc.value.array();
            for (; s > 0; ) {
                if (ccc[--s].invalid(x, y, this))
                    return false;
            }
        }
        return true;
    }


    public final int now() {
        return versioning.now();
    }

    public final void revert(int then) {
        versioning.revert(then);
    }

//    public void forEachVersioned(@NotNull BiConsumer<? super Term, ? super Versioned<Term>> each) {
//        xy.forEachVersioned(each);
//        //TODO yx also?
//    }

    public void forEachVersioned(@NotNull BiConsumer<? super Term, ? super Versioned<Term>> each) {
        xy.forEachVersioned(each);
    }


//    public boolean forEachVersioned(@NotNull BiPredicate<? super Term, ? super Versioned<Term>> each) {
//        return xy.forEachVersioned(each);
//        //TODO yx also?
//    }


//    /**
//     * VersionMap<Term,Term> which caches Variable keys, but allows any Term as keys (uncached)
//     */
//    public static final class VarCachedVersionMap extends VersionMap<Term, Term> implements Subst {
//
//        public VarCachedVersionMap(Versioning context) {
//            super(context);
//        }
//
////        public VarCachedVersionMap(Versioning context, Map<Term, Versioned<Term>> map) {
////            super(context, map);
////        }
//
//        @Override
//        public void forEach(@NotNull BiConsumer<? super Term, ? super Term> each) {
//            Map<Term, Versioned<Term>> m = this.map;
//            if (!m.isEmpty()) {
//                m.forEach((k, vv) -> {
//                    Term v = vv.get();
//                    if (v != null)
//                        each.accept(k, v);
//                });
//            }
//        }
//
//        public void forEachVersioned(@NotNull BiConsumer<? super Term, ? super Versioned<Term>> each) {
//            Map<Term, Versioned<Term>> m = this.map;
//            if (!m.isEmpty())
//                m.forEach(each);
//        }
//
//        /** returns true only if each evaluates true; if empty, returns true also */
//        public boolean forEachVersioned(@NotNull BiPredicate<? super Term, ? super Versioned<Term>> each) {
//            Map<Term, Versioned<Term>> m = this.map;
//            if (!m.isEmpty()) {
//                final boolean[] b = {true};
//
//                m.forEach((k,v)->{
//
//                    if (!each.test(k, v)) {
//                        b[0] = false;
//                    }
//
//                });
//
//                return b[0];
//            }
//            return true;
//        }
//
//
//        @Override
//        public final Term term(Term t) {
//
//            assert(!(t instanceof GenericVariable)); //throw new RuntimeException("variables must be normalized");
//
//            Versioned<Term> v = map.get(t);
//            return v == null ? null : v.get();
//        }
//
//        /**
//         * must inspect elements because the entries will be there but possibly null
//         */
//        @Override
//        public boolean isEmpty() {
//            if (!super.isEmpty()) {
//                for (Versioned x : map.values()) {
//                    if (x.get() != null) return false;
//                }
//            }
//            return true;
//        }
//    }

//    public boolean matchLinearReverse(@NotNull TermContainer X, @NotNull TermContainer Y) {
//        for (int i = X.size() - 1; i >= 0; i--) {
//            if (!matchSub(X, Y, i)) return false;
//        }
//        return true;
//    }


//    public void termute(int i, Termutator[] chain) {
//
//        int max = chain.length;
//        if (i == max) {
//            onMatch();
//            return;
//        }
//
//        Termutator t = chain[i];
//        t.reset(this, i, chain);
//
//    }

//    public final boolean putYX(@NotNull Term y /* usually a Variable */, Term x) {
//        //yx.put(x, y);
//
//        VarCachedVersionMap yx = this.yx;
//
//        Versioned<Term> v = yx.map.get(x);
//
//        /*if (!assignable(x, y))
//            return false;*/
//
//        if (v == null) {
//            v = yx.getOrCreateIfAbsent(x);
//        } else {
//            Term vv = (v != null) ? v.get() : null;
//            if (vv != null) {
//                return y.equals(vv);
//            }
//        }
//        v.set(y);
//        return true;
//    }

}


