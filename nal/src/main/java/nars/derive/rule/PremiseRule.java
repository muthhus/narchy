package nars.derive.rule;

import com.google.common.collect.Sets;
import jcog.list.FasterList;
import nars.$;
import nars.NAR;
import nars.Op;
import nars.derive.*;
import nars.derive.constraint.*;
import nars.derive.op.*;
import nars.index.term.PatternTermIndex;
import nars.index.term.TermIndex;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Terms;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.compound.GenericCompound;
import nars.term.container.TermContainer;
import nars.term.container.TermVector;
import nars.term.transform.CompoundTransform;
import nars.truth.func.BeliefFunction;
import nars.truth.func.GoalFunction;
import nars.truth.func.TruthOperator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;

import static java.lang.Math.max;
import static java.util.Collections.addAll;
import static nars.$.*;
import static nars.Op.CONJ;
import static nars.Op.PROD;
import static nars.term.Terms.concat;
import static nars.term.Terms.maxLevel;

/**
 * A rule which matches a Premise and produces a Task
 * contains: preconditions, predicates, postconditions, post-evaluations and metainfo
 */
public class PremiseRule extends GenericCompound {

    public static final Atomic Task = Atomic.the("task");
    static final Atomic Belief = Atomic.the("belief");
    private static final Term TaskAny = $.func("task", Atomic.the("any"));
    private static final Term QUESTION_PUNCTUATION = $.inh(Atomic.the("Question"), Atomic.the("Punctuation"));


    public boolean permuteBackward = false;
    public boolean permuteForward = false;


    /**
     * conditions which can be tested before unification
     */
    private PrediTerm[] PRE;

    /**
     * consequences applied after unification
     */
    public PostCondition[] POST;


    /**
     * maximum of the minimum NAL levels involved in the postconditions of this rule
     */
    public int minNAL;

    public String source;

    @Nullable
    private MatchTaskBelief match;

    /**
     * unless time(raw), projected belief truth will be used by default
     */
    private boolean beliefProjected = true;

    /**
     * for printing complex terms as a recursive tree
     */
    public static void printRecursive(@NotNull Term x) {
        Terms.printRecursive(System.out, x);
    }

    @NotNull
    private Compound getPremise() {
        return (Compound) sub(0);
    }

    @NotNull
    private Compound getConclusion() {
        return (Compound) sub(1);
    }

    public PremiseRule(TermContainer premiseAndResult) {
        super(PROD, premiseAndResult);
    }


    /**
     * compiles the conditions which are necessary to activate this rule
     */
    @NotNull
    public List<Term> conditions(@NotNull PostCondition post, NAR nar) {

        Set<Term> s = newHashSet(16); //for ensuring uniqueness / no duplicates

        byte puncOverride = post.puncOverride;

        TruthOperator belief = BeliefFunction.get(post.beliefTruth);
        if ((post.beliefTruth != null) && !post.beliefTruth.equals(TruthOperator.NONE) && (belief == null)) {
            throw new RuntimeException("unknown BeliefFunction: " + post.beliefTruth);
        }
        TruthOperator goal = GoalFunction.get(post.goalTruth);
        if ((post.goalTruth != null) && !post.goalTruth.equals(TruthOperator.NONE) && (goal == null)) {
            throw new RuntimeException("unknown GoalFunction: " + post.goalTruth);
        }

        Conclude conc = new Conclude(this, post.pattern);

        String beliefLabel = belief != null ? belief.toString() : "_";
        String goalLabel = goal != null ? goal.toString() : "_";

        List<Term> args = $.newArrayList(
                $.the(beliefLabel),
                $.the(goalLabel)
        );
        if (puncOverride != 0)
            args.add($.quote(((char) puncOverride)));

        if (!beliefProjected)
            args.add($.the("unproj"));

        Compound ii = $.func("truth", args);


        Solve truth = puncOverride == 0 ?
                new SolvePuncFromTask(ii, belief, goal, beliefProjected) :
                new SolvePuncOverride(ii, puncOverride, belief, goal, beliefProjected);

        //PREFIX
        {
            addAll(s, PRE);

            s.add(truth);

            s.addAll(match.pre);

        }

        List<Term> l = sort(new FasterList(s));

        l.addAll(match.constraints);

        //SUFFIX (order already determined for matching)
        {

            l.addAll(match.post);

            ((UnificationPrototype) match.post.get(match.post.size() - 1)).conclude.add(conc.apply(nar));
        }

        return l;
    }

    /**
     * higher is earlier
     */
    private static final HashMap<Object, Integer> preconditionScore = new HashMap() {{

        int rank = 50;

        put("PatternOp1", rank--);
        put("PatternOp0", rank--);

        put("BeliefExist", rank--);

        put(TaskBeliefOp.class, rank--);

        put(TaskPunctuation.class, rank--);

        put(TaskBeliefOccurrence.class, rank--);

        put(TaskBeliefHas.class, rank--);

        put(SubTermStructure.class, rank--);

        put(TaskPolarity.class, rank--); //includes both positive or negative
        put(BeliefPolarity.class, rank--);


        put(Solve.class, rank--);

    }};

    private static Object classify(Term b) {
        if (b instanceof AbstractPatternOp.PatternOp)
            return "PatternOp" + (((AbstractPatternOp.PatternOp) b).subterm == 0 ? "0" : "1"); //split

        if ((b == TaskPolarity.pos) || (b == TaskPolarity.neg)) return TaskPolarity.class;

        if (b == BeliefPolarity.beliefExist) return "BeliefExist";
        if ((b == BeliefPolarity.beliefPos) || (b == BeliefPolarity.beliefNeg)) return BeliefPolarity.class;

        if (b.getClass() == TaskBeliefHas.class) return TaskBeliefHas.class;

        if (b == TaskPunctuation.Goal) return TaskPunctuation.class;
        if (b == TaskPunctuation.Belief) return TaskPunctuation.class;
        if (b == TaskPunctuation.Question) return TaskPunctuation.class;
        if (b == TaskPunctuation.Quest) return TaskPunctuation.class;
        if (b == TaskPunctuation.QuestionOrQuest) return TaskPunctuation.class;
        if (b.getClass() == TaskBeliefOp.class) return TaskBeliefOp.class;

//        if (b instanceof TermNotEquals) return TermNotEquals.class;

        if (b == TaskBeliefOccurrence.bothEvents) return TaskBeliefOccurrence.class;
//        if (b == TaskBeliefOccurrence.afterOrEternal) return TaskBeliefOccurrence.class;
        if (b == TaskBeliefOccurrence.eventsOrEternals) return TaskBeliefOccurrence.class;
//        if (b == TaskBeliefOccurrence.beliefDTSimultaneous) return TaskBeliefOccurrence.class;

        if (b instanceof SubTermStructure) return SubTermStructure.class;

        if (b instanceof Solve) return Solve.class;

        throw new UnsupportedOperationException("unranked precondition: " + b);
        //return b.getClass();
    }

    /**
     * apply deterministic and uniform sort to the current preconditions.
     * the goal of this is to maximally fold subexpressions while also
     * pulling the cheapest and most discriminating tests to the beginning.
     */
    @NotNull
    private static List<Term> sort(@NotNull List<Term> l) {

        Collections.sort(l, (a, b) -> {

            Object ac = classify(a);
            Object bc = classify(b);

            HashMap<Object, Integer> ps = PremiseRule.preconditionScore;
            int c = Integer.compare(ps.get(bc) /*getOrDefault(bc, -1)*/, ps.get(ac) /*ps.getOrDefault(ac, -1)*/);
            return (c != 0) ? c : b.toString().compareTo(a.toString());
        });

        return l;
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

    /**
     * the task-term pattern
     */
    @NotNull
    public final Term getTask() {
        return (getPremise().sub(0));
    }

    /**
     * the belief-term pattern
     */
    @NotNull
    public final Term getBelief() {
        return (getPremise().sub(1));
    }

    @NotNull
    private Term getConclusionTermPattern() {
        return getConclusion().sub(0);
    }


    /**
     * deduplicate and generate match-optimized compounds for rules
     */
    private void compile(@NotNull TermIndex index) {
        Term[] premisePattern = ((Compound) sub(0)).toArray();
        premisePattern[0] = index.get(premisePattern[0], true).term(); //task pattern
        premisePattern[1] = index.get(premisePattern[1], true).term(); //belief pattern
    }

    private static final CompoundTransform UppercaseAtomsToPatternVariables = (containingCompound, v) -> {

        if (v instanceof Atom) {
            String name = v.toString();
            if (Character.isUpperCase(name.charAt(0)) && name.length() == 1) {
                //do not alter postconditions
                return (containingCompound.op() == Op.INH) && PostCondition.reservedMetaInfoCategories.contains(containingCompound.sub(1)) ?
                        v : v(Op.VAR_PATTERN, v.toString());

            }
        } /*else if (v.op().temporal) {
            return v.dt(XTERNAL); //convert to XTERNAL to allow free retemporalization
        }*/
        return v;
    };


    @NotNull
    public final PremiseRule normalizeRule(@NotNull PatternTermIndex index) {
        return new PremiseRule((Compound) index.pattern(
                (Compound) transform(UppercaseAtomsToPatternVariables)
        ));
    }


    @NotNull
    public final PremiseRule setup(@NotNull PatternTermIndex index) /* throws PremiseRuleException */ {

        compile(index);

        //1. construct precondition term array
        //Term[] terms = terms();

        Term[] precon = ((Compound) sub(0)).toArray();
        Term[] postcons = ((Compound) sub(1)).toArray();


        Set<PrediTerm> pres =
                //Global.newArrayList(precon.length);
                new TreeSet(); //for consistent ordering to maximize folding


        Term taskTermPattern = getTask();
        Term beliefTermPattern = getBelief();

        if (beliefTermPattern.op() == Op.ATOM) {
            throw new RuntimeException("belief term must contain no atoms: " + beliefTermPattern);
        }

        //if it contains an atom term, this means it is a modifier,
        //and not a belief term pattern
        //(which will not reference any particular atoms)

        //pattern = PatternCompound.make(p(taskTermPattern, beliefTermPattern));


        SortedSet<MatchConstraint> constraints = new TreeSet();

        char taskPunc = 0;


        //additional modifiers: either preConditionsList or beforeConcs, classify them here
        for (int i = 2; i < precon.length; i++) {

            Compound predicate = (Compound) precon[i];
            Term predicate_name = predicate.sub(1);

            String predicateNameStr = predicate_name.toString();

            Term[] args;
            Term X, Y, Z;

            //if (predicate.getSubject() instanceof SetExt) {
            //decode precondition predicate arguments
            args = ((Compound) (predicate.sub(0))).toArray();
            X = (args.length > 0) ? args[0] : null;
            Y = (args.length > 1) ? args[1] : null;
            Z = (args.length > 2) ? args[2] : null;
            //..

            /*} else {
                throw new RuntimeException("invalid arguments");*/
                /*args = null;
                arg1 = arg2 = null;*/
            //}

            String XString = X.toString();
            switch (predicateNameStr) {


                case "neq":
                    //neqPrefilter(pres, taskTermPattern, beliefTermPattern, X, Y, neq);
                    neq(constraints, X, Y); //should the constraints be ommited in this case?
                    break;


                case "neqAndCom":
                    //neqPrefilter(pres, taskTermPattern, beliefTermPattern, X, Y, neq);
                    neq(constraints, X, Y);
                    constraints.add(new CommonSubtermConstraint(X, Y));
                    constraints.add(new CommonSubtermConstraint(Y, X));
                    break;

                case "neqCom":
                    //neqPrefilter(pres, taskTermPattern, beliefTermPattern, X, Y, neqCom);
                    constraints.add(new NoCommonSubtermConstraint(X, Y, false));
                    constraints.add(new NoCommonSubtermConstraint(Y, X, false));
                    break;
                case "neqRCom":
                    //neqPrefilter(pres, taskTermPattern, beliefTermPattern, X, Y, neqRCom);
                    constraints.add(new NoCommonSubtermConstraint(X, Y, true));
                    constraints.add(new NoCommonSubtermConstraint(Y, X, true));
                    break;

                case "notSet":
                    opNot(taskTermPattern, beliefTermPattern, pres, constraints, X, Op.SetBits);
                    break;

                case "set":
                    pres.add(new TaskBeliefHas(Op.SetBits, taskTermPattern.contains(X), beliefTermPattern.contains(X)));
                    constraints.add(new OpInConstraint(X, Op.SETi, Op.SETe));
                    break;

                case "setext": //TODO rename: opSETe
                    isOp(pres, taskTermPattern, beliefTermPattern, constraints, X, Op.SETe);
                    break;

                case "setint": //TODO rename: opSETi
                    isOp(pres, taskTermPattern, beliefTermPattern, constraints, X, Op.SETi);
                    break;
                case "opSECTe":
                    isOp(pres, taskTermPattern, beliefTermPattern, constraints, X, Op.SECTe);
                    break;
                case "opSECTi":
                    isOp(pres, taskTermPattern, beliefTermPattern, constraints, X, Op.SECTi);
                    break;


                case "notEqui":
                    opNotContaining(taskTermPattern, beliefTermPattern, pres, constraints, X, Op.EQUI.bit);
                    break;

                case "notImplEqui":
                    opNotContaining(taskTermPattern, beliefTermPattern, pres, constraints, X, Op.ImplicationOrEquivalenceBits);
                    break;

//                case "events":
//                    throw new RuntimeException("depr");

//                case "contains":
//                    pres.add(new ContainedBy(withinNonCommutive(taskTermPattern, beliefTermPattern, X, Y)));
//                    break;

//                case "component":
//                    if (XString.equals("task") && Y.toString().equals("belief")) {
//                        pres.add(new ComposedBy(0,1));
//                    } else {
//                        throw new UnsupportedOperationException();
//                    }
//                    break;

                case "time":
                    switch (XString) {
                        case "raw":
                            beliefProjected = false;
                            break;
                        case "dtEvents":
                            pres.add(TaskBeliefOccurrence.bothEvents);
                            minNAL = 7;
                            break;
                        case "dtEventsReverse":
                            pres.add(TaskBeliefOccurrence.bothEvents);
                            minNAL = 7;
                            break;
                        //NOTE THIS SHOULD ACTUALLY BE CALLED dtBeforeAfterOrEternal or something
                        case "dtEventsOrEternals":
                            pres.add(TaskBeliefOccurrence.eventsOrEternals);
                            break;
                        case "dtEventsOrEternalsReverse":
                            pres.add(TaskBeliefOccurrence.eventsOrEternals);
                            break;

                        default:
                            throw new UnsupportedOperationException("time(" + XString + ") unknown");
                            //TODO warn about missing ones
                    }
                    break;

//                case "temporal":
//                    pres.add( Temporality.either;
//                    break;

//                case "occurr":
////                    pres.add( new occurr(arg1,arg2);
//                    break;

//                case "after":
//                    switch (arg1.toString()) {
//                        case "forward":
//                            pres.add( Event.After.forward;
//                            break;
//                        case "reverseStart":
//                            pres.add( Event.After.reverseStart;
//                            break;
//                        case "reverseEnd":
//                            pres.add( Event.After.reverseEnd;
//                            break;
//                        default:
//                            throw new RuntimeException("invalid after() argument: " + arg1);
//                    }
//                    break;

//                case "dt":
////                    switch (arg1.toString()) {
////                        case "avg":
////                            pres.add( dt.avg; break;
////                        case "task":
////                            pres.add( dt.task; break;
////                        case "belief":
////                            pres.add( dt.belief; break;
////                        case "exact":
////                            pres.add( dt.exact; break;
////                        case "sum":
////                            pres.add( dt.sum; break;
////                        case "sumNeg":
////                            pres.add( dt.sumNeg; break;
////                        case "bmint":
////                            pres.add( dt.bmint; break;
////                        case "tminb":
////                            pres.add( dt.tminb; break;
////
////                        case "occ":
////                            pres.add( dt.occ; break;
////
////                        default:
////                            throw new RuntimeException("invalid dt() argument: " + arg1);
////                    }
//                    break;

                case "belief":
                    switch (XString) {
                        case "negative":
                            pres.add(BeliefPolarity.beliefNeg);
                            break;
                        case "positive":
                            pres.add(BeliefPolarity.beliefPos);
                            break;

                        //HACK do somethign other than duplciate this with the "task" select below, and also generalize to all ops
                        case "\"*\"":
                            pres.add(new TaskBeliefOp(PROD, false, true));
                            break;
                        case "\"&&\"":
                            pres.add(new TaskBeliefOp(CONJ, false, true));
                            break;
                        default: throw new UnsupportedOperationException();
                    }
                    break;

                case "task":
                    switch (XString) {
                        case "negative":
                            pres.add(TaskPolarity.neg);
                            break;
                        case "positive":
                            pres.add(TaskPolarity.pos);
                            break;
                        case "\"?\"":
                            pres.add(TaskPunctuation.Question);
                            taskPunc = '?';
                            break;
                        case "\"?@\"":
                            pres.add(TaskPunctuation.QuestionOrQuest);
                            taskPunc = '?'; //this will choose quest as punctuation type when necessary, according to the task
                            break;
                        case "\"@\"":
                            pres.add(TaskPunctuation.Quest);
                            taskPunc = '@';
                            break;
                        case "\".\"":
                            pres.add(TaskPunctuation.Belief);
                            taskPunc = '.';
                            break;
                        case "\"!\"":
                            pres.add(TaskPunctuation.Goal);
                            taskPunc = '!';
                            break;

                        case "\"*\"":
                            pres.add(new TaskBeliefOp(PROD, true, false));
                            break;
                        case "\"&&\"":
                            pres.add(new TaskBeliefOp(CONJ, true, false));
                            break;

                        case "any":
                            taskPunc = ' ';
                            break;

                        default:
                            throw new RuntimeException("Unknown task punctuation type: " + XString);
                    }
                    break;


                default:
                    throw new RuntimeException("unhandled postcondition: " + predicateNameStr + " in " + this);

            }
        }

        this.match = new MatchTaskBelief(
                getTask(), getBelief(), //HACK
                constraints);

        List<PostCondition> postConditions = newArrayList(postcons.length);

        for (int i = 0; i < postcons.length; ) {
            Term t = postcons[i++];
            if (i >= postcons.length)
                throw new RuntimeException("invalid rule: missing meta term for postcondition involving " + t);

            Term[] modifiers = ((Compound) postcons[i++]).toArray();

            postConditions.add(PostCondition.make(this, t, Terms.sorted(modifiers)));
        }

        if (taskPunc == 0) {
            //default: add explicit no-questions rule
            // TODO restrict this further somehow
            //pres.add(TaskPunctuation.NotQuestion);
        } else if (taskPunc == ' ') {
            //any task type
            taskPunc = 0;
        }

        //store to arrays
        this.PRE = pres.toArray(new PrediTerm[pres.size()]);


        if (Sets.newHashSet(postConditions).size() != postConditions.size())
            throw new RuntimeException("postcondition duplicates:\n\t" + postConditions);

        POST = postConditions.toArray(new PostCondition[postConditions.size()]);
        if (POST.length == 0) {
            //System.out.println(Arrays.toString(postcons));
            throw new RuntimeException("no postconditions");
        }

        //TODO add modifiers to affect minNAL (ex: anything temporal set to 7)
        //this will be raised by conclusion postconditions of higher NAL level
        minNAL = max(minNAL,
                max(maxLevel(getConclusionTermPattern()),
                        max(maxLevel(getTask()),
                                maxLevel(getBelief())
                        )));


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
//        if (!getTask().hasVarPattern())
//            throw new RuntimeException("rule's task term pattern has no pattern variable");
//        if (!getBelief().hasVarPattern())
//            throw new RuntimeException("rule's task belief pattern has no pattern variable");
//        if (!getConclusionTermPattern().hasVarPattern())
//            throw new RuntimeException("rule's conclusion belief pattern has no pattern variable");

        return this;
    }

    private static void isOp(Set<PrediTerm> pres, Term taskTermPattern, Term beliefTermPattern, SortedSet<MatchConstraint> constraints, Term x, Op v) {
        pres.add(new TaskBeliefHas(v.bit, taskTermPattern.contains(x), beliefTermPattern.contains(x)));
        constraints.add(new OpConstraint(x, v));
    }


    private static void opNot(Term task, Term belief, @NotNull Set<PrediTerm> pres, @NotNull SortedSet<MatchConstraint> constraints, @NotNull Term t, int structure) {

        constraints.add(new OpExclusionConstraint(t, structure));
    }


    private static void opNotContaining(Term task, Term belief, @NotNull Set<PrediTerm> pres, @NotNull SortedSet<MatchConstraint> constraints, @NotNull Term t, int structure) {
        constraints.add(new StructureExclusionConstraint(t, structure));
    }

    private void neq(@NotNull SortedSet<MatchConstraint> constraints, @NotNull Term x, @NotNull Term y) {
        constraints.add(new NotEqualConstraint(x, y));
        constraints.add(new NotEqualConstraint(y, x));
    }


    /**
     * for each calculable "question reverse" rule,
     * supply to the consumer
     * <p>
     * ex:
     * (A --> B), (B --> C), not_equal(A,C) |- (A --> C), (Truth:Deduction, Goal:Strong, Derive:AllowBackward)
     * 1. Deriving of backward inference rules, since Derive:AllowBackward it allows deriving:
     * (A --> B), (A --> C), not_equal(A,C), task("?") |- (B --> C), (Truth:Deduction, Goal:Strong, Derive:AllowBackward)
     * (A --> C), (B --> C), not_equal(A,C), task("?") |- (A --> B), (Truth:Deduction, Goal:Strong, Derive:AllowBackward)
     * so each premise gets exchanged with the conclusion in order to form a own rule,
     * additionally task("?") is added to ensure that the derived rule is only used in backward inference.
     */
    public final void backwardPermutation(@NotNull PatternTermIndex index, @NotNull BiConsumer<PremiseRule, String> w) {

        Term T = getTask(); //Task
        Term B = getBelief(); //Belief
        Term C = getConclusionTermPattern(); //Conclusion

        {
            // C, B, [pre], task_is_question() |- T, [post]
            PremiseRule clone1 = clonePermutation(C, B, T, true, index);
            if (clone1 != null)
                w.accept(clone1, "C,B,question |- T");
        }

        {
            // T, C, [pre], task_is_question() |- B, [post]
            PremiseRule clone3 = clonePermutation(T, C, B, true, index);
            if (clone3 != null)
                w.accept(clone3, "T,C,question |- B");
        }

        //if needed, use Swap which would be applied before this recursively,
//        // T, C, [pre], task_is_question() |- B, [post]
//        PremiseRule clone2 = clonePermutation(C, T, B, true, index);
//        if (clone2 != null)
//            w.accept(clone2, "C,T,question |- B");


    }


    /**
     * for each calculable "question reverse" rule,
     * supply to the consumer
     * <p>
     * 2. Deriving of forward inference rule by swapping the premises since !s.contains("task(") && !s.contains("after(") && !s.contains("measure_time(") && !s.contains("Structural") && !s.contains("Identity") && !s.contains("Negation"):
     * (B --> C), (A --> B), not_equal(A,C) |- (A --> C), (Truth:Deduction, Goal:Strong, Derive:AllowBackward)
     * <p>
     * after generating, these are then backward permuted
     */
    @Nullable
    public final PremiseRule swapPermutation(@NotNull PatternTermIndex index) {

        // T, B, [pre] |- C, [post] ||--
        Term T = getTask();
        Term B = getBelief();

        if (T.equals(B)) {
            //no change, ignore the permutation
            return null;
        } else {
            Term C = getConclusionTermPattern();
            return clonePermutation(B, T, C, false, index);
        }
    }


    @NotNull
    private PremiseRule clonePermutation(Term newT, Term newB, Term newR, boolean question, @NotNull PatternTermIndex index) {


        Map<Term, Term> m = new HashMap(3);
        m.put(getTask(), newT);
        m.put(getBelief(), newB); //index.retemporalize(?

        //boolean swapTruth = (!question && getTask().equals(newB) && getBelief().equals(newT));

        m.put(getConclusionTermPattern(), newR);


        Compound remapped = (Compound) index.replace(this, m);

        //Append taskQuestion
        Compound pc = (Compound) remapped.sub(0);
        Term[] pp = pc.toArray(); //premise component
        Compound newPremise;

        Compound newConclusion = (Compound) remapped.sub(1);

        if (question) {

            newPremise = $.p(concat(pp, TaskAny));
            //newPremise = pc; //same


            //remove truth values and add '?' punct
//            TermContainer ss = ((Compound) newConclusion.sub(1)).subterms();
//            newConclusion = p(
//
//                    newConclusion.sub(0), $.p(ss.asFiltered((x) -> {
//                        Compound cx = (Compound) x;
//                        return !(cx.op() == Op.INH && (
//                                cx.sub(1).equals(BELIEF)
//                                        || cx.sub(1).equals(GOAL)));
//                    }).append(QUESTION_PUNCTUATION))
//            );
            newConclusion = $.p(newConclusion.sub(0), p(QUESTION_PUNCTUATION));

        } else {
//            if (swapTruth) {
//                newConclusion = (Compound) index.transform(newConclusion, truthSwap);
//            }


            newPremise = pc; //same
        }

        return PremiseRuleSet.normalize(new PremiseRule(TermVector.the(newPremise, newConclusion)), index);

    }


}




