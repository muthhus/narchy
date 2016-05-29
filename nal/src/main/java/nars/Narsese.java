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
import com.github.fge.grappa.stack.ValueStack;
import com.github.fge.grappa.support.Var;
import nars.index.TermIndex;
import nars.nal.Tense;
import nars.nal.meta.PremiseRule;
import nars.nal.meta.match.Ellipsis;
import nars.nal.nal8.operator.ImmediateOperator;
import nars.op.out.echo;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.*;
import nars.term.atom.Atom;
import nars.term.container.TermVector;
import nars.term.variable.GenericVariable;
import nars.term.variable.Variable;
import nars.truth.DefaultTruth;
import nars.truth.Truth;
import nars.util.Texts;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static nars.Op.*;
import static nars.Symbols.*;

/**
 * NARese, syntax and language for interacting with a NAR in NARS.
 * https://code.google.com/p/open-nars/wiki/InputOutputFormat
 */
public class Narsese extends BaseParser<Object> {


    //These should be set to something like RecoveringParseRunner for performance
    private final ParseRunner inputParser = new ParseRunner(Input());
    private final ParseRunner singleTaskParser = new ParseRunner(Task());
    private final ParseRunner singleTermParser = new ParseRunner(Term());
    //private final ParseRunner singleTaskRuleParser = new ListeningParseRunner3(TaskRule());

    //private final Map<String,Term> termCache = new HashMap();

    static final ThreadLocal<Narsese> parsers = ThreadLocal.withInitial(() -> Grappa.createParser(Narsese.class));

    public static Narsese the() {
        return parsers.get();
    }

    @Nullable
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
            return null;
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

        return ttt;
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

        List<Term> r = Global.newArrayList(1);
        List<Term> l = Global.newArrayList(1);

        Object popped;
        while ((popped = pop()) != PremiseRule.class) { //lets go back till to the start now
            r.add((Term) popped);
        }

        while (!getContext().getValueStack().isEmpty() && (popped = pop()) != PremiseRule.class) {
            l.add((Term) popped);
        }

        Collections.reverse(l);
        Collections.reverse(r);

        Compound premise;
        if (l.size() >= 1) {
            premise = $.p(l);
        } else {
            //empty premise list is invalid
            return null;
        }

        Compound conclusion;
        if (r.size() >= 1) {
            conclusion = $.p(r);
        } else {
            //empty premise list is invalid
            return null;
        }

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


    protected Object nonNull(Object o) {
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
                                Atom(), push(nonNull($.operator((String)pop()))), // <-- allows only atoms for operator names, normal

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

//                        //negation shorthand
                        seq(NEGATE.str, s(), Term(), push(
                                //Negation.make(popTerm(null, true)))),
                                $.neg(Atom.the(pop())))),

                        seq(SET_EXT.str,

                                firstOf(
                                    EmptyCompound(SET_EXT_CLOSER, SET_EXT),
                                    MultiArgTerm(SET_EXT, SET_EXT_CLOSER, false, false, false, false)
                                )
                        ),

                        seq(SET_INT.str,
                                firstOf(
                                    EmptyCompound(SET_INT_CLOSER, SET_INT),
                                    MultiArgTerm(SET_INT, SET_INT_CLOSER, false, false, false, false)
                                )
                        ),

                        seq(COMPOUND_TERM_OPENER,
                                firstOf(

                                        EmptyCompound(COMPOUND_TERM_CLOSER, PRODUCT),

                                        MultiArgTerm(null, COMPOUND_TERM_CLOSER, true, false, false, false),

                                        //default to product if no operator specified in ( )
                                        MultiArgTerm(PRODUCT, COMPOUND_TERM_CLOSER, false, false, false, false),

                                        MultiArgTerm(null, COMPOUND_TERM_CLOSER, false, true, true, false)
                                )
                        ),

                        NumberAtom(),
                        Atom()

                ),

                push(the(pop())),

                s()
        );
    }

    public Rule seq(@NotNull Object rule, @NotNull Object rule2,
                    Object... moreRules) {
        return sequence(rule, rule2, moreRules);
    }


    Rule EmptyCompound(char c, @NotNull Op op) {
        return sequence(
            s(), c, push(Terms.empty(op))
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


                push(TemporalRelationBuilder((Term) pop() /* pred */,
                        (Integer) pop() /*cycleDelta*/, (Op) pop() /*relation*/, (Term) pop() /* subj */))
        );
    }

    @Nullable
    public static Term TemporalRelationBuilder(Term pred, int cycles, @NotNull Op o, Term subj) {
        return $.the(o, -1, cycles, TermVector.the(subj, pred));
    }

    public final static String invalidCycleDeltaString = Integer.toString(Integer.MIN_VALUE);

    public Rule CycleDelta() {
        return
                firstOf(
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
        return sequence(OPERATOR.ch,
                Atom(), push($.operator((String)pop())));
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
                Term(false, true), s(), ':', s(), Term(),
                push($.inh((Term) pop(), (Term) pop()))
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

//    Rule AnyAlphas() {
//        //TODO handle \" escape
//        return sequence( alpha(), push(matchedChar()), zeroOrMore( alphanumeric() ), push(match()),
//                swap(),
//                push( pop().toString() + pop().toString()));
//    }

    //Rule alphanumeric() { return firstOf(alpha(), digit()); }


//    @Deprecated Rule IntervalLog() {
//        return sequence(INTERVAL_PREFIX_OLD, sequence(oneOrMore(digit()), push(match()),
//                //push(Interval.interval(-1 + Texts.i((String) pop())))
//                push(CyclesInterval.intervalLog(-1 + Texts.i((String) pop())))
//        ));
//    }


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
                        INTERSECT_EXT.str, INTERSECT_INT.str,
                        DIFF_EXT.str, DIFF_INT.str,
                        PRODUCT.str,
                        IMAGE_EXT.str, IMAGE_INT.str,

                        INHERIT.str,

                        SIMILAR.str,

                        PROPERTY.str,
                        INSTANCE.str,
                        INSTANCE_PROPERTY.str,

                        NEGATE.str,

                        IMPLICATION.str,

                        EQUIV.str,

                        DISJUNCTION.str,
                        CONJUNCTION.str
                ),

                push(getOperator(match()))
        );
    }

    Rule OpTemporal() {
        return sequence(
                trie(
                        IMPLICATION.str,
                        EQUIV.str,
                        CONJUNCTION.str
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

                push(popTerm(defaultOp, allowInternalOp))
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
    static Object the(@Nullable Object o) {
        if (o == null) return null; //pass through
        if (o instanceof Term) return o;
        if (o instanceof String) {
            String s = (String) o;
            //return Atom.the(Utf8.toUtf8(name));

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
    final Term popTerm(Op op /*default */, @Deprecated boolean allowInternalOp) {


        //System.err.println(getContext().getValueStack());

        ValueStack<Object> stack = getContext().getValueStack();


        List<Term> vectorterms = Global.newArrayList(2); //stack.size() + 1);

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
                op = OPERATOR;
                break;
            }

            if (p == Compound.class) break; //beginning of stack frame for this term


            if (p instanceof String) {
                throw new RuntimeException("string not expected here");
//                Term t = Atom.the((String) p);
//                vectorterms.add(t);
            } else if (p instanceof Term) {
                Term t = (Term) p;
                vectorterms.add(t);
            } else if (p instanceof Op) {

                if (op != null) {
                    if ((!allowInternalOp) && (!p.equals(op)))
                        throw new RuntimeException("Internal operator " + p + " not allowed here; default op=" + op);

                    throw new NarseseException("Too many operators involved: " + op + ',' + p + " in " + stack + ':' + vectorterms);
                }

                op = (Op) p;
            }
        }


        if (vectorterms.isEmpty()) return null;

        //int v = vectorterms.size();

        Collections.reverse(vectorterms);

//        if ((op == null || op == PRODUCT) && (vectorterms.get(0) instanceof Operator)) {
//            op = NALOperator.OPERATION;
//        }


        return (op == OPERATOR) ?
                $.exec($.operator(vectorterms.get(0).toString()),
                        $.p(vectorterms, 1, vectorterms.size())
                ) :
                $.the(op, -1, vectorterms);
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
    public static int tasks(@NotNull String input, @NotNull Collection<Task> c, @NotNull Memory m) throws NarseseException  {
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
    public static void tasks(@NotNull String input, @NotNull Consumer<Task> c, @NotNull Memory m) throws NarseseException  {
        tasksRaw(input, o -> {
            Task t = decodeTask(m, o);
            if (t == null) {
                m.eventError.emit("Invalid task: " + input);
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
    @Nullable
    public Task task(@NotNull String input, @NotNull Memory memory) throws NarseseException {
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
    @Nullable
    public static Task decodeTask(@NotNull Memory m, @NotNull Object[] x) throws NarseseException  {
        if (x.length == 1 && x[0] instanceof Task) {
            return (Task) x[0];
        }
        Term contentRaw = (Term) x[1];
        if (contentRaw == null)
            throw new NarseseException("Invalid task term");
        Termed content = m.index.normalized(contentRaw);
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
            Termed x = index.normalized(y);
            if (x == null)
                throw new NarseseException("Un-normalizable: " + y);
            y = x.term();
        }
        return y;
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
        public NarseseException(String input, @NotNull ParsingResult result, Throwable cause) {
            super(input + "\n" + result, cause);
            this.result = result;
        }
    }

    private static class MiniNullPointerException extends NullPointerException {

        @Override
        public synchronized Throwable fillInStackTrace() {
            return null;
        }
    }
}
