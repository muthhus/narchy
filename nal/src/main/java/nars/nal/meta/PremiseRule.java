package nars.nal.meta;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Sets;
import nars.Global;
import nars.Op;
import nars.concept.Temporalize;
import nars.nal.meta.constraint.MatchConstraint;
import nars.nal.meta.constraint.NoCommonSubtermsConstraint;
import nars.nal.meta.constraint.NotEqualsConstraint;
import nars.nal.meta.constraint.NotOpConstraint;
import nars.nal.meta.match.Ellipsis;
import nars.nal.meta.match.EllipsisOneOrMore;
import nars.nal.meta.match.EllipsisTransform;
import nars.nal.meta.match.EllipsisZeroOrMore;
import nars.nal.meta.op.*;
import nars.nal.op.Derive;
import nars.nal.op.Solve;
import nars.nal.op.substitute;
import nars.nal.op.substituteIfUnifies;
import nars.op.data.differ;
import nars.op.data.intersect;
import nars.op.data.union;
import nars.term.Compound;
import nars.term.Term;
import nars.term.TermIndex;
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.term.compound.GenericCompound;
import nars.term.container.TermVector;
import nars.term.index.PatternIndex;
import nars.term.transform.CompoundTransform;
import nars.term.transform.VariableNormalization;
import nars.term.transform.subst.MapSubst;
import nars.term.variable.AbstractVariable;
import nars.term.variable.GenericVariable;
import nars.term.variable.Variable;
import nars.truth.BeliefFunction;
import nars.truth.DesireFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import static nars.$.*;
import static nars.Op.VAR_PATTERN;
import static nars.term.Terms.*;

/**
 * A rule which matches a Premise and produces a Task
 * contains: preconditions, predicates, postconditions, post-evaluations and metainfo
 */
public class PremiseRule extends GenericCompound {


    public static final Class[] Operators = new Class[]{
            intersect.class,
            differ.class,
            union.class,
            substitute.class,
            substituteIfUnifies.class,
//        occurrsForward.class,
//        occurrsBackward.class
    };

    /**
     * blank marker trie node indicating the derivation and terminating the branch
     */
    public static final BooleanCondition END = new AtomicBooleanCondition<PremiseEval>() {

        @Override
        public boolean booleanValueOf(PremiseEval versioneds) {
            return true;
        }

        @Override
        public String toString() {
            return "End";
        }
    };

    public boolean immediate_eternalize;

    public boolean anticipate;


    /**
     * conditions which can be tested before term matching
     */
    public BooleanCondition[] prePreconditions;

    /**
     * conditions which are tested after term matching, including term matching itself
     */
    public BooleanCondition[] postPreconditions;

    public PostCondition[] postconditions;

    public PatternCompound pattern;

    //it has certain pre-conditions, all given as predicates after the two input premises


    boolean allowBackward;

    /**
     * maximum of the minimum NAL levels involved in the postconditions of this rule
     */
    public int minNAL;

    protected String source;

    public @Nullable MatchTaskBelief match;

    private Temporalize temporalize = Temporalize.Auto;

    @NotNull
    public final Compound getPremise() {
        return (Compound) term(0);
    }

    @NotNull
    public final Compound getConclusion() {
        return (Compound) term(1);
    }

    PremiseRule(@NotNull Compound premisesResultProduct) {
        this((Compound) premisesResultProduct.term(0), (Compound) premisesResultProduct.term(1));
    }

    public PremiseRule(@NotNull Compound premises, @NotNull Compound result) {
        super(Op.PRODUCT, TermVector.the(premises, result));
    }


//    public final boolean validTaskPunctuation(final char p) {
//        if ((p == Symbols.QUESTION) && !allowQuestionTask)
//            return false;
//        return true;
//    }

    protected final void ensureValid() {

//        if (getConclusionTermPattern().containsTemporal()) {
//            if ((!getTaskTermPattern().containsTemporal())
//                    &&
//                    (!getBeliefTermPattern().containsTemporal())) {
//                //if conclusion is temporal term but the premise has none:
//
//                String s = toString();
//                if ((!s.contains("after")) && (!s.contains("concurrent") && (!s.contains("measure")))) {
//                    //System.err.println
//                  throw new RuntimeException
//                            ("Possibly invalid temporal rule from atemporal premise: " + this);
//
//                }
//            }
//        }
//

        if (postconditions.length == 0)
            throw new RuntimeException(this + " has no postconditions");
//        if (!getTask().hasVarPattern())
//            throw new RuntimeException("rule's task term pattern has no pattern variable");
//        if (!getBelief().hasVarPattern())
//            throw new RuntimeException("rule's task belief pattern has no pattern variable");
//        if (!getConclusionTermPattern().hasVarPattern())
//            throw new RuntimeException("rule's conclusion belief pattern has no pattern variable");
    }


    /**
     * add the sequence of involved conditions to a list, for one given postcondition (ex: called for each this.postconditions)
     */
    @NotNull
    public List<Term> getConditions(@NotNull PostCondition post) {

        List<Term> l = Global.newArrayList(prePreconditions.length + postPreconditions.length + 4 /* estimate */);

        List<Term> beforeMatch = Global.newArrayList();
        for (BooleanCondition p : prePreconditions)
            p.addConditions(l);



        Solve truth = solver(post,
                this, anticipate, immediate_eternalize, postPreconditions, temporalize
        );


        beforeMatch.add(truth);
        match.addPreConditions(beforeMatch); //pre-conditions

        //TODO sort beforeMatch because the order can determine performance HACK
        if (beforeMatch.size() > 1) {
            Term second = beforeMatch.get(1);

            //pull these Task comparisons to the front
            if ((second instanceof TaskPunctuation) ||
                (second instanceof TaskNegative)) {
                beforeMatch.remove(1);
                beforeMatch.add(0, second);
            }
        }

//        Collections.sort(beforeMatch, (a,b)-> {
//
//        });



        l.addAll(beforeMatch);

        match.addConditions(l); //the match itself

        l.add(truth.getDerive()); //will be linked to and invoked by match callbacks

        l.add(END);

        return l;
    }

    @NotNull
    public static Solve solver(@NotNull PostCondition p, @NotNull PremiseRule rule, boolean anticipate, boolean eternalize,
                               @NotNull BooleanCondition[] postPreconditions, Temporalize temporalizer) {


        char puncOverride = p.puncOverride;

        BeliefFunction belief = BeliefFunction.get(p.beliefTruth);
        String beliefLabel = belief != null ? p.beliefTruth.toString() : "x";
        DesireFunction desire = DesireFunction.get(p.goalTruth);
        String desireLabel = desire != null ? p.goalTruth.toString() : "x";

        String sn = "Truth:(";
        String i = puncOverride == 0 ?
                sn + beliefLabel + ',' + desireLabel :
                sn + beliefLabel + ',' + desireLabel + ",punc:\"" + puncOverride + '\"';
        i += ')';


        Derive der = new Derive(rule, p.term,
                postPreconditions,
                belief != null && belief.single(),
                desire != null && desire.single(),
                anticipate,
                eternalize, temporalizer);

        return puncOverride == 0 ?
                new Solve.SolvePuncFromTask(i, der, belief, desire) :
                new Solve.SolvePuncOverride(i, der, puncOverride, belief, desire);


    }



    public void setSource(String source) {
        this.source = source;
    }


    /**
     * source string that generated this rule (for debugging)
     */
    public String getSource() {
        return source;
    }

    @Nullable
    protected final Term getTask() {
        return getPremise().term(0);
    }


    @Nullable
    protected final Term getBelief() {
        return getPremise().term(1);
    }

    @Nullable
    protected final Term getConclusionTermPattern() {
        return getConclusion().term(0);
    }


//    @Override
//    public final String toString(boolean pretty) {
//        return str;
//    }

    @Nullable
    public final Term task() {
        return pattern.term(0);
    }

    @Nullable
    public final Term belief() {
        return pattern.term(1);
    }

    /**
     * deduplicate and generate match-optimized compounds for rules
     */
    public void compile(@NotNull TermIndex index) {
        Term[] premisePattern = ((Compound) term(0)).terms();
        premisePattern[0] = index.the(premisePattern[0]).term(); //task pattern
        premisePattern[1] = index.the(premisePattern[1]).term(); //belief pattern
    }

    static final class UppercaseAtomsToPatternVariables implements CompoundTransform<Compound, Term> {


        @Override
        public boolean test(Term term) {
            if (term instanceof Atomic) {
                String name = term.toString();
                return (Character.isUpperCase(name.charAt(0)));
            }
            return false;
        }

        @NotNull
        @Override
        public Termed apply(@NotNull Compound containingCompound, @NotNull Term v, int depth) {

            //do not alter postconditions
            if ((containingCompound.op() == Op.INHERIT)
                    && PostCondition.reservedMetaInfoCategories.contains(
                    containingCompound.term(1)))
                return v;

            return v(Op.VAR_PATTERN, v.toString());
        }
    }

    static final UppercaseAtomsToPatternVariables UppercaseAtomsToPatternVariables = new UppercaseAtomsToPatternVariables();


    @NotNull
    public final PremiseRule normalizeRule(@NotNull PatternIndex index) {
        try {

            //HACK
            Term tt = terms.transform(
                            (Compound)terms.transform(this, UppercaseAtomsToPatternVariables),
                            new PremiseRuleVariableNormalization());

            Compound premiseComponents = (Compound) index.the(tt);


            return new PremiseRule( premiseComponents );

        } catch (Exception e) {
            e.printStackTrace();
            logger.error("normalizeRule untransformed: {} {}", this, e.getCause());
            return null;
        }


    }


    @NotNull
    public final PremiseRule setup(@NotNull PatternIndex index) /* throws PremiseRuleException */ {

        compile(index);

        //1. construct precondition term array
        //Term[] terms = terms();

        Term[] precon = ((Compound) term(0)).terms();
        Term[] postcons = ((Compound) term(1)).terms();


        List<BooleanCondition> pres = Global.newArrayList(precon.length);
        List<BooleanCondition> posts = Global.newArrayList(precon.length);


        Term taskTermPattern = getTaskTermPattern();
        Term beliefTermPattern = getBeliefTermPattern();

        if (beliefTermPattern.op() == Op.ATOM) {
            throw new RuntimeException("belief term must contain no atoms: " + beliefTermPattern);
        }

        //if it contains an atom term, this means it is a modifier,
        //and not a belief term pattern
        //(which will not reference any particular atoms)

        pattern = PatternCompound.make(p(taskTermPattern, beliefTermPattern));


        ListMultimap<Term, MatchConstraint> constraints = MultimapBuilder.treeKeys().arrayListValues().build();

        //additional modifiers: either preConditionsList or beforeConcs, classify them here
        for (int i = 2; i < precon.length; i++) {

            Compound predicate = (Compound) precon[i];
            Term predicate_name = predicate.term(1);

            String predicateNameStr = predicate_name.toString().substring(1);//.replace("^", "");

            BooleanCondition next = null, preNext = null;

            Term[] args;
            Term arg1, arg2;

            //if (predicate.getSubject() instanceof SetExt) {
            //decode precondition predicate arguments
            args = ((Compound) (predicate.term(0))).terms();
            arg1 = (args.length > 0) ? args[0] : null;
            arg2 = (args.length > 1) ? args[1] : null;
            /*} else {
                throw new RuntimeException("invalid arguments");*/
                /*args = null;
                arg1 = arg2 = null;*/
            //}

            switch (predicateNameStr) {

                //postcondition test
                case "not_equal":
                    next = NotEqual.make(arg1, arg2);
                    break;

                case "neq":
                    constraints.put(arg1, new NotEqualsConstraint(arg2));
                    constraints.put(arg2, new NotEqualsConstraint(arg1));

                    //next = NotEqual.make(arg1, arg2); //TODO decide if necesary

                    break;

                case "no_common_subterm":
                    constraints.put(arg1, new NoCommonSubtermsConstraint(arg2));
                    constraints.put(arg2, new NoCommonSubtermsConstraint(arg1));
                    break;


                case "notSet":
                    constraints.put(arg1, new NotOpConstraint(Op.SetsBits));
                    break;
                case "notConjunction":
                    constraints.put(arg1, new NotOpConstraint(Op.CONJUNCTION));
                    break;

                case "notImplicationOrEquivalence":
                    constraints.put(arg1, new NotOpConstraint(Op.ImplicationOrEquivalenceBits));
                    break;
                case "notImplicationEquivalenceOrConjunction":
                    constraints.put(arg1, new NotOpConstraint(Op.ImplicationOrEquivalenceBits | Op.CONJUNCTION.bit()));
                    break;

                case "events":
                    throw new RuntimeException("depr");

                case "time":
                    switch (arg1.toString()) {
                        case "after":
                            preNext = events.after;
                            break;
                        case "afterOrEternal":
                            preNext = events.afterOrEternal;
                            break;
                        /*case "taskPredicate":
                            preNext = events.taskPredicate;
                            break;*/
                        case "dt":
                            temporalize = Temporalize.dt;
                            break;


                        case "dtBelief":
                            temporalize = Temporalize.dtBelief;
                            break;
                        case "dtBeliefEnd":
                            temporalize = Temporalize.dtBeliefEnd;
                            break;
                        case "dtTask":
                            temporalize = Temporalize.dtTask;
                            break;
                        case "dtTaskEnd":
                            temporalize = Temporalize.dtTaskEnd;
                            break;


                        case "dtCombine":
                            temporalize = Temporalize.dtCombine;
                            break;
                        case "dtReverse":
                            temporalize = Temporalize.dtReverse;
                            break;
                        case "dtIfEvent":
                            temporalize = Temporalize.dtIfEvent;
                            break;
                        case "dtAfter":
                            temporalize = Temporalize.dt;
                            preNext = events.after;
                            break;
                        case "dtReverseAfter":
                            temporalize = Temporalize.dtReverse;
                            preNext = events.after;
                            break;
                        case "dtAfterOrEternal":
                            temporalize = Temporalize.dt;
                            preNext = events.afterOrEternal;
                            break;
                        default:
                            throw new RuntimeException("invalid events parameters");
                    }
                    break;

//                case "temporal":
//                    preNext = Temporality.either;
//                    break;

//                case "occurr":
////                    preNext = new occurr(arg1,arg2);
//                    break;

//                case "after":
//                    switch (arg1.toString()) {
//                        case "forward":
//                            preNext = Event.After.forward;
//                            break;
//                        case "reverseStart":
//                            preNext = Event.After.reverseStart;
//                            break;
//                        case "reverseEnd":
//                            preNext = Event.After.reverseEnd;
//                            break;
//                        default:
//                            throw new RuntimeException("invalid after() argument: " + arg1);
//                    }
//                    break;

//                case "dt":
////                    switch (arg1.toString()) {
////                        case "avg":
////                            preNext = dt.avg; break;
////                        case "task":
////                            preNext = dt.task; break;
////                        case "belief":
////                            preNext = dt.belief; break;
////                        case "exact":
////                            preNext = dt.exact; break;
////                        case "sum":
////                            preNext = dt.sum; break;
////                        case "sumNeg":
////                            preNext = dt.sumNeg; break;
////                        case "bmint":
////                            preNext = dt.bmint; break;
////                        case "tminb":
////                            preNext = dt.tminb; break;
////
////                        case "occ":
////                            preNext = dt.occ; break;
////
////                        default:
////                            throw new RuntimeException("invalid dt() argument: " + arg1);
////                    }
//                    break;


                case "task":
                    switch (arg1.toString()) {
                        case "negative":
                            preNext = TaskNegative.the;
                            break;
                        case "\"?\"":
                            preNext = TaskPunctuation.TaskQuestion;
                            break;
                        case "\".\"":
                            preNext = TaskPunctuation.TaskJudgment;
                            break;
                        case "\"!\"":
                            preNext = TaskPunctuation.TaskGoal;
                            break;
                        default:
                            throw new RuntimeException("Unknown task punctuation type: " + predicate.term(0));
                    }
                    break;


                default:
                    throw new RuntimeException("unhandled postcondition: " + predicateNameStr + " in " + this + "");

            }

            if (preNext != null) {
                if (!pres.contains(preNext)) //unique
                    pres.add(preNext);
            }

            if (next != null)
                posts.add(next);

        }


        this.match = new MatchTaskBelief(
                new TaskBeliefPair(pattern.term(0), pattern.term(1)), //HACK
                constraints);


        //store to arrays
        prePreconditions = pres.toArray(new BooleanCondition[pres.size()]);
        postPreconditions = posts.toArray(new BooleanCondition[posts.size()]);


        List<PostCondition> postConditions = Global.newArrayList();

        for (int i = 0; i < postcons.length; ) {
            Term t = postcons[i++];
            if (i >= postcons.length)
                throw new RuntimeException("invalid rule: missing meta term for postcondition involving " + t);


            Term[] modifiers = ((Compound) postcons[i++]).terms();

            PostCondition pc = PostCondition.make(this, t,
                    toSortedSetArray(modifiers));

            if (pc != null)
                postConditions.add(pc);
        }

        if (Sets.newHashSet(postConditions).size() != postConditions.size())
            throw new RuntimeException("postcondition duplicates:\n\t" + postConditions);

        postconditions = postConditions.toArray(new PostCondition[postConditions.size()]);


        //TODO add modifiers to affect minNAL (ex: anything temporal set to 7)
        //this will be raised by conclusion postconditions of higher NAL level
        minNAL =
                Math.max(minNAL,
                        Math.max(
                                maxLevel(pattern.term(0)),
                                maxLevel(pattern.term(1)
                                )));


        ensureValid();

        return this;
    }

    public final Term getTaskTermPattern() {
        return ((Compound) term(0)).terms()[0];
    }

    public final Term getBeliefTermPattern() {
        return ((Compound) term(0)).terms()[1];
    }

    public final void setAllowBackward() {
        this.allowBackward = true;
    }


    /**
     * for each calculable "question reverse" rule,
     * supply to the consumer
     *
     * ex:
     * (A --> B), (B --> C), not_equal(A,C) |- (A --> C), (Truth:Deduction, Desire:Strong, Derive:AllowBackward)
     * 1. Deriving of backward inference rules, since Derive:AllowBackward it allows deriving:
     (A --> B), (A --> C), not_equal(A,C), task("?") |- (B --> C), (Truth:Deduction, Desire:Strong, Derive:AllowBackward)
     (A --> C), (B --> C), not_equal(A,C), task("?") |- (A --> B), (Truth:Deduction, Desire:Strong, Derive:AllowBackward)
     so each premise gets exchanged with the conclusion in order to form a own rule,
     additionally task("?") is added to ensure that the derived rule is only used in backward inference.

     */
    public final void backwardPermutation(@NotNull BiConsumer<PremiseRule, String> w) {

        Term T = getTaskTermPattern(); //Task
        Term B = getBeliefTermPattern(); //Belief
        Term C = getConclusionTermPattern(); //Conclusion

        // C, B, [pre], task_is_question() |- T, [post]
        PremiseRule clone1 = clonePermutation(C, B, T, true);
        w.accept(clone1, "C,B,question |- B");

        // T, C, [pre], task_is_question() |- B, [post]
        PremiseRule clone2 = clonePermutation(T, C, B, true);
        w.accept(clone2, "T,C,question |- B");

    }


//    @Override
//    public Term clone(TermContainer subs) {
//        return null;
//    }

    //    @Override
//    public Term clone(Term[] x) {
//        return new TaskRule((Compound)x[0], (Compound)x[1]);
//    }


    /**
     * for each calculable "question reverse" rule,
     * supply to the consumer
     *
     2. Deriving of forward inference rule by swapping the premises since !s.contains("task(") && !s.contains("after(") && !s.contains("measure_time(") && !s.contains("Structural") && !s.contains("Identity") && !s.contains("Negation"):
     (B --> C), (A --> B), not_equal(A,C) |- (A --> C), (Truth:Deduction, Desire:Strong, Derive:AllowBackward)
     *
     * after generating, these are then backward permuted
     */
    @NotNull
    public final PremiseRule forwardPermutation() {

        // T, B, [pre] |- C, [post] ||--

        Term T = getTaskTermPattern();
        Term B = getBeliefTermPattern();
        Term C = getConclusionTermPattern();

        ////      B, T, [pre], task_is_question() |- T, [post]
        //      B, T, [pre], task_is_question() |- C, [post]

        return clonePermutation(B, T, C, false);
    }

    static final Term TaskQuestionTerm = exec("task", "\"?\"");

    @NotNull
    private PremiseRule clonePermutation(Term newT, Term newB, Term newR, boolean question) {

        Map<Term, Term> m = new HashMap(3);
        m.put(getTaskTermPattern(), newT);
        m.put(getBeliefTermPattern(), newB);
        m.put(getConclusionTermPattern(), newR);

        Compound remapped = (Compound) terms.transform(this, new MapSubst(m));

        //Append taskQuestion
        Compound pc = (Compound) remapped.term(0);
        Term[] pp = pc.terms(); //premise component
        Compound newPremise = question ?
                p(concat(pp, TaskQuestionTerm)) :
                pc;

        return new PremiseRule(newPremise, (Compound) remapped.term(1));


//
//        /*if (StringUtils.countMatches(newPremise.toString(), "task(\"") > 1) {
//            System.err.println(newPremise);
//        }*/
//
//        newPremise.terms()[0] = newT;
//        newPremise.terms()[1] = newB;
//
//        Term[] newConclusion = getConclusion().terms().clone();
//        newConclusion[0] = newR;
//
//
//        return new PremiseRule(newPremise, $.p( newConclusion ));
    }

//    /**
//     * -1 or +1 depending on how arg1 and arg2 match either Task/Belief of the premise
//     * @return +1 if first arg=task, second arg = belief, -1 if opposite,
//     * throws exception if incomplete match
//     */
//    public final int getTaskOrder(Term arg1, Term arg2) {
//
//        Product p = getPremises();
//        Term taskPattern = p.term(0);
//        Term beliefPattern = p.term(1);
//        if (arg2.equals(taskPattern) && arg1.equals(beliefPattern)) {
//            return -1;
//        } else if (arg1.equals(taskPattern) && arg2.equals(beliefPattern)) {
//            return 1;
//        } else {
//            throw new RuntimeException("after(X,Y) needs to match both taks and belief patterns, in one of 2 orderings");
//        }
//
//    }

    //public final int nal() { return minNAL; }

    public static final class PremiseRuleVariableNormalization extends VariableNormalization {


        public static final int ELLIPSIS_ZERO_OR_MORE_ID_OFFSET = 1 * 256;
        public static final int ELLIPSIS_ONE_OR_MORE_ID_OFFSET = 2 * 256;
        public static final int ELLIPSIS_TRANSFORM_ID_OFFSET = 3 * 256;

        int offset;

        public static AbstractVariable varPattern(int i) {
            return v(VAR_PATTERN, i);
        }

        @NotNull
        @Override
        protected Variable newVariable(@NotNull Term v, int serial) {


            int actualSerial = serial + offset;

            if (v instanceof Ellipsis.EllipsisTransformPrototype) {
                //special

                Ellipsis.EllipsisTransformPrototype ep = (Ellipsis.EllipsisTransformPrototype) v;

//                Term from = ep.from;
//                if (from != Op.Imdex) from = applyAfter((GenericVariable)from);
//                Term to = ep.to;
//                if (to != Op.Imdex) to = applyAfter((GenericVariable)to);
//
                return EllipsisTransform.make(varPattern(actualSerial + ELLIPSIS_TRANSFORM_ID_OFFSET), ep.from, ep.to, this);

            } else if (v instanceof Ellipsis.EllipsisPrototype) {
                Ellipsis.EllipsisPrototype ep = (Ellipsis.EllipsisPrototype) v;
                return Ellipsis.EllipsisPrototype.make(actualSerial +
                                (ep.minArity == 0 ? ELLIPSIS_ZERO_OR_MORE_ID_OFFSET : ELLIPSIS_ONE_OR_MORE_ID_OFFSET) //these need to be distinct
                        , ep.minArity);
            } else if (v instanceof Ellipsis) {

                int idOffset;
                if (v instanceof EllipsisTransform) {
                    idOffset = ELLIPSIS_TRANSFORM_ID_OFFSET;
                } else if (v instanceof EllipsisZeroOrMore) {
                    idOffset = ELLIPSIS_ZERO_OR_MORE_ID_OFFSET;
                } else if (v instanceof EllipsisOneOrMore) {
                    idOffset = ELLIPSIS_ONE_OR_MORE_ID_OFFSET;
                } else {
                    throw new RuntimeException("N/A");
                }

                Variable r = ((Ellipsis) v).clone(varPattern(actualSerial + idOffset), this);
                offset = 0; //return to zero
                return r;
            } else if (v instanceof GenericVariable) {
                return ((GenericVariable) v).normalize(actualSerial); //HACK
            } else {
                return v(v.op(), actualSerial);
            }
        }

        @Override
        public final boolean testSuperTerm(@NotNull Compound t) {
            //descend all, because VAR_PATTERN is not yet always considered a variable
            return true;
        }

        public Termed applyAfter(Variable secondary) {
            offset++;
            return apply(null, secondary, -1);
        }
    }
}




