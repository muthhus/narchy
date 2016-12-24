package nars.term.transform;

import nars.Op;
import nars.nal.meta.Derivation;
import nars.term.Compound;
import nars.term.Term;
import nars.term.subst.SubUnify;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.term.Terms.equal;
import static nars.time.Tense.DTERNAL;

/**
 * substituteIfUnifies....(term, varFrom, varTo)
 *
 * <patham9_> for A ==> B, D it is valid to unify both dependent and independent ones ( unify(A,D) )
 <patham9_> same for B ==> A, D     ( unify(A,D) )
 <patham9_> same for <=> and all temporal variations
 <patham9_> is this the general solution you are searching for?
 <patham9_> for (&&,...) there are only dep-vars anyway so there its easy
 <sseehh> i found a solution for one which would be to make it do both dep and indep var
 <sseehh> like you said
 <sseehh> which im working now, making it accept either
 <patham9_> ah I see
 <sseehh> i dont know if the others are solved by this or not
 <patham9_> we also allow both in 2.0.x here
 <sseehh> in all cases?
 <sseehh> no this isnt the general solution i imagined would be necessary. it may need just special cases always, i dunno
 <sseehh> my dep/indep introducer is general because it isnt built from any specific rule but operates on any term
 <patham9_> the cases I mentioned above, are there cases that are not captured here?
 <sseehh> i dont know i have to look at them all
 <sseehh> i jus tknow that currently each one is either dep or indep
 <sseehh> and im making the first one which is both
 <sseehh> and if this works then ill see if the others benefit from it
 <patham9_> yes it should allow both here anyway
 <sseehh> i hope its the case that they all can be either
 <patham9_> unify("$") also allows unify("#") but not vice versa
 <patham9_> thats what we also had in 1.7.0
 <sseehh> so you're syaing anywhree i have substituteIfUnifiesDep i can not make both, but anywhere that is substituteIfUnifiesIndep i can?
 <sseehh> or that they both can
 <patham9_> yes thats what I'm saying
 <sseehh> k
 <patham9_> substituteIfUnifiesIndep  is always used on conditional rules like the ones above, this is why unifying dep here is also fine here
 <patham9_> for substituteIfUnifiesDep there has to be a dependent variable that was unified, else the rule application leads to a redundant and weaker result
 <patham9_> imagine this case: (&&,<tim --> cat>,<#1 --> animal>).   <tim --> cat>.   would lead to <#1 --> animal>  Truth:AnonymousAnalogy altough no anonymous analogy was attempted here
 <patham9_> which itself is weaker than:  <#1 --> animal>  as it would have come from deduction rule alone here already
 <sseehh> i think this is why i tried something like subtituteOnlyIfUnifiesDep but it probably needed this condition instead
 <sseehh> but i had since removed that
 <patham9_> I see
 <patham9_> yes dep-var unification needs a dep-var that was unified. while the cases where ind-var unification is used, it doesnt matter if there is a variable at all
 <sseehh> ok that clarifies it ill add your notes here as comments
 <sseehh> coding this now, carefly
 <sseehh> carefuly
 <patham9_> also i can't think of a case where dep-var unification would need the ability to also unify ind-vars, if you find such a case i don't see an issue with allowing it, as long as it requires one dep-var to be unified it should work
 <patham9_> hm not it would be wrong to allow ind-var-unification for dep-var unification, reason: (&&,<$1 --> #1> ==> <$1 --> blub>,<cat --> #1>) could derive <cat --> #1> from a more specific case such as <tim --> #1> ==> <tim --> blub>>
 <patham9_> *no
 <patham9_> so its really this:
 <patham9_> allow dep-var unify on ind-var unify, but not vice versa.
 <patham9_> and require at least one dep-var to be unified in dep-var unification.
 <patham9_> in principle the restriction to have at least one dep-var unified could be skipped, but the additional weaker result doesn't add any value to the system
 */
abstract public class substituteIfUnifies extends Functor {

    //private final OneMatchFindSubst subMatcher;
    protected final Derivation parent; //parent matcher context

    protected substituteIfUnifies(String id, Derivation parent) {
        super(id);
        this.parent = parent;
        //this.subMatcher = sub;
    }

//    public substituteIfUnifies(PremiseEval parent, OneMatchFindSubst sub) {
//        this("subIfUnifies", parent, sub);
//    }


    /**
     * whether an actual substitution is required to happen; when true and no substitution occurrs, then fails
     */
    protected boolean mustSubstitute() {
        return false;
    }

    @Nullable
    abstract protected Op unifying();

    @Nullable
    @Override
    //public Term function(@NotNull Compound p, @NotNull PremiseEval r) {
    public Term apply(@NotNull Term[] a) {

//        if (xx.length < 3) {
//            throw new UnsupportedOperationException();
//        }

        Term term = a[0];
        Term x = a[1];
        Term y = a[2];

        return unify(term, x, y);
    }

    public @Nullable Term unify(@NotNull Term term, @NotNull Term x, @NotNull Term y) {

        if (forwardOnly()) {
            if (!(term instanceof Compound))
                return null;
            int dt = ((Compound)term).dt();
            if (!(dt == DTERNAL || dt == 0 || term.subtermTime( x ) == 0))
                return null;
        }

        @Nullable Op op = unifying();
        boolean hasAnyOp = op == null || (x.hasAny(op) && term.hasAny(op));

        if (!hasAnyOp && mustSubstitute()) {
            return null; //FAILED
        }

        boolean equals = equal(x, y, false /* different dt */, true /* same polarity */);
//        if (!equals) {
//            //try auto-negation:
//            boolean xn = (x.op()==NEG);
//            boolean yn = (y.op()==NEG);
//            Term px = (xn) ? x.unneg() : x; //positive X
//            Term py = (yn) ? y.unneg() : y; //positive Y
//            if (equalAtemporally(px, py)) {
//                equals = true;
//                if (xn ^ yn) {
//                    if (yn/* && !xn*/) { //x isnt negated and y is, so
//                        y = py;
//                    } else { //if (xn && !yn) { //x is negated and y isn't, so
//                        y = $.neg(y);
//                    }
//
//                    term = $.neg(term);
//
//                    //now x and y have matching polarities
//                } else if (xn/* && yn*/) {
//                    //both negated
//                } else {
//                    //shouldnt hapen?
//                }
//            }
//        }

        if (!equals && hasAnyOp) {
            SubUnify m = new SubUnify(parent, op);

            Term newTerm = m.tryMatch(parent, term, x, y);
            return newTerm != null ? newTerm : null;
        } else {
            return equals ? term : null;
        }
    }

    protected boolean forwardOnly() {
        return false;
    }


    public static class substituteIfUnifiesAny extends substituteIfUnifies {


        public substituteIfUnifiesAny(Derivation parent) {
            super("subIfUnifiesAny", parent);
        }

        @Override
        public Op unifying() {
            return null;
        }
    }

    public static final class substituteIfUnifiesDep extends substituteIfUnifies {


        public substituteIfUnifiesDep(Derivation parent) {
            super("subIfUnifiesDep", parent);
        }

        @Override
        protected boolean mustSubstitute() {
            return true;
        }


        @NotNull
        @Override
        public Op unifying() {
            return Op.VAR_DEP;
        }
    }

//    public static final class substituteOnlyIfUnifiesDep extends substituteIfUnifies {
//
//        public substituteOnlyIfUnifiesDep(PremiseEval parent) {
//            super("subOnlyIfUnifiesDep", parent);
//        }
//
//        @Override
//        protected boolean mustSubstitute() {
//            return true;
//        }
//
//        @NotNull
//        @Override
//        public Op unifying() {
//            return Op.VAR_DEP;
//        }
//    }

//    public static final class substituteIfUnifiesIndep extends substituteIfUnifies {
//
//        public substituteIfUnifiesIndep(PremiseEval parent) {
//            super("subIfUnifiesIndep",parent);
//        }
//
//
//        @NotNull
//        @Override
//        public Op unifying() {
//            return Op.VAR_INDEP;
//        }
//    }


    /** specifies a forward ordering constraint, for example:
     *      B, (C && A), time(decomposeBelief) |- substituteIfUnifiesIndepForward(C,A,B), (Desire:Strong)
     *
     *  if B unifies with A then A must be eternal, simultaneous, or future with respect to C
     *
     *  for now, this assumes the decomposed term is in the belief position
     */
    public static final class substituteIfUnifiesForward extends substituteIfUnifies {

        public substituteIfUnifiesForward(Derivation parent) {
            super("subIfUnifiesForward",parent);
        }

        @Override
        protected boolean forwardOnly() {
            return true;
        }

        @Override
        protected @Nullable Op unifying() {
            return null;
        }
    }

//    public static final class substituteOnlyIfUnifiesIndep extends substituteIfUnifies {
//
//        public substituteOnlyIfUnifiesIndep(PremiseEval parent) {
//
//            super("subOnlyIfUnifiesIndep", parent);
//        }
//
//        @Override
//        protected boolean mustSubstitute() {
//            return true;
//        }
//
//        @NotNull
//        @Override
//        public Op unifying() {
//            return Op.VAR_INDEP;
//        }
//    }
}
