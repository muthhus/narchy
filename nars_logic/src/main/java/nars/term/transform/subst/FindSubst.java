package nars.term.transform.subst;

import com.gs.collections.api.map.ImmutableMap;
import com.gs.collections.api.set.MutableSet;
import nars.Global;
import nars.Memory;
import nars.NAR;
import nars.Op;
import nars.nal.meta.constraint.MatchConstraint;
import nars.nal.meta.match.Ellipsis;
import nars.nal.meta.match.EllipsisMatch;
import nars.nal.meta.match.EllipsisTransform;
import nars.nal.meta.match.ImageMatch;
import nars.nal.meta.op.MatchTerm;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termlike;
import nars.term.container.TermContainer;
import nars.term.transform.subst.choice.Choose1;
import nars.term.transform.subst.choice.Choose2;
import nars.term.transform.subst.choice.CommutivePermutations;
import nars.term.transform.subst.choice.Termutator;
import nars.term.variable.CommonVariable;
import nars.term.variable.Variable;
import nars.util.version.VersionMap;
import nars.util.version.Versioned;
import nars.util.version.Versioning;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.BiConsumer;


/* recurses a pair of compound term tree's subterms
across a hierarchy of sequential and permutative fanouts
where valid matches are discovered, backtracked,
and collected until power is depleted. */
public abstract class FindSubst extends Versioning implements Subst {


    /** maximum changes which are stored in stack */
    final static int defaultHistoryLength = 192;

    public final Random random;

    public final Op type;

    /**
     * variables whose contents are disallowed to equal each other
     */
    @Nullable
    public Versioned<ImmutableMap<Term, MatchConstraint>> constraints = null;

    //public abstract Term resolve(Term t, Substitution s);

    @NotNull
    public final VarCachedVersionMap xy;
    @NotNull
    public final VarCachedVersionMap yx;

    /**
     * current "y"-term being matched against
     */
    @NotNull
    public final Versioned<Term> term;

    /**
     * parent, if in subterms
     */
    @NotNull
    public final Versioned<Compound> parent;


    public final List<Termutator> termutes = Global.newArrayList();

//    public static Ellipsis getFirstEllipsis(@NotNull Compound X) {
//        int xsize = X.size();
//        for (int i = 0; i < xsize; i++) {
//            Term xi = X.term(i);
//            if (xi instanceof Ellipsis) {
//                return (Ellipsis) xi;
//            }
//        }
//        return null;
//    }

    /**
     * @param x a compound which contains one or more ellipsis terms
     */
    public static int countNumNonEllipsis(@NotNull Compound x) {
        //TODO depending on the expression, determine the sufficient # of terms Y must contain
        return Ellipsis.numNonEllipsisSubterms(x);
    }


    @NotNull
    @Override
    public String toString() {
        return "subst:{" +
                "now:" + now() +
                ", type:" + type +
                ", term:" + term +
                ", parent:" + parent +
                //"random:" + random +
                ", xy:" + xy +
                ", yx:" + yx +
                '}';
    }


    protected FindSubst(Op type, Random random) {
        this(type, random, null);
    }

    protected FindSubst(Op type, Random random, Versioning toSharePool) {
        super(defaultHistoryLength, toSharePool);
        this.random = random;
        this.type = type;

        xy = new VarCachedVersionMap(this);
        yx = new VarCachedVersionMap(this);
        term = new Versioned(this);
        parent = new Versioned(this);
        constraints = new Versioned(this);
        //branchPower = new Versioned(this);


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
    public void clear() {
        revert(0);
    }


    @Nullable
    @Override
    public final Term getXY(@NotNull Object t) {
        return xy.getXY(t);
    }

    @Nullable
    public Term getYX(@NotNull Term t) {
        return yx.getXY(t);
    }


//    public final void goSubterm(int index) {
//        term.set(parent.get().term(index));
//    }

    public final void matchAll(@NotNull Term x, @NotNull Term y) {
        matchAll(x, y, true);
    }


    /** appended to the end of termuator execution chains to invoke
     *  any accumulated termutations occurring during the match
     *  or onMatch() if it was stable
     * **/
    private final Termutator termunator = new Termutator(".") {

        /** should be be synchronized if threadsafe necessary*/
        @Override
        public void run(FindSubst f, Termutator[] ignored, int ignoredAlwaysNegativeOne) {

            if (termutes.isEmpty()) {
                onMatch();
            } else {
                Termutator.next(FindSubst.this, next(), -1);
            }

        }

        @NotNull
        private Termutator[] next() {
            List<Termutator> t = FindSubst.this.termutes;
            int n = t.size();

            t.add(termunator);
            Termutator[] tt = FindSubst.this.termutes.toArray(new Termutator[n]);
            t.clear();
            return tt;
        }

        @Override
        public int getEstimatedPermutations() {
            return 0;
        }
    };

    /**
     * setting finish=false allows matching in pieces before finishing
     */
    public void matchAll(@NotNull Term x, @NotNull Term y, boolean finish) {

        if (match(x, y) && finish) {

            termunator.run(this, null, -1);

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
        }

        Op t = type;


        Op xOp = x.op();
        Op yOp = y.op();

        if ((xOp == yOp) && (x instanceof Compound)) {
            return ((Compound) x).match((Compound) y, this);
        }

        if (xOp == t) {
            return matchXvar((Variable) x, y);
        }

        if (yOp == t) {
            return matchYvar(x, /*(Variable)*/y);
        }

        if (xOp.isVar() && yOp.isVar()) {
            return nextVarX((Variable) x, y);
        }

        return false;
    }

    private boolean matchYvar(@NotNull Term x, @NotNull Term y) {
        Term ySubst = getYX(y);

        if (ySubst != null) {
            return match(x, ySubst); //loop
        } else {
            if (putYX(/*(Variable)*/ y, x)) {
                if (y instanceof CommonVariable) {
                    return putXY(x, /*(Variable)*/ y);
                }
                return true;
            }
        }
        return false;
    }

    public boolean matchXvar(@NotNull Variable x, @NotNull Term y) {
        Term xSubst = getXY(x);

        //try {
            return (xSubst != null) ?
                    match(xSubst, y) :
                    nextVarX(x, y);
        /*}
        catch (StackOverflowError e) {
            e.printStackTrace();
            return false;
        }*/

    }

//    private static void printComparison(int power, Compound cx, Compound cy) {
//        System.out.println(cx.structureString() + " " + cx.volume() + "\t" + cx);
//        System.out.println(cy.structureString() + " " + cy.volume() + "\t" + cy);
//        System.out.println(!cx.impossibleToMatch(cy) + "|" + !cy.impossibleToMatch(cx) + " ---> " + (power >= 0) + " " + power);
//        System.out.println();
//    }


    private boolean nextVarX(@NotNull Variable xVar, @NotNull Term y) {
        Op xOp = xVar.op();

        if (xOp == type) {
            return putVarX(xVar, y);
        } else {
            if (y.op() == xOp) {
                return putCommon(xVar, (Variable) y);
            }
        }

        return false;
    }

    @Override
    public final boolean isEmpty() {
//        if (xy.isEmpty() && !yx.isEmpty()) {
//            System.err.println("YX: " + yx);
//        }
        return xy.isEmpty();
    }

    public final boolean matchCompoundWithEllipsis(@NotNull Compound X, @NotNull Compound Y) {



//        final int numNonpatternVars;
//        int ellipsisToMatch = Ellipsis.numUnmatchedEllipsis(X, this);
//        if (ellipsisToMatch == 0) {
//
//            int ellipsisTotal = Ellipsis.numEllipsis(X);
//            if (ellipsisTotal > 0) {
//                //compute a virtual set of subterms based on an existing Ellipsis match
//                Term XX = X.substituted(this);
//                return (match(XX, Y));
//            }
//
//            /** NORMAL: match subterms but do not collect for ellipsis */
//            if (xsize != Y.size()) {
//                return false;
//            }
//            numNonpatternVars = xsize;
//        } else {
//            numNonpatternVars = Ellipsis.countNumNonEllipsis(X);
//        }

        //TODO see if there is a volume or structural constraint that can terminate early here


        Ellipsis e = Ellipsis.firstEllipsis(X);


//        if (!e.valid(numNonpatternVars, ysize)) {
//            return false;
//        }


        if (X.isCommutative()) {
            return matchCompoundWithEllipsisCommutative(X, Y, e);
        } else {
            return matchCompoundWithEllipsisLinear(X, Y, e);
        }

    }

    public boolean matchCompoundWithEllipsisLinear(@NotNull Compound X, @NotNull Compound Y, Ellipsis e) {
        int xsize = X.size();
        int ysize = Y.size();

        if (e instanceof EllipsisTransform) {
            //this involves a special "image ellipsis transform"

            EllipsisTransform et = (EllipsisTransform) e;
            if (et.from.equals(Op.Imdex)) {

                Term n = resolve(et.to);
                if (n == null)
                    return false;

                //the indicated term should be inserted
                //at the index location of the image
                //being processed. (this is the opposite
                //of the other condition of this if { })
                if (matchEllipsedLinear(X, e, Y)) {
                    EllipsisMatch raw = (EllipsisMatch) getXY(e);
                    xy.put(e, null); //HACK clear it to replace with a new value
                    return putXY(e, ImageMatch.put(raw.term, n, Y));
                }
            } else {
                Term n = resolve(et.from);
                if (n == null)
                    return false;

                //resolving may be possible to defer to substitution if
                //Y and et.from are components of ImageShrinkEllipsisMatch

                int imageIndex = Y.indexOf(n);
                return ((imageIndex != -1) && matchEllipsedLinear(X, e, Y)) &&
                        putXY(e, ImageMatch.take((EllipsisMatch) getXY(e), imageIndex));

            }
            return false;
        }

        /** if they are images, they must have same relationIndex */
        if (X.op().isImage()) { //PRECOMPUTABLE

            //if the ellipsis is normal, then interpret the relationIndex as it is
            if (countNumNonEllipsis(X) > 0) {

                int xEllipseIndex = X.indexOf(e);
                int xRelationIndex = X.relation();
                int yRelationIndex = Y.relation();

                if (xEllipseIndex >= xRelationIndex) {
                    //compare relation from beginning as in non-ellipsis case
                    if (xRelationIndex != yRelationIndex)
                        return false;
                } else {
                    //compare relation from end
                    if ((xsize - xRelationIndex) != (ysize - yRelationIndex))
                        return false;
                }
            } else {
                //ignore the location of imdex in the pattern and match everything

            }

        }

        return matchEllipsedLinear(X, e, Y);
    }

    public boolean matchCompoundWithEllipsisCommutative(@NotNull Compound X, @NotNull Compound Y, Ellipsis e) {
        return matchEllipsedCommutative(
                X, e, Y
        );
    }

//    private boolean matchEllipsisImage(Compound x, Ellipsis e, Compound y) {
//        /*  ex:
//           (M --> (A..B=_..+))
//        */
//        putXY(e, new ShadowProduct(x.terms()));
//        return false;
//    }

    protected boolean addTermutator(@NotNull Termutator x) {

        //resolve termutator interferences that the addition may cause
        Termlike a = x.key;
        List<Termutator> t = this.termutes;
        int s = t.size();
        for (int i = 0; i < s; i++) {
            Termutator y = t.get(i);
            Termlike b = y.key;
            if (a.equals(b)) {
                //TODO maybe bifurcate a termutator tree with an OR branch?

                //if exact same conditions, dont add duplicate
                if (a.getClass() == b.getClass() &&
                        y.toString().equals(t.toString()))
                    return true;
                else
                    continue;
            }
            if (a.containsTerm((Term) b)) {
                //insert b before a since it is more specific
                t.add(i, x);
                return true;
            } /*else if (b.containsTerm((Term) a)) {
                //a contained by b; append to end (after a)
                continue;
            } */

        }

        t.add(x);
        return true;
    }

    public final boolean matchPermute(@NotNull TermContainer x, @NotNull Compound y) {
        boolean isPattern = type == Op.VAR_PATTERN;

        if
            ((!isPattern && !x.hasAny(type.bit()) ||
            (isPattern && !x.hasVarPattern())))

        {
            //SPECIAL CASE: no variables
            return matchLinear(x, y);
        }

        return addTermutator(new CommutivePermutations(this, x, y));
    }


    /**
     * commutive compound match: Y into X which contains one ellipsis
     * <p>
     * X pattern contains:
     * <p>
     * one unmatched ellipsis (identified)
     * <p>                    //HACK should not need new list
     * <p>
     * zero or more "constant" (non-pattern var) terms
     * all of which Y must contain
     * <p>
     * zero or more (non-ellipsis) pattern variables,
     * each of which may be matched or not.
     * matched variables whose resolved values that Y must contain
     * unmatched variables determine the amount of permutations/combinations:
     * <p>
     * if the number of matches available to the ellipse is incompatible with the ellipse requirements, fail
     * <p>
     * (total eligible terms) Choose (total - #normal variables)
     * these are then matched in revertable frames.
     * <p>
     * *        proceed to collect the remaining zero or more terms as the ellipse's match using a predicate filter
     *
     * @param X the pattern term
     * @param Y the compound being matched into X
     */
    public final boolean matchEllipsedCommutative(@NotNull Compound X, Ellipsis Xellipsis, @NotNull Compound Y) {

        //ALL OF THIS CAN BE PRECOMPUTED
        Set<Term> xSpecific = Global.newHashSet(0); //Global.newHashSet(0);

        //constant terms which have been verified existing in Y and will not need matched
        Set<Term> ineligible = Global.newHashSet(0);

        boolean ellipsisMatched = false;
        for (Term x : X.terms()) {

            boolean xVar = x.op() == type;
            Term v = getXY(x); //xVar ? getXY(x) : x;
            if (v == null) v = x;

            //ellipsis to be matched in stage 2
            //if (x == Xellipsis) {
            //    continue;
            //}

            if (v instanceof EllipsisMatch) {
                //assume it's THE ellipsis here, ie. x == xEllipsis
                ellipsisMatched = true;
                Xellipsis = null;

                //check that Y contains all of these
                if (!((EllipsisMatch) v).addWhileMatching(Y, ineligible))
                    return false;

                continue;
            } else if (!xVar) {
                if (!Y.containsTerm(v))
                    return false;
                ineligible.add(v);
                continue;
            }

            if (x != Xellipsis)
                xSpecific.add(x);


        }

        MutableSet<Term> yFree = Y.toSet();

        if (ellipsisMatched) {
            //Xellipsis = null;
            return ineligible.equals(yFree);
        }

        yFree.removeAll(ineligible);

        if (!Xellipsis.valid(yFree.size() - xSpecific.size())) {
            //wouldnt be enough remaining matches to satisfy ellipsis cardinality
            return false;
        }

        return matchCommutiveRemaining(Xellipsis, xSpecific, yFree);

    }


    /**
     * toMatch matched into some or all of Y's terms
     */
    private boolean matchCommutiveRemaining(Term xEllipsis, @NotNull Set<Term> x, @NotNull MutableSet<Term> yFree) {
        int xs = x.size();

        switch (xs) {
            case 0:
                return putXY(xEllipsis, new EllipsisMatch(yFree));
            case 1:
                return addTermutator(new Choose1(
                        xEllipsis, x.iterator().next(), yFree));
            case 2:
                return addTermutator(new Choose2(this,
                        xEllipsis, x.toArray(new Term[x.size()]), yFree));
            default:
                //3 or more combination
                throw new RuntimeException("unimpl: " + xs + " arity combination unimplemented");
        }

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


    /**
     * non-commutive compound match
     * X will contain at least one ellipsis
     * <p>
     * match subterms in sequence
     * <p>
     * WARNING this implementation only works if there is one ellipse in the subterms
     * this is not tested for either
     */
    public final boolean matchEllipsedLinear(@NotNull Compound X, @NotNull Ellipsis Xellipsis, @NotNull Compound Y) {

        int i = 0, j = 0;
        int xsize = X.size();
        int ysize = Y.size();

        //TODO check for shim and subtract xsize?

        while (i < xsize) {
            Term x = X.term(i++);

            if (x instanceof Ellipsis) {
                int available = ysize - j;

                Term eMatched = getXY(x); //EllipsisMatch, or null
                if (eMatched == null) {

                    //COLLECT
                    if (i == xsize) {
                        //SUFFIX
                        if (!Xellipsis.valid(available))
                            return false;

                        //TODO special handling to extract intermvals from Sequence terms here

                        if (!putXY(Xellipsis,
                                new EllipsisMatch(
                                        Y, j, j + available
                                ))) {
                            return false;
                        }
                    } else {
                        //PREFIX the ellipsis occurred at the start and there are additional terms following it
                        //TODO
                        return false;
                    }
                } else {
                    //previous match exists, match against what it had
                    if (i == xsize) {
//                        //SUFFIX - match the remaining terms against what the ellipsis previously collected
//                        //HACK this only works with EllipsisMatch type
//                        Term[] sp = ((EllipsisMatch) eMatched).term;
//                        if (sp.length!=available)
//                            return false; //incorrect size
//
//                        //match every item
//                        for (Term aSp : sp) {
//                            if (!match(aSp, Y.term(j++)))
//                                return false;
//                        }
                    } else {
                        //TODO other cases
                        return false;
                    }

                }
            } else {
                if (!match(x, Y.term(j++)))
                    return false;
            }
        }

        return true;
    }


    /**
     * elimination
     */
    private boolean putVarX(Variable x, @NotNull Term y) {
        if (putXY(x, y)) {
            if (x instanceof CommonVariable) {
                return putYX(y, x);
            }
            return true;
        }
        return false;
    }


    private boolean putCommon(@NotNull Variable x, @NotNull Variable y) {
        Variable commonVar = CommonVariable.make(x, y);
        return putXY(x, commonVar) && putYX(y, commonVar);
    }

    /**
     * a branch for comparing a particular permutation, called from the main next()
     */
    public boolean matchLinear(@NotNull TermContainer X, @NotNull TermContainer Y) {
        int s = X.size();
        switch (s) {
            case 0:
                return true;
            case 1:
                return matchSub(X, Y, 0);
            case 2:
                //match the target variable first, if exists
                int first = X.term(1, type) ? 1 : 0;
                return matchSub(X,Y,first) && matchSub(X,Y,1-first);
            default:
                return matchLinearForward(X, Y);
        }
    }


    public boolean matchLinearForward(@NotNull TermContainer X, @NotNull TermContainer Y) {
        final int s = X.size();
        for (int i = 0; i < s; i++) {
            if (!matchSub(X, Y, i)) return false;
        }
        return true;
    }

    public boolean matchSub(@NotNull TermContainer X, @NotNull TermContainer Y, int i) {
        return match(X.term(i), Y.term(i));
    }

    public boolean matchLinearReverse(@NotNull TermContainer X, @NotNull TermContainer Y) {
        for (int i = X.size() - 1; i >= 0; i--) {
            if (!matchSub(X, Y, i)) return false;
        }
        return true;
    }


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

    public final boolean putYX(@NotNull Term y /* usually a Variable */, Term x) {
        //yx.put(x, y);

        VarCachedVersionMap yx = this.yx;

        Versioned<Term> v = yx.map.get(x);

        /*if (!assignable(x, y))
            return false;*/

        if (v == null) {
            v = yx.getOrCreateIfAbsent(x);
        } else {
            Term vv = (v != null) ? v.get() : null;
            if (vv != null) {
                return y.equals(vv);
            }
        }
        v.set(y);
        return true;
    }


    /**
     * returns true if the assignment was allowed, false otherwise
     */
    public final boolean putXY(Term x /* usually a Variable */, @NotNull Term y) {

//        if (x.equals(y))
//            throw new RuntimeException("x==y");

        VarCachedVersionMap xy = this.xy;

        Versioned<Term> v = xy.map.get(x);
        Term vv = (v != null) ? v.get() : null;
        if (vv != null) {
            return y.equals(vv);
        }
        if (!assignable(x, y))
            return false;

        if (v == null) {
            v = xy.getOrCreateIfAbsent(x);
        }

        v.set(y);
        return true;
    }

    /**
     * true if the match assignment is allowed by constraints
     * TODO find a way to efficiently eliminate redundant rules shared between versions
     */
    public final boolean assignable(Term x, Term y) {
        Versioned<ImmutableMap<Term, MatchConstraint>> cc = this.constraints;
        int s = cc.size() - 1;
        if (s < 0) return true;
        Object[] ccc = cc.value.array();
        for ( ; s >=0; s--) {
            MatchConstraint c = ((ImmutableMap<Term, MatchConstraint>)ccc[s]).get(x);
            if ((c != null) && c.invalid(x, y, this))
                return false;
        }
        return true;
    }

    @Override public void forEach(BiConsumer<? super Term, ? super Term> each) {
        xy.forEach(each);
        //TODO yx also?
    }

    @Nullable
    public Term resolve(Term t) {
        throw new RuntimeException("unimpl");
    }


    /**
     * default compound matching; op will already have been compared. no ellipsis will be involved
     */
    public final boolean matchCompound(@NotNull Compound x, @NotNull Compound y) {
        int ys = y.size();
        if ((x.size() == ys) && (x.relation() == y.relation())) {
            return (y.isCommutative()) ?
                    matchPermute(x, y) :
                    matchLinear(x.subterms(), y.subterms());
        }
        return false;
    }

    /**
     * VersionMap<Term,Term> which caches Variable keys, but allows any Term as keys (uncached)
     */
    public static final class VarCachedVersionMap extends VersionMap<Term, Term> implements Subst {

        public VarCachedVersionMap(Versioning context) {
            super(context);
        }

        public VarCachedVersionMap(Versioning context, Map<Term, Versioned<Term>> map) {
            super(context, map);
        }

        @Override
        public void forEach(BiConsumer<? super Term, ? super Term> each) {
            Map<Term, Versioned<Term>> m = this.map;
            if (!m.isEmpty()) {
                m.forEach((k, vv) -> {
                    Term v = vv.get();
                    if (v != null)
                        each.accept(k, v);
                });
            }
        }

        @Override
        public boolean cache(Term key) {
            //since these should always be normalized variables, they will not exceed a predictable range of entries (ex: $1, $2, .. $n)
            return key instanceof Variable;
        }

        @Override
        public Term getXY(Object t) {
            Versioned<Term> v = map.get(t);
            if (v == null) return null;
            return v.get();
        }

        /** must inspect elements because the entries will be there but possibly null */
        @Override public boolean isEmpty() {
            if (super.isEmpty())
                return true;

            for (Versioned x : map.values()) {
                if (x.get()!=null) return false;
            }
            return true;
        }
    }
}


