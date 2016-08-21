package nars;

import com.github.fge.grappa.Grappa;
import com.github.fge.grappa.annotations.Cached;
import com.github.fge.grappa.matchers.MatcherType;
import com.github.fge.grappa.matchers.base.AbstractMatcher;
import com.github.fge.grappa.parsers.BaseParser;
import com.github.fge.grappa.rules.Rule;
import com.github.fge.grappa.run.ParseRunner;
import com.github.fge.grappa.run.ParsingResult;
import com.github.fge.grappa.run.context.MatcherContext;
import com.github.fge.grappa.stack.ArrayValueStack;
import com.github.fge.grappa.stack.ValueStack;
import com.github.fge.grappa.support.Var;
import nars.index.TermIndex;
import nars.nal.Tense;
import nars.nal.TermBuilder;
import nars.nal.meta.match.Ellipsis;
import nars.nal.nal8.operator.ImmediateOperator;
import nars.nal.rule.PremiseRule;
import nars.op.out.echo;
import nars.task.MutableTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Operator;
import nars.term.var.GenericVariable;
import nars.term.var.Variable;
import nars.truth.DefaultTruth;
import nars.truth.Truth;
import nars.util.Texts;
import nars.util.data.map.CapacityLinkedHashMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import static nars.Op.*;
import static nars.Symbols.*;

/**
 * NARese, syntax and language for interacting with a NAR in NARS.
 * https://code.google.com/p/open-nars/wiki/InputOutputFormat
 */
public class Narsese extends BaseParser<Object> {


    public static final String NARSESE_TASK_TAG = "Narsese";


    //These should be set to something like RecoveringParseRunner for performance
    private final ParseRunner inputParser = new ParseRunner(Input());
    private final ParseRunner singleTaskParser = new ParseRunner(Task());
    private final ParseRunner singleTermParser = new ParseRunner(Term());
    //private final ParseRunner singleTaskRuleParser = new ListeningParseRunner3(TaskRule());

    //private final Map<String,Term> termCache = new HashMap();

    static final ThreadLocal<Narsese> parsers = ThreadLocal.withInitial(() -> Grappa.createParser(Narsese.class));

    static final ThreadLocal<Map<Pair<Op,List>,Term>> vectorTerms = ThreadLocal.withInitial(() ->
            new CapacityLinkedHashMap<Pair<Op, List>, Term>(512));

    //static final Map<Pair<Op,List>,Term> vectorTerms = new LimitedNonBlockingHashMap<>(2048, 2);


    public static Narsese the() {
        return parsers.get();
    }

    @NotNull
    public static Task makeTask(@NotNull Memory memory, @Nullable float[] b, Termed content, char p, @Nullable Truth t, @NotNull Tense tense) {

//        if (p == null)
//            throw new RuntimeException("character is null");
//
//        if ((t == null) && ((p == JUDGMENT) || (p == GOAL)))
//            t = new DefaultTruth(p);
//
        int blen = b != null ? b.length : 0;
//        if ((blen > 0) && (Float.isFinite(b[0])))
//            blen = 0;
//

        if (!(content instanceof Compound)) {
            throw new RuntimeException("Task content is not compound");
        }

        if (t == null) {
            t = memory.truthDefault(p);
        }

        MutableTask ttt =
                new MutableTask(content, p, t)
                        .time(
                                memory.time(), //creation time
                                Tense.getRelativeOccurrence(
                                        tense,
                                        memory
                                ));

        switch (blen) {
            case 0:     /* do not set, Memory will apply defaults */
                break;
            case 1:
                if ((p == Symbols.QUEST || p == Symbols.QUESTION)) {
                    ttt.budget(b[0],
                            memory.durabilityDefault(p),
                            memory.qualityDefault(p));

                } else {
                    ttt.budget(b[0],
                            memory.durabilityDefault(p));
                }
                break;
            case 2:
                ttt.budget(b[1], b[0]);
                break;
            default:
                ttt.budget(b[2], b[1], b[0]);
                break;
        }

        return ttt.log(NARSESE_TASK_TAG);
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
                        firstOf(
                                LineComment(),
                                Task()
                        ),
                        s()
                        //)
                ), eof());
    }

    /**
     * {Premise1,Premise2} |- Conclusion.
     */
    public Rule TaskRule() {

        //use a var to count how many rule conditions so that they can be pulled off the stack without reallocating an arraylist
        return sequence(
                STATEMENT_OPENER, s(),
                push(PremiseRule.class),

                Term(), //cause

                zeroOrMore(sepArgSep(), Term()),
                s(), TASK_RULE_FWD, s(),

                push(PremiseRule.class), //stack marker

                Term(), //effect

                zeroOrMore(sepArgSep(), Term()),
                s(), STATEMENT_CLOSER,

                push(popTaskRule())
        );
    }


    @Nullable
    public PremiseRule popTaskRule() {
        //(Term)pop(), (Term)pop()

        List<Term> r = $.newArrayList(16);
        List<Term> l = $.newArrayList(16);

        Object popped;
        while ((popped = pop()) != PremiseRule.class) { //lets go back till to the start now
            r.add(the(popped));
        }
        if (r.isEmpty()) //empty premise list is invalid
            return null;

        while (!getContext().getValueStack().isEmpty() && (popped = pop()) != PremiseRule.class) {
            l.add(the(popped));
        }
        if (l.isEmpty()) //empty premise list is invalid
            return null;


        Collections.reverse(l);
        Collections.reverse(r);

        Compound premise = $.p(l);
        Compound conclusion = $.p(r);

        return new PremiseRule(premise, conclusion);
    }

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
                LineCommentEchoed(),
                firstOf("\n", eof() /* may not have newline at end of file */)
        );
    }

    public Rule LineCommentEchoed() {
        //return Atom.the(Utf8.toUtf8(name));

        //return $.the('"' + t + '"');

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
        return sequence(
            zeroOrMore(noneOf("\n")),
            push(ImmediateOperator.command(echo.class, $.quote(match())))
        );
    }

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

        Var<float[]> budget = new Var();
        Var<Character> punc = new Var();
        Var<Term> term = new Var();
        Var<Truth> truth = new Var();
        Var<Tense> tense = new Var(Tense.Eternal);

        return sequence(
                s(),

                optional(Budget(budget)),


                Term(true, false),
                term.set((Term) pop()),

                SentencePunctuation(punc),

                optional(
                        s(), Tense(tense)
                ),

                optional(
                        s(), Truth(truth, tense)

                ),

                push(new Object[]{budget.get(), term.get(), punc.get(), truth.get(), tense.get()})
                //push(getTask(budget, term, punc, truth, tense))

        );
    }


    Rule Budget(@NotNull Var<float[]> budget) {
        return sequence(
                BUDGET_VALUE_MARK,

                ShortFloat(),

                firstOf(
                        BudgetPriorityDurabilityQuality(budget),
                        BudgetPriorityDurability(budget),
                        BudgetPriority(budget)
                ),

                optional(BUDGET_VALUE_MARK)
        );
    }

    boolean BudgetPriority(@NotNull Var<float[]> budget) {
        return budget.set(new float[]{(float) pop()});
    }

    Rule BudgetPriorityDurability(@NotNull Var<float[]> budget) {
        return sequence(
                VALUE_SEPARATOR, ShortFloat(),
                budget.set(new float[]{(float) pop(), (float) pop()}) //intermediate representation
        );
    }

    Rule BudgetPriorityDurabilityQuality(@NotNull Var<float[]> budget) {
        return sequence(
                VALUE_SEPARATOR, ShortFloat(), VALUE_SEPARATOR, ShortFloat(),
                budget.set(new float[]{(float) pop(), (float) pop(), (float) pop()}) //intermediate representation
        );
    }

    Rule Tense(@NotNull Var<Tense> tense) {
        return firstOf(
                sequence(TENSE_PRESENT, tense.set(Tense.Present)),
                sequence(TENSE_PAST, tense.set(Tense.Past)),
                sequence(TENSE_FUTURE, tense.set(Tense.Future))
        );
    }

    Rule Truth(@NotNull Var<Truth> truth, @NotNull Var<Tense> tense) {
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

    Rule TruthTenseSeparator(char defaultChar, @NotNull Var<Tense> tense) {
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

    Rule SentencePunctuation(@NotNull Var<Character> punc) {
        return firstOf(

                sequence(anyOf(".?!@;"), punc.set(matchedChar())),

                //default to command if punctuation missing
                sequence(eof(), punc.set(';'))
        );
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

    @Cached
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
                        QuotedMultilineLiteral(),
                        QuotedLiteral(),

                        Operator(),

                        seq(meta, Ellipsis()),

                        seq(meta, TaskRule()),


                        TemporalRelation(),

                        //Functional form of an Operation, ex: operate(p1,p2), TODO move to FunctionalOperationTerm() rule
                        seq(oper,

                                //Term(false, false), //<-- allows non-atom terms for operator names
                                Atom(), push(nonNull($.oper((String)pop()))), // <-- allows only atoms for operator names, normal

                                COMPOUND_TERM_OPENER, s(),
                                firstOf(
                                    seq(COMPOUND_TERM_CLOSER, push( nonNull($.exec((Operator)pop())) )),
                                    MultiArgTerm(null, COMPOUND_TERM_CLOSER, false, false, false, true)
                                )

                        ),
                        seq(oper, ColonReverseInheritance()),

                        seq(STATEMENT_OPENER,
                                MultiArgTerm(null, STATEMENT_CLOSER, false, true, true, false)
                        ),


                        Variable(),

                        seq(SETe.str,

                                firstOf(
                                    EmptyCompound(SET_EXT_CLOSER, SETe),
                                    MultiArgTerm(SETe, SET_EXT_CLOSER, false, false, false, false)
                                )
                        ),

                        seq(SETi.str,
                                firstOf(
                                    EmptyCompound(SET_INT_CLOSER, SETi),
                                    MultiArgTerm(SETi, SET_INT_CLOSER, false, false, false, false)
                                )
                        ),

                        seq(COMPOUND_TERM_OPENER,
                                firstOf(

                                        EmptyCompound(COMPOUND_TERM_CLOSER, PROD),

                                        MultiArgTerm(null, COMPOUND_TERM_CLOSER, true, false, false, false),

                                        //default to product if no operator specified in ( )
                                        MultiArgTerm(PROD, COMPOUND_TERM_CLOSER, false, false, false, false),

                                        MultiArgTerm(null, COMPOUND_TERM_CLOSER, false, true, true, false)
                                )
                        ),

                        //negation shorthand
                        seq(NEG.str, s(), Term(), push(
                                //Negation.make(popTerm(null, true)))),
                                $.neg( /*$.$(*/ (Term)pop() ))),

                        NumberAtom(),
                        Atom()

                ),

                //ATOM
                push((pop())),

                s()
        );
    }

    public Rule seq(@NotNull Object rule, @NotNull Object rule2,
                    Object... moreRules) {
        return sequence(rule, rule2, moreRules);
    }


    Rule EmptyCompound(char c, @NotNull Op op) {
        return sequence(
            s(), c, push(TermBuilder.empty(op))
        );
    }

//    public Rule ConjunctionParallel() {
//    }

    @Deprecated public Rule TemporalRelation() {
        return seq(

                COMPOUND_TERM_OPENER,
                s(),
                Term(true,false),
                s(),
                OpTemporal(),
                CycleDelta(),
                s(),
                Term(true,false),
                s(),
                COMPOUND_TERM_CLOSER,


                push(TemporalRelationBuilder(the(pop()) /* pred */,
                        (Integer) pop() /*cycleDelta*/, (Op) pop() /*relation*/, the(pop()) /* subj */))
        );
    }

    @Nullable
    public static Term TemporalRelationBuilder(Term pred, int cycles, @NotNull Op o, Term subj) {
        return $.compound(o, cycles, subj, pred);
    }

    public final static String invalidCycleDeltaString = Integer.toString(Integer.MIN_VALUE);

    public Rule CycleDelta() {
        return
                firstOf(
                    seq("+-", push(Tense.XTERNAL)),
                    seq('+',oneOrMore(digit()),
                        push(Integer.parseInt(matchOrDefault(invalidCycleDeltaString)))
                    ),
                    seq('-',oneOrMore(digit()),
                        push(-Integer.parseInt(matchOrDefault(invalidCycleDeltaString)))
                    )
                )
        ;
    }

    public Rule Operator() {
        return sequence(OPER.ch,
                Atom(), push($.oper((String)pop())));
                //Term(false, false),
                //push($.operator(pop().toString())));
    }

    //final static String invalidAtomCharacters = " ,.!?" + INTERVAL_PREFIX_OLD + "<>-=*|&()<>[]{}%#$@\'\"\t\n";

    /**
     * an atomic term, returns a String because the result may be used as a Variable name
     */
    Rule Atom() {
        return sequence(
                ValidAtomCharMatcher.the,
                push(match())
        );
    }

    Rule NumberAtom() {
        return sequence(
                sequence(
                        optional('-'),
                        oneOrMore(digit()),
                        optional('.', oneOrMore(digit()))
                ),
                push($.the(Float.parseFloat(matchOrDefault("NaN"))))
        );
    }


    public static final class ValidAtomCharMatcher extends AbstractMatcher {

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
        public <V> boolean match(@NotNull MatcherContext<V> context) {
            int count = 0;
            int max = context.getInputBuffer().length() - context.getCurrentIndex();

            while (count < max && isValidAtomChar(context.getCurrentChar())) {
                context.advanceIndex(1);
                count++;
            }

            return count > 0;
        }
    }

    public static boolean isValidAtomChar(char c) {
        int x = c;

        //TODO replace these with Symbols. constants
        switch (x) {
            case ' ':
            case Symbols.ARGUMENT_SEPARATOR:
            case Symbols.BELIEF:
            case Symbols.GOAL:
            case Symbols.QUESTION:
            case Symbols.QUEST:
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
                push($.inh(the(pop()), the(pop())))
        );
    }

//    /**
//     * MACRO: y`x    becomes    <{x} --> y>
//     */
//    Rule BacktickReverseInstance() {
//        return sequence(
//                Atom(), s(), '`', s(), Term(false),
//                push(Instance.make((Term)(pop()), Atom.the(pop())))
//        );
//    }
//

//    /** creates a parser that is not associated with a memory; it will not parse any operator terms (which are registered with a Memory instance) */
//    public static NarseseParser newParser() {
//        return newParser((Memory)null);
//    }
//
//    public static NarseseParser newMetaParser() {
//        return newParser((Memory)null);
//    }


    Rule QuotedLiteral() {
        return sequence(dquote(), AnyString(), push($.quote(match())), dquote());
    }

    Rule QuotedMultilineLiteral() {
        return sequence(
                TripleQuote(), //dquote(), dquote(), dquote()),
                AnyString(), push('\"' + match() + '\"'),
                TripleQuote() //dquote(), dquote(), dquote()
        );
    }

    Rule TripleQuote() {
        return string("\"\"\"");
    }

    Rule Ellipsis() {
        return sequence(
                Variable(), "..",
                firstOf(

                        seq("_=", Term(false,false), "..+",
                                swap(2),
                                push(new Ellipsis.EllipsisTransformPrototype(/*Op.VAR_PATTERN,*/
                                        (Variable) pop(), Op.Imdex, (Term) pop()))
                        ),
                        seq(Term(false,false), "=_..+",
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

    Rule AnyString() {
        //TODO handle \" escape
        return oneOrMore(noneOf("\""));
    }


    Rule Variable() {
        /*
           <variable> ::= "$"<word>                          // independent variable
                        | "#"[<word>]                        // dependent variable
                        | "?"[<word>]                        // query variable in question
                        | "%"[<word>]                        // pattern variable in rule
        */
        return sequence(
                anyOf(new char[] {
                        Symbols.VAR_INDEPENDENT,
                        Symbols.VAR_DEPENDENT,
                        Symbols.VAR_QUERY,
                        Symbols.VAR_PATTERN
                } ),
                push(match()),
                Atom(),
                swap(),
                push( $.v( ((String)pop()).charAt(0), (String)pop()))
//
//                sequence(Symbols.VAR_INDEPENDENT, Atom(), push($.v(VAR_INDEP, (String) pop()))),
//                sequence(Symbols.VAR_DEPENDENT, Atom(), push($.v(VAR_DEP, ((String) pop())))),
//                sequence(Symbols.VAR_QUERY, Atom(), push($.v(Op.VAR_QUERY, (String) pop()))),
//                sequence(Symbols.VAR_PATTERN, Atom(), push($.v(Op.VAR_PATTERN, (String) pop())))
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

                push(getOperator(match()))
        );
    }

    Rule OpTemporal() {
        return sequence(
                trie(
                        IMPL.str,
                        EQUI.str,
                        CONJ.str
                ),
                push(getOperator(match()))
        );
    }

    Rule sepArgSep() {
        return sequence(s(), optional(ARGUMENT_SEPARATOR), s());
    }



    /**
     * list of terms prefixed by a particular compound term operate
     */
    @Cached
    Rule MultiArgTerm(Op defaultOp, char close, boolean initialOp, boolean allowInternalOp, @Deprecated boolean spaceSeparates, boolean operatorPrecedes) {


        return sequence(

                /*operatorPrecedes ? *OperationPrefixTerm()* true :*/

                operatorPrecedes ?
                        push(new Object[]{pop(), (Operator.class)})
                         : push(Compound.class),

                initialOp ? Op() : Term(),

                spaceSeparates ?

                        sequence(s(), Op(), s(), Term())

                        :

                        zeroOrMore(sequence(
                                sepArgSep(),
                                allowInternalOp ? AnyOperatorOrTerm() : Term()
                        )),

                s(), close,

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
        if (o == null) return null; //pass through
        if (o instanceof Term) return (Term) o;
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
    @Deprecated
    final Term popTerm(Op op /*default */) {

        //System.err.println(getContext().getValueStack());

        ArrayValueStack<Object> stack = (ArrayValueStack)getContext().getValueStack();

        List vectorterms = $.newArrayList(2); //stack.size() + 1);

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

            if (p == Operator.class) {
                op = OPER;
                break;
            }

            if (p == Compound.class) break; //beginning of stack frame for this term


            if (p instanceof String) {
                //throw new RuntimeException("string not expected here");
                //Term t = $.the((String) p);
                vectorterms.add(p);
            } else if (p instanceof Term) {
                vectorterms.add(p);
            } else if (p instanceof Op) {

                if (op != null) {
                    //if ((!allowInternalOp) && (!p.equals(op)))
                        //throw new RuntimeException("Internal operator " + p + " not allowed here; default op=" + op);

                    throw new NarseseException("Too many operators involved: " + op + ',' + p + " in " + stack + ':' + vectorterms);
                }

                op = (Op) p;
            }
        }


        if (vectorterms.isEmpty()) return null;


        return vectorTerms.get().computeIfAbsent(Tuples.pair(op, (List) vectorterms), popTermFunction);
    }


    @Nullable
    public static final Function<Pair<Op, List>, Term> popTermFunction = (x) -> {
        return _popTerm(x.getOne(), x.getTwo());
    };



    @Nullable
    private static Term _popTerm(@NotNull Op op, @NotNull List vectorterms) {
        Collections.reverse(vectorterms);

        for (int i = 0, vectortermsSize = vectorterms.size(); i < vectortermsSize; i++) {
            Object x = vectorterms.get(i);
            if (x instanceof String) {
                //string to atom
                vectorterms.set(i, $.the(x));
            }
        }
//        if ((op == null || op == PRODUCT) && (vectorterms.get(0) instanceof Operator)) {
//            op = NALOperator.OPERATION;
//        }


        return (op == OPER) ?
                $.exec($.oper(vectorterms.get(0).toString()),
                        vectorterms.subList(1, vectorterms.size())
                ) :
                $.compound(op, vectorterms);
    }


    /**
     * whitespace, optional
     */
    @NotNull Rule s() {
        return zeroOrMore(anyOf(" \t\f\n\r"));
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
    public static int tasks(@NotNull String input, @NotNull Collection<Task> c, @NotNull Consumer<Object[]> unparsed, @NotNull NAR m)  {
        int[] i = new int[1];
        tasks(input, t -> {
            c.add(t);
            i[0]++;
        }, unparsed, m);
        if (i[0] == 0)
            unparsed.accept(new Object[]{input});
        return i[0];
    }

    /**
     * gets a stream of raw immutable task-generating objects
     * which can be re-used because a Memory can generate them
     * ondemand
     */
    public static void tasks(@NotNull String input, @NotNull Consumer<Task> c, @NotNull Consumer<Object[]> unparsed, @NotNull NAR m) {
        tasksRaw(input, o -> {
            Task t = decodeTask(m, o);
            if (t == null) {
                if (unparsed!=null)
                    unparsed.accept(o);
            } else {
                c.accept(t);
            }
        });
    }


    /**
     * supplies the source array of objects that can construct a Task
     */
    public static void tasksRaw(@NotNull CharSequence input, @NotNull Consumer<Object[]> c) {

        ParsingResult r = the().inputParser.run(input);

        int size = r.getValueStack().size();

        for (int i = size - 1; i >= 0; i--) {
            Object o = r.getValueStack().peek(i);

            if (o instanceof Task) {
                //wrap the task in an array
                c.accept(new Object[]{o});
            } else if (o instanceof Object[]) {
                c.accept((Object[]) o);
            } else {
                throw new RuntimeException("Unrecognized input result: " + o);
            }
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
    public Task task(@NotNull String input, @NotNull NAR memory) throws NarseseException {
        ParsingResult r;
        try {
            r = singleTaskParser.run(input);
            if (r == null)
                throw new NarseseException(input);

            try {
                return decodeTask(memory, (Object[]) r.getValueStack().peek());
            } catch (Exception e) {
                throw new NarseseException(input, r, e);
            }

        } catch (Throwable ge) {
            //ge.printStackTrace();
            throw new NarseseException(input, ge.getCause());
        }

    }

    /**
     * returns null if the Task is invalid (ex: invalid term)
     */
    @NotNull
    public static Task decodeTask(@NotNull NAR m, @NotNull Object[] x) throws NarseseException  {
        if (x.length == 1 && x[0] instanceof Task) {
            return (Task) x[0];
        }
        Term contentRaw = (Term) x[1];
        if (!(contentRaw instanceof Compound))
            throw new NarseseException("Invalid task term");
        Termed content = m.normalize((Compound)contentRaw);
        if (content == null)
            throw new NarseseException("Task term unnormalizable: " + contentRaw);

        char punct = (Character) x[2];

        Truth t = (Truth) x[3];
        if (t!=null && !Float.isFinite(t.conf()))
            t = t.withConf(m.confidenceDefault(punct));

        return makeTask(m, (float[]) x[0], content, punct, t, (Tense) x[4]);
    }

    /**
     * parse one term NOT NORMALIZED
     */
    @NotNull public Term term(@NotNull CharSequence s) throws NarseseException {

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
                    return (Term)x;
            }

        } catch (Exception e) {
            errorCause = e;
        }

        throw new NarseseException(s.toString(), r, errorCause);
    }


    @NotNull public Term term(@NotNull String s, @NotNull TermIndex t) throws NarseseException  {
        return term(s, t, t!=null);
    }

    @NotNull
    public Term term(@NotNull String s, @Nullable TermIndex index, boolean normalize) throws NarseseException  {
        Term y = term(s);
        if (normalize) {
            if (y instanceof Compound) {
                Compound x = index.normalize((Compound)y);
                if (x == null)
                    throw new NarseseException("Un-normalizable: " + y);
                return x;
            }

        }
        return index.get(y, true).term(); //y;
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
    public static class NarseseException extends RuntimeException {

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
        public synchronized Throwable fillInStackTrace() {
            return null;
        }
    }
}
