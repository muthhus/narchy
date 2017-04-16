package nars;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.fge.grappa.matchers.MatcherType;
import com.github.fge.grappa.matchers.base.AbstractMatcher;
import com.github.fge.grappa.matchers.base.Matcher;
import com.github.fge.grappa.parsers.BaseParser;
import com.github.fge.grappa.rules.Rule;
import com.github.fge.grappa.run.ParseRunner;
import com.github.fge.grappa.run.ParsingResult;
import com.github.fge.grappa.run.context.MatcherContext;
import com.github.fge.grappa.stack.ArrayValueStack;
import com.github.fge.grappa.stack.ValueStack;
import com.github.fge.grappa.support.Var;
import com.github.fge.grappa.transform.ParserTransformer;
import com.google.common.collect.ImmutableList;
import jcog.Texts;
import nars.budget.BudgetFunctions;
import nars.derive.meta.match.Ellipsis;
import nars.index.TermBuilder;
import nars.index.term.TermIndex;
import nars.task.TaskBuilder;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Terms;
import nars.term.atom.AtomicSingleton;
import nars.term.var.GenericVariable;
import nars.term.var.Variable;
import nars.time.Tense;
import nars.truth.DefaultTruth;
import nars.truth.Truth;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static nars.Op.*;

/**
 * NARese, syntax and language for interacting with a NAR in NARS.
 * https://code.google.com/p/open-nars/wiki/InputOutputFormat
 */
public class Narsese extends BaseParser<Object> {


    public static final String NARSESE_TASK_TAG = "Narsese";

    static class MyParseRunner extends ParseRunner {

        /**
         * Constructor
         *
         * @param rule the rule
         */
        public MyParseRunner(Rule rule) {
            super(rule);
        }

        @Override
        public boolean match(MatcherContext context) {
            final Matcher matcher = context.getMatcher();

            //final PreMatchEvent<T> preMatchEvent = new PreMatchEvent<>(context);
            //bus.post(preMatchEvent);

//            if (throwable != null)
//                throw new GrappaException("parsing listener error (before match)",
//                        throwable);

            // FIXME: is there any case at all where context.getMatcher() is null?
            @SuppressWarnings("ConstantConditions") final boolean match = matcher.match(context);
//
//            final MatchContextEvent<T> postMatchEvent = match
//                    ? new MatchSuccessEvent<>(context)
//                    : new MatchFailureEvent<>(context);

            //bus.post(postMatchEvent);

//            if (throwable != null)
//                throw new GrappaException("parsing listener error (after match)",
//                        throwable);

            return match;
        }

    }

    //These should be set to something like RecoveringParseRunner for performance
    private final ParseRunner inputParser = new MyParseRunner(Input());
    private final ParseRunner singleTaskParser = new MyParseRunner(Task());
    private final ParseRunner singleTermParser = new MyParseRunner(Term());
    //private final ParseRunner singleTaskRuleParser = new ListeningParseRunner3(TaskRule());

    //private final Map<String,Term> termCache = new HashMap();

    static final Class parser;

    static {
        Class p;
        try {
            p = ParserTransformer.extendParserClass(Narsese.class).getExtendedClass();
        } catch (Exception e) {
            e.printStackTrace();
            p = null;
        }
        parser = p;
    }

    static final ThreadLocal<Narsese> parsers = ThreadLocal.withInitial(
            //() -> Grappa.createParser(Narsese.class)
            () -> {
                try {
                    return (Narsese) parser.newInstance();
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
    );

//    static final ThreadLocal<Map<Pair<Op, List>, Term>> vectorTerms = ThreadLocal.withInitial(() ->
//            new CapacityLinkedHashMap<Pair<Op, List>, Term>(512));


    public TermBuilder T = $.terms; //HACK

    @NotNull
    public static Narsese the() {
        return parsers.get();
    }

    public static float quality(NAR nar, byte p, @Nullable Truth t) {
        return t != null ? BudgetFunctions.truthToQuality(t) : nar.qualityDefault(p);
    }

    public static boolean isPunctuation(char c) {
        switch (c) {
            case BELIEF:
            case GOAL:
            case QUEST:
            case QUESTION:
                return true;
        }
        return false;
    }


    public Rule Input() {
        return sequence(
                zeroOrMore( //1 or more?
                        //sequence(
                        //firstOf(
                        //LineComment(),
                        s(),
                        Task()
                        //)

                        //)
                ), s(), eof());
    }

//    /**
//     * {Premise1,Premise2} |- Conclusion.
//     */
//    public Rule TaskRule() {
//
//        //use a var to count how many rule conditions so that they can be pulled off the stack without reallocating an arraylist
//        return sequence(
//                STATEMENT_OPENER, s(),
//                push(PremiseRule.class),
//
//                Term(), //cause
//
//                zeroOrMore(sepArgSep(), Term()),
//                s(), TASK_RULE_FWD, s(),
//
//                push(PremiseRule.class), //stack marker
//
//                Term(), //effect
//
//                zeroOrMore(sepArgSep(), Term()),
//                s(), STATEMENT_CLOSER, s(),
//
//                eof(),
//
//                push(popTaskRule())
//        );
//    }


//    @Nullable
//    public PremiseRule popTaskRule() {
//        //(Term)pop(), (Term)pop()
//
//        List<Term> r = $.newArrayList(16);
//        List<Term> l = $.newArrayList(16);
//
//        Object popped;
//        while ((popped = pop()) != PremiseRule.class) { //lets go back till to the start now
//            r.add(the(popped));
//        }
//        if (r.isEmpty()) //empty premise list is invalid
//            return null;
//
//        while ((popped = pop()) != PremiseRule.class) {
//            l.add(the(popped));
//        }
//        if (l.isEmpty()) //empty premise list is invalid
//            return null;
//
//
//        Collections.reverse(l);
//        Collections.reverse(r);
//
//        Compound premise = $.p(l);
//        Compound conclusion = $.p(r);
//
//        return new PremiseRule(premise, conclusion);
//    }

    public Rule LineComment() {
        return sequence(
                s(),
                //firstOf(
                "//",
                //"'",
                //sequence("***", zeroOrMore('*')), //temporary
                //"OUT:"
                //),
                //sNonNewLine(),
                //LineCommentEchoed(),
                firstOf("\n", eof() /* may not have newline at end of file */)
        );
    }

//    public Rule LineCommentEchoed() {
//        //return Atom.the(Utf8.toUtf8(name));
//
//        //return $.the('"' + t + '"');
//
////        int olen = name.length();
////        switch (olen) {
////            case 0:
////                throw new RuntimeException("empty atom name: " + name);
////
//////            //re-use short term names
//////            case 1:
//////            case 2:
//////                return theCached(name);
////
////            default:
////                if (olen > Short.MAX_VALUE/2)
////                    throw new RuntimeException("atom name too long");
//
//        //  }
//        return sequence(
//                zeroOrMore(noneOf("\n")),
//                push(ImmediateOperator.command(echo.class, $.quote(match())))
//        );
//    }

//    public Rule PauseInput() {
//        return sequence( s(), IntegerNonNegative(),
//                push( PauseInput.pause( (Integer) pop() ) ), sNonNewLine(),
//                "\n" );
//    }


//    public Rule TermEOF() {
//        return sequence( s(), Term(), s(), eof() );
//    }
//    public Rule TaskEOF() {
//        return sequence( s(), Task(), s(), eof() );
//    }

    public Rule Task() {

        Var<Float> budget = new Var();
        Var<Character> punc = new Var(Op.COMMAND);
        Var<Term> term = new Var();
        Var<Truth> truth = new Var();
        Var<Tense> tense = new Var(Tense.Eternal);

        return sequence(
                s(),

                optional(Budget(budget)),

                Term(true, false),
                term.set(the(pop())),

                s(),

                optional(SentencePunctuation(punc), s()),

                optional(Tense(tense), s()),

                optional(Truth(truth, tense), s()),

                push(new Object[]{budget.get(), term.get(), punc.get(), truth.get(), tense.get()})
                //push(getTask(budget, term, punc, truth, tense))

        );
    }


    public Rule Budget(Var<Float> budget) {
        return sequence(
                BUDGET_VALUE_MARK,

                ShortFloat(),

//                firstOf(
//                        BudgetPriorityDurabilityQuality(budget),
//                        BudgetPriorityDurability(budget),
                        BudgetPriority(budget),
//                ),

                optional(BUDGET_VALUE_MARK)
        );
    }

    boolean BudgetPriority(Var<Float> budget) {
        return budget.set((Float)(pop()));
    }

//    public Rule BudgetPriorityDurability(Var<float[]> budget) {
//        return sequence(
//                VALUE_SEPARATOR, ShortFloat(),
//                budget.set(new float[]{(float) pop(), (float) pop()}) //intermediate representation
//        );
//    }

//    public Rule BudgetPriorityDurabilityQuality(Var<float[]> budget) {
//        return sequence(
//                VALUE_SEPARATOR, ShortFloat(), VALUE_SEPARATOR, ShortFloat(),
//                budget.set(new float[]{(float) pop(), (float) pop(), (float) pop()}) //intermediate representation
//        );
//    }

    public Rule Tense(Var<Tense> tense) {
        return firstOf(
                sequence(TENSE_PRESENT, tense.set(Tense.Present)),
                sequence(TENSE_PAST, tense.set(Tense.Past)),
                sequence(TENSE_FUTURE, tense.set(Tense.Future))
        );
    }

    public Rule Truth(Var<Truth> truth, Var<Tense> tense) {
        return sequence(

                TRUTH_VALUE_MARK,

                ShortFloat(), //Frequency

                //firstOf(

                sequence(

                        TruthTenseSeparator(VALUE_SEPARATOR, tense), // separating ;,|,/,\

                        ShortFloat(), //Conf

                        optional(TRUTH_VALUE_MARK), //tailing '%' is optional

                        swap() && truth.set(new DefaultTruth((float) pop(), (float) pop()))
                )
                        /*,

                        sequence(
                                TRUTH_VALUE_MARK, //tailing '%'

                                truth.set(new DefaultTruth((float) pop() ))
                        )*/
                //)
        );
    }

    Rule TruthTenseSeparator(char defaultChar, Var<Tense> tense) {
        return firstOf(
                defaultChar,
                sequence('|', tense.set(Tense.Present)),
                sequence('\\', tense.set(Tense.Past)),
                sequence('/', tense.set(Tense.Future))
        );
    }


    Rule ShortFloat() {
        return sequence(
                sequence(
                        optional(digit()),
                        optional('.', oneOrMore(digit()))
                ),
                push(Texts.f(matchOrDefault("NaN"), 0, 1.0f))
        );
    }


//    Rule IntegerNonNegative() {
//        return sequence(
//                oneOrMore(digit()),
//                push(Integer.parseInt(matchOrDefault("NaN")))
//        );
//    }

//    Rule Number() {
//
//        return sequence(
//                sequence(
//                        optional('-'),
//                        oneOrMore(digit()),
//                        optional('.', oneOrMore(digit()))
//                ),
//                push(Float.parseFloat(matchOrDefault("NaN")))
//        );
//    }

    Rule SentencePunctuation(Var<Character> punc) {
        return sequence(anyOf(".?!@;"), punc.set(matchedChar()));
    }


    public Rule Term() {
        return Term(true, true);
    }

//    Rule nothing() {
//        return new NothingMatcher();
//    }


    @NotNull
    protected Object nonNull(@Nullable Object o) {
        return o != null ? o : new MiniNullPointerException();
    }

    //@Cached
    Rule Term(boolean oper, boolean meta) {
        /*
                 <term> ::= <word>                             // an atomic constant term
                        | <variable>                         // an atomic variable term
                        | <compound-term>                    // a term with internal structure
                        | <statement>                        // a statement can serve as a term
        */

        return seq(
                s(),
                firstOf(
                        QuotedAtom(),

                        seq(oper, ColonReverseInheritance()),

                        seq(meta, Ellipsis()),
                        Variable(),

                        NumberAtom(),


                        seq(SETe.str,

                                MultiArgTerm(SETe, SET_EXT_CLOSER, false, false)

                        ),

                        seq(SETi.str,

                                MultiArgTerm(SETi, SET_INT_CLOSER, false, false)

                        ),

                        TemporalRelation(),

                        //Functional form of an Operation, ex: operate(p1,p2), TODO move to FunctionalOperationTerm() rule
                        seq(oper,

                                Atom(),
                                //Term(false, false), //<-- allows non-atom terms for operator names
                                //Atom(), //push(nonNull($.oper((String)pop()))), // <-- allows only atoms for operator names, normal

                                push($.the(pop())),

                                COMPOUND_TERM_OPENER, s(),

                                firstOf(
                                        seq(COMPOUND_TERM_CLOSER, push(Terms.ZeroProduct)),// nonNull($.exec((Term)pop())) )),
                                        MultiArgTerm(PROD, COMPOUND_TERM_CLOSER, false, false)
                                ),

                                push(T.the(INH, (Term) pop(), (Term) pop()))

                        ),


                        seq(COMPOUND_TERM_OPENER, s(),
                                firstOf(

                                        sequence(
                                                COMPOUND_TERM_CLOSER, push(Terms.ZeroProduct)
                                        ),


                                        MultiArgTerm(null, COMPOUND_TERM_CLOSER, true, false),

                                        //default to product if no operator specified in ( )
                                        MultiArgTerm(null, COMPOUND_TERM_CLOSER, false, false),

                                        MultiArgTerm(null, COMPOUND_TERM_CLOSER, false, true),

                                        ConjunctionParallel()

                                )

                        ),

                        //deprecated form: <a --> b>
                        seq(STATEMENT_OPENER,
                                MultiArgTerm(null, STATEMENT_CLOSER, false, true)
                        ),

                        //negation shorthand
                        seq(NEG.str, s(), Term(), push(
                                //Negation.make(popTerm(null, true)))),
                                T.neg( /*$.$(*/ (Term) pop()))),


                        Atom()

                ),

                //ATOM
                push((pop())),

                s()
        );
    }

    public Rule seq(Object rule, Object rule2,
                    Object... moreRules) {
        return sequence(rule, rule2, moreRules);
    }


    public Rule ConjunctionParallel() {
        return seq(

                "&|", sepArgSep(),

                Term(true, false),
                s(),
                oneOrMore(sequence(

                        sepArgSep(),

                        Term(true, false)
                )),
                s(),
                COMPOUND_TERM_CLOSER,

                push(T.the(CONJ, 0, ((Compound) popTerm(CONJ)).subterms()) /* HACK construct a dt=0 copy */)
        );
    }

    @Deprecated
    public Rule TemporalRelation() {
        return seq(

                COMPOUND_TERM_OPENER,
                s(),
                Term(true, false),
                s(),
                firstOf(
                        seq(OpTemporal(), CycleDelta()),
                        seq(OpTemporalParallel(), push(0) /* dt=0 */)
                ),
                s(),
                Term(true, false),
                s(),
                COMPOUND_TERM_CLOSER,


                push(TemporalRelationBuilder(the(pop()) /* pred */,
                        (Integer) pop() /*cycleDelta*/, (Op) pop() /*relation*/, the(pop()) /* subj */))
        );
    }

    @Nullable
    Term TemporalRelationBuilder(Term pred, int cycles, Op o, Term subj) {
        return T.the(o, cycles, subj, pred);
    }

    public final static String invalidCycleDeltaString = Integer.toString(Integer.MIN_VALUE);

    public Rule CycleDelta() {
        return
                firstOf(
                        seq("+-", push(Tense.XTERNAL)),
                        seq('+', oneOrMore(digit()),
                                push(Integer.parseInt(matchOrDefault(invalidCycleDeltaString)))
                        ),
                        seq('-', oneOrMore(digit()),
                                push(-Integer.parseInt(matchOrDefault(invalidCycleDeltaString)))
                        )
                )
                ;
    }

//    public Rule Operator() {
//        return sequence(OPER.ch,
//                Atom(), push($.oper((String)pop())));
//                //Term(false, false),
//                //push($.operator(pop().toString())));
//    }


    /**
     * an atomic term, returns a String because the result may be used as a Variable name
     */
    Rule Atom() {
        return seq(
                ValidAtomCharMatcher.the,
                push(match())
        );
    }

    Rule NumberAtom() {
        return seq(

                seq(
                        optional('-'),
                        oneOrMore(digit()),
                        optional('.', oneOrMore(digit()))
                ),

                push($.the(Float.parseFloat(matchOrDefault("NaN"))))
        );
    }


    static final class ValidAtomCharMatcher extends AbstractMatcher {

        public static final ValidAtomCharMatcher the = new ValidAtomCharMatcher();

        protected ValidAtomCharMatcher() {
            super("'ValidAtomChar'");
        }

        @NotNull
        @Override
        public MatcherType getType() {
            return MatcherType.TERMINAL;
        }

        @Override
        public <V> boolean match(MatcherContext<V> context) {
            int count = 0;
            int max = context.getInputBuffer().length() - context.getCurrentIndex();

            while (count < max && isValidAtomChar(context.getCurrentChar())) {
                context.advanceIndex(1);
                count++;
            }

            return count > 0;
        }
    }

    public static boolean isValidAtomChar(char x) {

        //TODO replace these with Symbols. constants
        switch (x) {
            case ' ':
            case ARGUMENT_SEPARATOR:
            case BELIEF:
            case GOAL:
            case QUESTION:
            case QUEST:
            case '\"':

            case '^':

            case '<':
            case '>':

            case '~':
            case '=':

            case '+':
            case '-':
            case '*':

            case '|':
            case '&':
            case '(':
            case ')':
            case '[':
            case ']':
            case '{':
            case '}':
            case '%':
            case '#':
            case '$':
            case ':':
            case ';':
            case '`':

            case '\'':
            case '\t':
            case '\n':
                return false;
        }
        return true;
    }


    /**
     * MACRO: y:x    becomes    <x --> y>
     */
    Rule ColonReverseInheritance() {
        return sequence(
                Term(false, true), ':', Term(),
                push(T.the(INH, the(pop()), the(pop())))
        );
    }


    Rule QuotedAtom() {
        return sequence(
                dquote(), //leading quote
                firstOf(
                        //multi-line TRIPLE quotes
                        seq(regex("\"\"[\\s\\S]+\"\"\""), push($.the('\"' + match()))),

                        //one quote
                        seq(
                                //regex("[\\s\\S]+\""),
                                regex("(?:[^\"\\\\]|\\\\.)*\""),
                                push($.the('\"' + match())))
                )
        );
    }


    Rule Ellipsis() {
        return sequence(
                Variable(), "..",
                firstOf(

                        seq("_=", Term(false, false), "..+",
                                swap(2),
                                push(new Ellipsis.EllipsisTransformPrototype(/*Op.VAR_PATTERN,*/
                                        (Variable) pop(), Op.Imdex, (Term) pop()))
                        ),
                        seq(Term(false, false), "=_..+",
                                swap(2),
                                push(new Ellipsis.EllipsisTransformPrototype(/*Op.VAR_PATTERN,*/
                                        (Variable) pop(), (Term) pop(), Op.Imdex))
                        ),
                        seq("+",
                                push(new Ellipsis.EllipsisPrototype(Op.VAR_PATTERN, (GenericVariable) pop(), 1))
                        ),
                        seq("*",
                                push(new Ellipsis.EllipsisPrototype(Op.VAR_PATTERN, (GenericVariable) pop(), 0))
                        )
                )
        );
    }

    Rule AnyStringExceptQuote() {
        //TODO handle \" escape
        return zeroOrMore(noneOf("\""));
    }


    Rule AnyString() {
        return zeroOrMore(ANY);
    }


    Rule Variable() {
        /*
           <variable> ::= "$"<word>                          // independent variable
                        | "#"[<word>]                        // dependent variable
                        | "?"[<word>]                        // query variable in question
                        | "%"[<word>]                        // pattern variable in rule
        */
        return sequence(
                anyOf(new char[]{
                        Op.VAR_INDEP.ch,
                        Op.VAR_DEP.ch,
                        Op.VAR_QUERY.ch,
                        Op.VAR_PATTERN.ch
                }),
                push(match()),
                Atom(),
                swap(),
                push($.v(((String) pop()).charAt(0), (String) pop()))
        );
    }

    //Rule CompoundTerm() {
        /*
         <compound-term> ::= "{" <term> {","<term>} "}"         // extensional set
                        | "[" <term> {","<term>} "]"         // intensional set
                        | "(&," <term> {","<term>} ")"       // extensional intersection
                        | "(|," <term> {","<term>} ")"       // intensional intersection
                        | "(*," <term> {","<term>} ")"       // product
                        | "(/," <term> {","<term>} ")"       // extensional image
                        | "(\," <term> {","<term>} ")"       // intensional image
                        | "(||," <term> {","<term>} ")"      // disjunction
                        | "(&&," <term> {","<term>} ")"      // conjunction
                        | "(&/," <term> {","<term>} ")"      // (sequential events)
                        | "(&|," <term> {","<term>} ")"      // (parallel events)
                        | "(--," <term> ")"                  // negation
                        | "(-," <term> "," <term> ")"        // extensional difference
                        | "(~," <term> "," <term> ")"        // intensional difference
        
        */

    //}

    Rule Op() {
        return sequence(
                trie(
                        SECTe.str, SECTi.str,
                        DIFFe.str, DIFFi.str,
                        PROD.str,
                        IMGe.str, IMGi.str,

                        INH.str,

                        SIM.str,


                        NEG.str,

                        IMPL.str,

                        EQUI.str,

                        CONJ.str,

                        //TODO make these special case macros
                        DISJ.str,
                        PROPERTY.str,
                        INSTANCE.str,
                        INSTANCE_PROPERTY.str

                ),

                push(Op.fromString(match()))
        );
    }

    Rule OpTemporal() {
        return sequence(
                trie(
                        IMPL.str,
                        EQUI.str,
                        CONJ.str
                ),
                push(Op.fromString(match()))
        );
    }

    Rule OpTemporalParallel() {
        return firstOf(
                seq("<|>", push(EQUI)),
                seq("=|>", push(IMPL)),
                seq("&|", push(CONJ))
        );
    }

    Rule sepArgSep() {
        return firstOf(
                        seq(s(), optional(ARGUMENT_SEPARATOR), s()),
                        ss()
                );
    }


    static final Object functionalForm = new Object();

    /**
     * list of terms prefixed by a particular compound term operate
     */
    //@Cached
    Rule MultiArgTerm(@Nullable Op defaultOp, char close, boolean initialOp, boolean allowInternalOp) {

        return sequence(

                /*operatorPrecedes ? *OperationPrefixTerm()* true :*/

//                operatorPrecedes ?
//                        push(new Object[]{pop(), functionalForm})
//                        :
                push(Compound.class),

                initialOp ? Op() : Term(),

                allowInternalOp ?

                        sequence(s(), Op(), s(), Term())

                        :

                        zeroOrMore(sequence(
                                sepArgSep(),
                                allowInternalOp ? AnyOperatorOrTerm() : Term()
                        )),

                s(),

                close,

                push(popTerm(defaultOp))
        );
    }

//    /**
//     * operation()
//     */
//    Rule EmptyOperationParens() {
//        return sequence(
//
//                OperationPrefixTerm(),
//
//                /*s(),*/ COMPOUND_TERM_OPENER, s(), COMPOUND_TERM_CLOSER,
//
//                push(popTerm(OPERATOR, false))
//        );
//    }

    Rule AnyOperatorOrTerm() {
        return firstOf(Op(), Term());
    }


    @Nullable
    static Term the(@Nullable Object o) {
        if (o instanceof Term) return (Term) o;
        if (o == null) return null; //pass through
        if (o instanceof String) {
            String s = (String) o;
            //return s;
            return $.the(s);

//        int olen = name.length();
//        switch (olen) {
//            case 0:
//                throw new RuntimeException("empty atom name: " + name);
//
////            //re-use short term names
////            case 1:
////            case 2:
////                return theCached(name);
//
//            default:
//                if (olen > Short.MAX_VALUE/2)
//                    throw new RuntimeException("atom name too long");

            //  }
        }
        throw new RuntimeException(o + " is not a term");
    }

    /**
     * produce a term from the terms (& <=1 NALOperator's) on the value stack
     */
    @Nullable
    @Deprecated
    final Term popTerm(Op op /*default */) {

        //System.err.println(getContext().getValueStack());

        ArrayValueStack<Object> stack = (ArrayValueStack) getContext().getValueStack();

//        if (stack.isEmpty())
//            return null;

        List vectorterms = $.newArrayList(stack.size());

        while (!stack.isEmpty()) {
            Object p = pop();

            if (p instanceof Object[]) {
                //it's an array so unpack by pushing everything back onto the stack except the last item which will be used as normal below
                Object[] pp = (Object[]) p;
                if (pp.length > 1) {
                    for (int i = pp.length - 1; i >= 1; i--) {
                        stack.push(pp[i]);
                    }
                }

                p = pp[0];
            }


            if (p == functionalForm) {
                op = ATOM;
                break;
            }

            if (p == Compound.class) break; //beginning of stack frame for this term


            if (p instanceof String) {
                //throw new RuntimeException("string not expected here");
                //Term t = $.the((String) p);
                vectorterms.add($.the((String) p));
            } else if (p instanceof Term) {
                vectorterms.add(p);
            } else if (p instanceof Op) {

//                if (op != null) {
//                    //if ((!allowInternalOp) && (!p.equals(op)))
//                    //throw new RuntimeException("Internal operator " + p + " not allowed here; default op=" + op);
//
//                    throw new NarseseException("Too many operators involved: " + op + ',' + p + " in " + stack + ':' + vectorterms);
//                }

                op = (Op) p;
            }
        }

        Collections.reverse(vectorterms);

        if (op == null)
            op = PROD;

        //return $.compound(op, vectorterms);
        Term x = termCache.get(Tuples.pair(op, ImmutableList.copyOf(vectorterms)), termBuilder);
        if (x == NarseseNull)
            return null;
        return x;
    }

    final static Cache<Pair<Op, ImmutableList<Term>>, Term> termCache = Caffeine.newBuilder()
            .weakValues()
            //.maximumSize(1024 * 32)
            .build();

    /**
     * place-holder for invalid term. this value should never leave this class
     */
    public final static AtomicSingleton NarseseNull = new AtomicSingleton("");

    final Function<Pair<Op, ImmutableList<Term>>, Term> termBuilder = (k) -> {
        ImmutableList<Term> args = k.getTwo();
        Term x = T.the(k.getOne(), args.toArray(new Term[args.size()]));
        return x == null ? NarseseNull : x;
    };

//    @Nullable
//    public static final Function<Pair<Op, List>, Term> popTermFunction = (x) -> {
//        Op op = x.getOne();
//        List vectorterms = x.getTwo();
//        Collections.reverse(vectorterms);
//
//        for (int i = 0, vectortermsSize = vectorterms.size(); i < vectortermsSize; i++) {
//            Object x1 = vectorterms.get(i);
//            if (x1 instanceof String) {
//                //string to atom
//                vectorterms.set(i, $.the(x1));
//            }
//        }
////        if ((op == null || op == PRODUCT) && (vectorterms.get(0) instanceof Operator)) {
////            op = NALOperator.OPERATION;
////        }
//
//
////        switch (op) {
//////            case OPER:
//////                return $.inh(
//////                        $.p(vectorterms.subList(1, vectorterms.size())),
//////                        $.the(vectorterms.get(0).toString())
//////                );
////            default:
//                return $.compound(op, vectorterms);
////        }
//    };


    /**
     * whitespace, optional
     */
    public Rule s() {
        return zeroOrMore(whitespace());
    }

    /**
     * whitespace, requried
     */
    public Rule ss() {
        return oneOrMore(whitespace());
    }

    public Rule whitespace() {
        return anyOf(" \t\f\n\r");
    }

//    Rule sNonNewLine() {
//        return zeroOrMore(anyOf(" \t\f"));
//    }

//    public static NarseseParser newParser(NAR n) {
//        return newParser(n.memory);
//    }
//
//    public static NarseseParser newParser(Memory m) {
//        NarseseParser np = ;
//        return np;
//    }


    /**
     * returns number of tasks created
     */
    public static int tasks(String input, Collection<Task> c, NAR m) throws NarseseException {
        int[] i = new int[1];
        tasks(input, t -> {
            c.add(t);
            i[0]++;
        }, m);
        return i[0];
    }


    /**
     * gets a stream of raw immutable task-generating objects
     * which can be re-used because a Memory can generate them
     * ondemand
     */
    public static void tasks(String input, Consumer<Task> c, NAR m) throws NarseseException {
        @NotNull Narsese p = the();
        p.T = m.concepts;
        try {

            ParsingResult r = p.inputParser.run(input);

            int size = r.getValueStack().size();

            for (int i = size - 1; i >= 0; i--) {
                Object o = r.getValueStack().peek(i);

                Object[] y;
                if (o instanceof Task) {
                    y = (new Object[]{o});
                } else if (o instanceof Object[]) {
                    y = ((Object[]) o);
                } else {
                    throw new NarseseException("Parse error: " + input);
                }
                Task t = decodeTask(m, y);
                if (t != null) {
                    c.accept(t);
                }
            }

        } finally {
            p.T = $.terms;
        }
    }





    //r.getValueStack().clear();

//        r.getValueStack().iterator().forEachRemaining(x -> {
//            if (x instanceof Task)
//                c.accept((Task) x);
//            else {
//                throw new RuntimeException("Unknown parse result: " + x + " (" + x.getClass() + ')');
//            }
//        });


    /**
     * parse one task
     */
    @NotNull
    public Task task(String input, NAR n) throws NarseseException {
        ParsingResult r;
        T = n.concepts;
        try {
            r = singleTaskParser.run(input);
            if (r == null)
                throw new NarseseException(input);

            return decodeTask(n, (Object[]) r.getValueStack().peek());

        } catch (Throwable ge) {
            throw new NarseseException(input, ge.getCause());
        } finally {
            T = $.terms;
        }
    }

    /**
     * returns null if the Task is invalid (ex: invalid term)
     */
    @NotNull
    static Task decodeTask(NAR m, Object[] x) throws NarseseException {
        if (x.length == 1 && x[0] instanceof Task) {
            return (Task) x[0];
        }
        Term contentRaw = (Term) x[1];
        if (!(contentRaw instanceof Compound))
            throw new NarseseException("Invalid task term");
        Term content = /*m.normalize*/((Compound) contentRaw);
        /*if (!(content instanceof Compound)) {
            throw new NarseseException("Task term unnormalizable: " + contentRaw);
            //return Command.task($.func("log", content));
        } else */
        {

            Object px = x[2];

            byte punct =
                    px instanceof Byte ?
                            ((Byte) x[2]).byteValue()
                            :
                            (byte) (((Character) x[2]).charValue());

            Truth t = (Truth) x[3];
            if (t != null && !Float.isFinite(t.conf()))
                t = t.withConf(m.confDefault(punct));

            @Nullable Truth t1 = t;

//        if (p == null)
//            throw new RuntimeException("character is null");
//
//        if ((t == null) && ((p == JUDGMENT) || (p == GOAL)))
//            t = new DefaultTruth(p);
//
//        if ((blen > 0) && (Float.isFinite(b[0])))
//            blen = 0;
//

            if (t1 == null) {
                t1 = m.truthDefault(punct);
            }

            TaskBuilder ttt =
                    new TaskBuilder((Compound) content, punct, t1)
                            .time(
                                    m.time(), //creation time
                                    Tense.getRelativeOccurrence(
                                            (Tense) x[4],
                                            m
                                    ));

            if ((Float) x[0] == null)  /* do not set, Memory will apply defaults */
                ttt.budgetSafe(m.priorityDefault(punct));
            else
                ttt.budgetSafe((Float) x[0]);


            return ttt.log(NARSESE_TASK_TAG).apply(m);
        }
    }

    /**
     * parse one term NOT NORMALIZED
     */
    @NotNull
    public Term term(CharSequence s) throws NarseseException {

        Exception errorCause = null;
        ParsingResult r = null;

        try {

            r = singleTermParser.run(s);

            ValueStack stack = r.getValueStack();

            if (stack.size() == 1) {
                Object x = stack.pop();

                if (x instanceof String)
                    return $.the((String) x);
                else if (x instanceof Term)
                    return (Term) x;

            }
        } catch (Exception e) {
            errorCause = e;
        }

        throw new NarseseException(s.toString(), r, errorCause);
    }


    @NotNull
    public Term term(String s, @Nullable TermIndex index, boolean normalize) throws NarseseException {
        T = index;
        try {
            Term y = term(s);
            if (normalize && y instanceof Compound) {
                return index.normalize((Compound) y);
            } else {
                return index.get(y, true).term(); //y;
            }
        } finally {
            T = $.terms;
        }

    }

//    public TaskRule taskRule(String input) {
//        Term x = termRaw(input, singleTaskRuleParser);
//        if (x==null) return null;
//
//        return x.normalizeDestructively();
//    }


//    @Nullable
//    public <T extends Term> T termRaw(CharSequence input) throws NarseseException {
//
//        ParsingResult r = singleTermParser.run(input);
//
//        DefaultValueStack stack = (DefaultValueStack) r.getValueStack();
//        FasterList sstack = stack.stack;
//
//        switch (sstack.size()) {
//            case 1:
//
//
//                Object x = sstack.get(0);
//
//                if (x instanceof String)
//                    x = $.$((String) x);
//
//                if (x != null) {
//
//                    try {
//                        return (T) x;
//                    } catch (ClassCastException cce) {
//                        throw new NarseseException("Term mismatch: " + x.getClass(), cce);
//                    }
//                }
//                break;
//            case 0:
//                return null;
//            default:
//                throw new RuntimeException("Invalid parse stack: " + sstack);
//        }
//
//        return null;
//    }


    //    /**
//     * interactive parse test
//     */
//    public static void main(String[] args) {
//        NAR n = new NAR(new Default());
//        NarseseParser p = NarseseParser.newParser(n);
//
//        Scanner sc = new Scanner(System.in);
//
//        String input = null; //"<a ==> b>. %0.00;0.9%";
//
//        while (true) {
//            if (input == null)
//                input = sc.nextLine();
//
//            ParseRunner rpr = new ListeningParseRunner<>(p.Input());
//            //TracingParseRunner rpr = new TracingParseRunner(p.Input());
//
//            ParsingResult r = rpr.run(input);
//
//            //p.printDebugResultInfo(r);
//            input = null;
//        }
//
//    }

//    public void printDebugResultInfo(ParsingResult r) {
//
//        System.out.println("valid? " + (r.isSuccess() && (r.getParseErrors().isEmpty())));
//        r.getValueStack().iterator().forEachRemaining(x -> System.out.println("  " + x.getClass() + ' ' + x));
//
//        for (Object e : r.getParseErrors()) {
//            if (e instanceof InvalidInputError) {
//                InvalidInputError iie = (InvalidInputError) e;
//                System.err.println(e);
//                if (iie.getErrorMessage() != null)
//                    System.err.println(iie.getErrorMessage());
//                for (MatcherPath m : iie.getFailedMatchers()) {
//                    System.err.println("  ?-> " + m);
//                }
//                System.err.println(" at: " + iie.getStartIndex() + " to " + iie.getEndIndex());
//            } else {
//                System.err.println(e);
//            }
//
//        }
//
//        System.out.println(printNodeTree(r));
//
//    }


    /**
     * Describes an error that occurred while parsing Narsese
     */
    public static class NarseseException extends Exception {

        @Nullable
        public final ParsingResult result;

        /**
         * An invalid addInput line.
         *
         * @param message type of error
         */
        public NarseseException(String message) {
            super(message);
            this.result = null;
        }

        public NarseseException(String input, Throwable cause) {
            this(input, null, cause);
        }

        public NarseseException(String input, ParsingResult result, Throwable cause) {
            super(input + "\n" + result, cause);
            this.result = result;
        }
    }

    private static class MiniNullPointerException extends NullPointerException {

        @Nullable
        @Override
        public Throwable fillInStackTrace() {
            return null;
        }
    }
}
