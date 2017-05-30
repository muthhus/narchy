package nars.term.transform;

import nars.$;
import nars.Op;
import nars.control.premise.Derivation;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.container.TermContainer;
import nars.term.subst.SubUnify;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.Op.False;

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

    protected substituteIfUnifies(Atom id, Derivation parent) {
        super(id);
        this.parent = parent;
        //this.subMatcher = sub;
    }

//    public substituteIfUnifies(PremiseEval parent, OneMatchFindSubst sub) {
//        this("subIfUnifies", parent, sub);
//    }


    @Nullable
    abstract protected Op unifying();

    @NotNull
    @Override
    public Term apply(@NotNull TermContainer a) {

//        if (a.size() < 3) {
//            throw new UnsupportedOperationException();
//        }

        Term input = a.sub(0);

        Term x = a.sub(1);
        Term y = a.sub(2);


        boolean strict = a.subEquals(3, substitute.STRICT);
        if (x.equals(y))
            return strict ? False : input; //unification would occurr but no changes would result

//        if (y.equals(input))
//            return failureTerm;

        @Nullable Op op = unifying();
//        boolean hasAnyOp =
//                (op==null && (x.vars() + x.varPattern()  > 0))
//                ||
//                (op!=null && x.hasAny(op));
//
//        if (!hasAnyOp/* && mustSubstitute()*/) {
//            return False; //no change
//        }

        Term z = new SubUnify(parent, op).tryMatch(input, x, y);
        return (z != null) ? z : False;
    }

    public static class substituteIfUnifiesAny extends substituteIfUnifies {

        final static Atom func = (Atom) $.the("subIfUnifiesAny");

        public substituteIfUnifiesAny(Derivation parent) {
            super(func, parent);
        }

        @Override
        public Op unifying() {
            return null;
        }
    }

    public static class substituteIfUnifiesDep extends substituteIfUnifies {

        final static Atom func = (Atom) $.the("subIfUnifiesDep");

        public substituteIfUnifiesDep(Derivation parent) {
            super(func, parent);
        }


        @NotNull
        @Override
        public Op unifying() {
            return Op.VAR_DEP;
        }
    }
//    public static final class substituteIfUnifiesIndep extends substituteIfUnifies {
//
//
//        public substituteIfUnifiesIndep(Derivation parent) {
//            super("subIfUnifiesIndep", parent);
//        }
//
//
//        @NotNull
//        @Override
//        public Op unifying() {
//            return Op.VAR_INDEP;
//        }
//    }
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


//    /** specifies a forward ordering constraint, for example:
//     *      B, (C && A), time(decomposeBelief) |- substituteIfUnifiesIndepForward(C,A,B), (Desire:Strong)
//     *
//     *  if B unifies with A then A must be eternal, simultaneous, or future with respect to C
//     *
//     *  for now, this assumes the decomposed term is in the belief position
//     */
//    public static final class substituteIfUnifiesForward extends substituteIfUnifies {
//
//        public substituteIfUnifiesForward(Derivation parent) {
//            super("subIfUnifiesForward",parent);
//        }
//
//        @Override
//        protected boolean forwardOnly() {
//            return true;
//        }
//
//        @Override
//        protected @Nullable Op unifying() {
//            return null;
//        }
//    }

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
