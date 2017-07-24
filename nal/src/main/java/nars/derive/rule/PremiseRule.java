package nars.derive.rule;

import com.google.common.collect.Sets;
import jcog.list.FasterList;
import nars.$;
import nars.NAR;
import nars.Op;
import nars.control.Derivation;
import nars.derive.*;
import nars.derive.constraint.*;
import nars.derive.match.Ellipsis;
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
import nars.term.transform.VariableNormalization;
import nars.term.var.AbstractVariable;
import nars.term.var.Variable;
import nars.time.TimeFunctions;
import nars.truth.func.BeliefFunction;
import nars.truth.func.GoalFunction;
import nars.truth.func.TruthOperator;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;

import static java.lang.Math.max;
import static java.util.Collections.addAll;
import static nars.$.newArrayList;
import static nars.$.newHashSet;
import static nars.$.p;
import static nars.$.v;
import static nars.Op.*;
import static nars.term.Terms.concat;
import static nars.term.Terms.maxLevel;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.ETERNAL;

/**
 * A rule which matches a Premise and produces a Task
 * contains: preconditions, predicates, postconditions, post-evaluations and metainfo
 */
public class PremiseRule extends GenericCompound {

    public static final Atomic Task = Atomic.the("task");
    static final Atomic Belief = Atomic.the("belief");
    static final Term TaskAny = $.func("task", Atomic.the("any"));
    static final Term QUESTION_PUNCTUATION = $.inh(Atomic.the("Question"), Atomic.the("Punctuation"));


    public boolean permuteBackward = false;
    public boolean permuteForward = false;


    /**
     * conditions which can be tested before unification
     */
    public PrediTerm[] PRE;

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

    @Nullable
    private TimeFunctions timeFunction = TimeFunctions.Auto;

    /**
     * unless time(raw), projected belief truth will be used by default
     */
    boolean beliefProjected = true;

//    @Nullable
//    private static final CompoundTransform truthSwap = new PremiseTruthTransform(true, true) {
//        @Override
//        public Term apply(@NotNull Term func) {
//            return Atomic.the(func.toString() + 'X');
//        }
//    };

    /**
     * for printing complex terms as a recursive tree
     */
    public static void printRecursive(@NotNull Term x) {
        Terms.printRecursive(System.out, x);
    }

    @NotNull
    public final Compound getPremise() {
        return (Compound) sub(0);
    }

    @NotNull
    public final Compound getConclusion() {
        return (Compound) sub(1);
    }

    public PremiseRule(TermContainer premiseAndResult) {
        super(PROD, premiseAndResult);
    }


//    public final boolean validTaskPunctuation(final char p) {
//        if ((p == Symbols.QUESTION) && !allowQuestionTask)
//            return false;
//        return true;
//    }


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

        Conclude conc = new Conclude(this, post.pattern, timeFunction);

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

//        if (belief!=null && goal!=null && !belief.single() && !goal.single())
//            s.add(BeliefPolarity.beliefExist); //quick filter TODO verify if this is safe with respect to questions

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

    //    public static void eachOperator(NAR nar, BiConsumer<Class, TermTransform> eachTransform) {
//        for (Class<? extends TermTransform> c : PremiseRule.Operators) {
//
//            Constructor<?>[] ccc = c.getConstructors();
//            try {
//                int n = 0;
//                TermTransform o = null;
//                do {
//                    Constructor cc = ccc[n++];
//
//                    if (Modifier.isPublic(cc.getModifiers())) {
//                        int params = cc.getParameterCount();
//                        if (params == 0) {
//                            //default empty constructor
//                            o = (c.newInstance());
//                        } else if (params == 1) {
//                            //HACK support 'NAR' only parameter constructor
//                            o = ((TermTransform) cc.newInstance(nar));
//                        }
//                    }
//                } while (o == null && n < ccc.length);
//
//                eachTransform.accept(c, o);
//
//            } catch (Exception e) {
//                throw new RuntimeException("Invalid ImmediateTermTransform: " + c);
//            }
//
//
//        }
//    }


    /**
     * higher is earlier
     */
    static final HashMap<Object, Integer> preconditionScore = new HashMap() {{

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

//        put(TermNotEquals.class, rank--);

//        put(PatternOpNot.class, rank--);


//        put(SubTermOp.class, 10);
//        put(TaskPunctuation.class, 9);
//        put(TaskNegative.class, 8);
//        put(SubTermStructure.class, 7);
//        put(Solve.class, 1);
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
        if (b == TaskBeliefOccurrence.afterOrEternal) return TaskBeliefOccurrence.class;
        if (b == TaskBeliefOccurrence.eventsOrEternals) return TaskBeliefOccurrence.class;
        if (b == TaskBeliefOccurrence.beliefDTSimultaneous) return TaskBeliefOccurrence.class;

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
    protected final Term getConclusionTermPattern() {
        return getConclusion().sub(0);
    }


//    @Override
//    public final String toString(boolean pretty) {
//        return str;
//    }

//    @Nullable
//    public final Term task() {
//        return pattern.term(0);
//    }
//
//    @Nullable
//    public final Term belief() {
//        return pattern.term(1);
//    }

    /**
     * deduplicate and generate match-optimized compounds for rules
     */
    public void compile(@NotNull TermIndex index) {
        Term[] premisePattern = ((Compound) sub(0)).toArray();
        premisePattern[0] = index.get(premisePattern[0], true).term(); //task pattern
        premisePattern[1] = index.get(premisePattern[1], true).term(); //belief pattern
    }

//    @NotNull
//    public Term reified() {
//
//        //TODO include representation of precondition and postconditions
//        return $.impl(
//                p(getTask(), getBelief()),
//                getConclusion()
//        );
//    }

    static final CompoundTransform UppercaseAtomsToPatternVariables = (containingCompound, v) -> {

        if (v instanceof Atom) {
            String name = v.toString();
            if (Character.isUpperCase(name.charAt(0)) && name.length() == 1) {
                //do not alter postconditions
                return (containingCompound.op() == Op.INH) && PostCondition.reservedMetaInfoCategories.contains(containingCompound.sub(1)) ?
                        v : v(Op.VAR_PATTERN, v.toString());

            }
        }
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
                    timeFunction = time(pres, Y, Z, XString);
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
        minNAL = max( minNAL,
                    max( maxLevel(getConclusionTermPattern()),
                        max( maxLevel(getTask()),
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

    public static void isOp(Set<PrediTerm> pres, Term taskTermPattern, Term beliefTermPattern, SortedSet<MatchConstraint> constraints, Term x, Op v) {
        pres.add(new TaskBeliefHas(v.bit, taskTermPattern.contains(x), beliefTermPattern.contains(x)));
        constraints.add(new OpConstraint(x, v));
    }

    public TimeFunctions time(Set<PrediTerm> pres, Term y, Term z, String XString) {
        TimeFunctions timeFunction = TimeFunctions.Auto;
        switch (XString) {
            case "task":
                timeFunction = (Term derived, @NotNull Derivation p, @NotNull long[] occReturn, @NotNull float[] confScale) -> {
                    long occ = p.task.start();
                    occReturn[0] = occ;
                    boolean temporal = false;
                    if (occ != ETERNAL) {
                        temporal = true;
                        @Nullable Term yr = p.transform(y).eval(p.terms);
                        if (yr == null)
                            return null;
                        int occShift = p.taskTerm.subtermTime(yr);
                        if (occShift != DTERNAL)
                            occReturn[0] += occShift;
                    } else if (p.belief != null && p.belief.start() != ETERNAL) {
                        temporal = true;
                        occReturn[0] = p.belief.start();
                        if (z != null) {
                            //if Z occurrs in belief, then there is an additional shift
                            @Nullable Term zr = p.transform(z).eval(p.terms);
                            if (zr == null)
                                return null;
                            int relOccShift = p.taskTerm.subtermTime(zr);
                            if (relOccShift != DTERNAL)
                                occReturn[0] -= (relOccShift - p.taskTerm.dtRange());

                            @Nullable Term yr = p.transform(y).eval(p.terms);
                            if (yr == null)
                                return null;
                            int occShift = p.taskTerm.subtermTime(yr);
                            if (occShift != DTERNAL)
                                occReturn[0] += occShift;
                        }
                    }

                    if (temporal && occReturn[0] == ETERNAL) {
                        return null; //uncomputable temporal basis
                    }

                    TimeFunctions.shiftIfImmediate(p, occReturn, derived);

                    //HACK retemporalize a non-temporal conjunction result
                    if (p.taskTerm.op() == CONJ && p.taskTerm.dt() != DTERNAL &&
                            derived.op() == CONJ && derived.dt() == DTERNAL) {

                        switch (derived.size()) {
                            case 2:
                                Term A = derived.sub(0);
                                int a = p.taskTerm.subtermTime(A);
                                Term B = derived.sub(1);
                                int b = p.taskTerm.subtermTime(B);
                                if ((a > 0) && (occReturn[0] != ETERNAL)) {
                                    //shift ahead, aligning the first subterm with its position in the task
                                    occReturn[0] += a;
                                    b -= a;
                                }
                                derived = CONJ.the(b - a, A, B);
                                break;
                            default:
                                assert (derived.dt() == DTERNAL);
                                derived = p.terms.the((Compound)derived, 0);
                                break;
                        }
                    }

                    return filterEternalBasis(derived, p, occReturn);
                };
                break;
            case "belief":
                timeFunction = (Term derived, @NotNull Derivation p, @NotNull long[] occReturn, @NotNull float[] confScale) -> {
                    long occ;
                    boolean temporal = false;
                    if (!p.task.isEternal()) {
                        temporal = true;
                        //task determines occurrence if non-eternal
                        occ = p.task.start();
                        if (z != null) {
                            //if Z occurrs in task, then there is an additional shift
                            @Nullable Term zr = p.transform(z).eval(p.terms);
                            if (zr == null)
                                return null;
                            int relOccShift = p.beliefTerm.subtermTime(zr);
                            if (relOccShift != DTERNAL)
                                occ -= relOccShift;
                        }
                    } else {
                        if (p.belief != null) {
                            if ((occ = p.belief.start()) != ETERNAL)
                                temporal = true;
                        } else {
                            occ = ETERNAL;
                        }
                    }

                    if (occ != ETERNAL) {
                        occReturn[0] = occ;
                        @Nullable Term yr = p.transform(y).eval(p.terms);
                        if (yr == null)
                            return null;
                        int occShift = p.beliefTerm.subtermTime(yr);
                        if (occShift != DTERNAL)
                            occReturn[0] += occShift - yr.dtRange();
                    } else {
                        if (!temporal) {
                            return derived;
                        } else {
                            return null; //uncomputable temporal basis
                        }
                    }

                    TimeFunctions.shiftIfImmediate(p, occReturn, derived);

                    return filterEternalBasis(derived, p, occReturn);
                };
                break;

//                        case "after":
//                            pres.add(events.after);
//                            break;
//
//                        case "eternal":
//                            pres.add(events.eternal);
//                            timeFunction = TimeFunctions.dternal;
//                            break;
//
//                        case "afterOrEternal":
//                            pres.add(events.afterOrEternal);
//                            break;
            /*case "taskPredicate":
                pres.add( events.taskPredicate;
                break;*/
//                        case "dt":
//                            timeFunction = TimeFunctions.occForward;
//                            break;


//                        case "dtBelief":
//                            timeFunction = TimeFunctions.dtBelief;
//                            break;
//                        case "dtBeliefEnd":
//                            timeFunction = TimeFunctions.dtBeliefEnd;
//                            break;
            case "dtBeliefExact":
                timeFunction = TimeFunctions.dtBeliefExact;
                break;
            case "dtBeliefReverse":
                timeFunction = TimeFunctions.dtBeliefExact;
                break;
//                        case "dtTask":
//                            timeFunction = TimeFunctions.dtTask;
//                            break;
//                        case "dtTaskEnd":
//                            timeFunction = TimeFunctions.dtTaskEnd;
//                            break;
            case "dtTaskExact":
                timeFunction = TimeFunctions.dtTaskExact;
                break;

            case "decomposeTask":
                timeFunction = TimeFunctions.decomposeTask;
                break;
//                        case "decomposeTaskIfTemporal":
//                            pres.add(events.taskNotDTernal);
//                            timeFunction = TimeFunctions.decomposeTask;
//                            break;

            case "decomposeTaskSubset":
                timeFunction = TimeFunctions.decomposeTaskSubset;
                break;
            case "decomposeTaskComponents":
                timeFunction = TimeFunctions.decomposeTaskComponents;
                break;

            case "beliefDTSimultaneous":
                pres.add(TaskBeliefOccurrence.beliefDTSimultaneous);
                break;

//                        case "decomposeTaskIfTermLinkBefore":
//                            timeFunction = TimeFunctions.decomposeTask;
//                            pres.add(IfTermLinkBefore.ifTermLinkBefore);
//                            break;
//
//                        case "decomposeTaskIfBeliefBefore":
//                            timeFunction = TimeFunctions.decomposeTask;
//                            pres.add(IfTermLinkBefore.ifBeliefBefore);
//                            break;

            case "decomposeBelief":
                timeFunction = TimeFunctions.decomposeBelief;
                break;
            case "decomposeBeliefLate":
                timeFunction = TimeFunctions.decomposeBeliefLate;
                break;


//                        case "dtForward":
//                            timeFunction = TimeFunctions.occForward;
//                            pres.add(events.bothTemporal);
//                            break;

//                        case "conjoin":
//                            timeFunction = TimeFunctions.occForwardMerge;
//                            pres.add(events.nonEternal);
//                            break;


//                        case "dtAfter":
//                            timeFunction = TimeFunctions.occForward;
//                            pres.add(events.nonEternal);
//                            break;
//                        case "dtAfterReverse":
//                            timeFunction = TimeFunctions.occReverse;
//                            pres.add(events.nonEternal);
//                            break;

            case "raw":
                beliefProjected = false;
                break;

//                        case "dtBefore":
//                            timeFunction = TimeFunctions.occReverse;
//                            pres.add(events.after);
//                            break;

//                        case "dtIfEvent":
//                            temporalize = Temporalize.dtIfEvent;
//                            break;


            case "dtCombine":
                timeFunction = TimeFunctions.dtCombine;
                pres.add(TaskBeliefOccurrence.eventsOrEternals);
                break;
            case "dtCombinePre":
                timeFunction = TimeFunctions.dtCombinePre;
                break;
            case "dtCombinePost":
                timeFunction = TimeFunctions.dtCombinePost;
                break;

            case "dtEvents":
                timeFunction = TimeFunctions.occForward;
                pres.add(TaskBeliefOccurrence.bothEvents);
                minNAL = 7;
                break;
            case "dtEventsReverse":
                timeFunction = TimeFunctions.occReverse;
                pres.add(TaskBeliefOccurrence.bothEvents);
                minNAL = 7;
                break;
            //NOTE THIS SHOULD ACTUALLY BE CALLED dtBeforeAfterOrEternal or something
            case "dtEventsOrEternals":
                timeFunction = TimeFunctions.occForward;
                pres.add(TaskBeliefOccurrence.eventsOrEternals);
                break;
            case "dtEventsOrEternalsReverse":
                timeFunction = TimeFunctions.occReverse;
                pres.add(TaskBeliefOccurrence.eventsOrEternals);
                break;

            case "dtTminB":
                timeFunction = TimeFunctions.dtTminB;
                break;
            case "dtBminT":
                timeFunction = TimeFunctions.dtBminT;
                break;
//                        case "dtIntersect":
//                            timeFunction = TimeFunctions.dtIntersect;
//                            break;

            case "dtSum":
                timeFunction = TimeFunctions.dtSum;
                break;
            case "dtSumReverse":
                timeFunction = TimeFunctions.dtSumReverse;
                break;
            //                        case "occMerge":
//                            timeFunction = TimeFunctions.occMerge;
//                            break;

            default:
                throw new RuntimeException("invalid events parameters");
        }
        return timeFunction;
    }

    public @Nullable Term filterEternalBasis(Term derived, @NotNull Derivation p, @NotNull long[] occReturn) {
        if (derived == null)
            return null;

        if (occReturn[0] == ETERNAL && (!p.task.isEternal() && (p.belief == null || !p.belief.isEternal())))
            return null; //no temporal basis

        return derived;
    }

    public static void opNot(Term task, Term belief, @NotNull Set<PrediTerm> pres, @NotNull SortedSet<MatchConstraint> constraints, @NotNull Term t, int structure) {

//        boolean prefiltered = false;
//        if (t.equals(task)) {
//            pres.add(new PatternOpNot(0, structure));
//            prefiltered = true;
//        } else if (t.equals(belief)) {
//            pres.add(new PatternOpNot(1, structure));
//            prefiltered = true;
//        }
//
//        if (!prefiltered)
        constraints.add(new OpExclusionConstraint(t, structure));
    }


    public static void opNotContaining(Term task, Term belief, @NotNull Set<PrediTerm> pres, @NotNull SortedSet<MatchConstraint> constraints, @NotNull Term t, int structure) {


        boolean prefiltered = false;
//
//        if (t.equals(task)) {
//            pres.add(new PatternOpNotContaining(0, structure));
//            prefiltered = true;
//        } else if (t.equals(belief)) {
//            pres.add(new PatternOpNotContaining(1, structure));
//            prefiltered = true;
//        }

        //if (!prefiltered)
        constraints.add(new StructureExclusionConstraint(t, structure));
    }

    public void neq(@NotNull SortedSet<MatchConstraint> constraints, @NotNull Term x, @NotNull Term y) {
        constraints.add(new NotEqualConstraint(x, y));
        constraints.add(new NotEqualConstraint(y, x));
    }


//    /**
//     * returns whether the prefilter was successful, otherwise a constraint must be tested
//     */
//    public boolean neqPrefilter(@NotNull Collection<BoolPredicate> pres, @NotNull Term task, @NotNull Term belief, @NotNull Term arg1, @NotNull Term arg2, Function<TaskBeliefSubterms, BoolPredicate[]> filter) {
//        TaskBeliefSubterms tb = withinNonCommutive(task, belief, arg1, arg2);
//        if (tb != null) {
//            //cheaper to compute this in precondition
//            BoolPredicate[] bp = filter.apply(tb);
//            for (BoolPredicate b : bp)
//                pres.add(b);
//            return true;
//        }
//
//        return false;
//    }


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


//    static final Term BELIEF = $.the("Belief");
//    static final Term DESIRE = $.the("Desire");

    //    public PremiseRule negative(PatternIndex index) {
//
//        @NotNull Term tt = getTask();
////        if (tt.op() == Op.ATOM) {
////            //raw pattern var, no need to invert the rule
////            //System.err.println("--- " + tt);
////            //return null;
////        } else {
////            //System.err.println("NEG " + neg(tt));
////        }
//
//        Compound newTask = (Compound) neg(tt);
//        Term[] pp = getPremise().terms().clone();
//        pp[0] = newTask;
//
//
//        Map<Term,Term> negMap = Maps.mutable.of(tt, newTask);
//        Term prevTask = pp[1];
//        pp[1] = $.terms.remap(prevTask, negMap);
//
//
//        Compound newPremise = (Compound) $.compound(getPremise().op(), pp);
//
//        @NotNull Compound prevConclusion = getConclusion();
//        Compound newConclusion = (Compound) $.terms.remap(prevConclusion, negMap);
//
//        //only the task was affected
//        if (newConclusion.equals(prevConclusion) && newTask.equals(prevTask))
//            return null;
//
//        //Compound newConclusion = (Compound) terms.transform(getConclusion(), truthNegate);
//
//        @NotNull PremiseRule neg = PremiseRuleSet.normalize(new PremiseRule(newPremise, newConclusion), index);
//
//        //System.err.println(term(0) + " |- " + term(1) + "  " + "\t\t" + remapped);
//
//        return neg;
//    }

//    /**
//     * safe negation
//     */
//    @NotNull
//    private static Term neg(@NotNull Term x) {
//        if (x.op() == NEG) {
//            return ((Compound) x).term(0); //unwrap
//        } else {
//            //do this manually for premise rules since they will need to negate atoms which is not usually allowed
//            return new GenericCompound(NEG, TermVector.the(x));
//        }
//    }


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


    public static final class PremiseRuleVariableNormalization extends VariableNormalization {


        public static final int ELLIPSIS_ZERO_OR_MORE_ID_OFFSET = 1 * 256;
        public static final int ELLIPSIS_ONE_OR_MORE_ID_OFFSET = 2 * 256;
        public static final int ELLIPSIS_TRANSFORM_ID_OFFSET = 3 * 256;

        public PremiseRuleVariableNormalization() {
            super(new UnifiedMap<>());
        }

        public static AbstractVariable varPattern(int i) {
            return v(VAR_PATTERN, i);
        }

        @NotNull
        @Override
        protected Variable newVariable(@NotNull Variable x) {


            int actualSerial = count;

//            if (x instanceof Ellipsis.EllipsisTransformPrototype) {
//                //special
//
//                Ellipsis.EllipsisTransformPrototype ep = (Ellipsis.EllipsisTransformPrototype) x;
//
////                Term from = ep.from;
////                if (from != Op.Imdex) from = applyAfter((GenericVariable)from);
////                Term to = ep.to;
////                if (to != Op.Imdex) to = applyAfter((GenericVariable)to);
////
//                return EllipsisTransform.make(varPattern(actualSerial + ELLIPSIS_TRANSFORM_ID_OFFSET), ep.from, ep.to, this);
//
//            } else
            if (x instanceof Ellipsis.EllipsisPrototype) {
                Ellipsis.EllipsisPrototype ep = (Ellipsis.EllipsisPrototype) x;
                return Ellipsis.EllipsisPrototype.make(actualSerial +
                                (ep.minArity == 0 ? ELLIPSIS_ZERO_OR_MORE_ID_OFFSET : ELLIPSIS_ONE_OR_MORE_ID_OFFSET) //these need to be distinct
                        , ep.minArity);
            } else if (x instanceof Ellipsis) {

                return x;

                //throw new UnsupportedOperationException("?");
//                int idOffset;
//                if (v instanceof EllipsisTransform) {
//                    idOffset = ELLIPSIS_TRANSFORM_ID_OFFSET;
//                } else if (v instanceof EllipsisZeroOrMore) {
//                    idOffset = ELLIPSIS_ZERO_OR_MORE_ID_OFFSET;
//                } else if (v instanceof EllipsisOneOrMore) {
//                    idOffset = ELLIPSIS_ONE_OR_MORE_ID_OFFSET;
//                } else {
//                    throw new RuntimeException("N/A");
//                }
//
//                Variable r = ((Ellipsis) v).clone(varPattern(actualSerial + idOffset), this);
//                offset = 0; //return to zero
//                return r;
            } /*else if (v instanceof GenericVariable) {
                return ((GenericVariable) v).normalize(actualSerial); //HACK
            } else {
                return v(v.op(), actualSerial);
            }*/
            return super.newVariable(x);
        }

//        @Override
//        public final boolean testSuperTerm(@NotNull Compound t) {
//            //descend all, because VAR_PATTERN is not yet always considered a variable
//            return true;
//        }

        @NotNull
        public Term applyAfter(@NotNull Variable secondary) {
            if (secondary.equals(Op.Imdex))
                return secondary; //dont try to normalize any imdex
            else
                return apply(null, secondary);
        }
    }


}




